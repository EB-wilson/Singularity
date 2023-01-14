package singularity.world.blocks.distribute.matrixGrid;

import arc.Core;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.ctype.ContentType;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.blocks.distribute.IOPointBlock;
import singularity.world.blocks.distribute.TargetConfigure;
import singularity.world.blocks.distribute.matrixGrid.RequestHandlers.RequestHandler;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.components.distnet.DistMatrixUnitComp;
import singularity.world.components.distnet.IOPointComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.request.DistRequestBase;
import singularity.world.meta.SglStat;
import universecore.UncCore;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.SecondableConfigBuildComp;
import universecore.util.DataPackable;
import universecore.util.NumberStrify;
import universecore.util.colletion.TreeSeq;

import static mindustry.Vars.control;
import static mindustry.Vars.world;
import static singularity.world.blocks.distribute.matrixGrid.MatrixGridBlock.MatrixGridBuild.PosCfgPair.typeID;

@SuppressWarnings({"unchecked", "rawtypes"})
@Annotations.ImplEntries
public class MatrixGridBlock extends DistNetBlock implements DistMatrixUnitComp{
  static {
    DataPackable.assignType(typeID, param -> Pools.obtain(MatrixGridBuild.PosCfgPair.class, MatrixGridBuild.PosCfgPair::new));
  }

  public int bufferCapacity = 256;
  
  public MatrixGridBlock(String name){
    super(name);
    displayFlow = false;
    hasItems = hasLiquids = true;
    outputItems = outputsLiquid = false;
    configurable = true;
    independenceInventory = false;
    independenceLiquidTank = false;

    schematicPriority = -20;
  }
  
  @Override
  public void init(){
    super.init();
    itemCapacity = bufferCapacity/8;
    liquidCapacity = bufferCapacity/4f;
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(SglStat.bufferSize, t -> {
      t.defaults().left().fillX().padBottom(5).padLeft(10);
      t.row();
      t.add(Core.bundle.get("content.item.name") + ": " + NumberStrify.toByteFix(256, 2));
      t.row();
      t.add(Core.bundle.get("content.liquid.name") + ": " + NumberStrify.toByteFix(256, 2));
    });
  }
  
  @Override
  public void parseConfigObjects(SglBuilding e, Object obj){
    MatrixGridBuild entity = (MatrixGridBuild) e;
    if(obj instanceof TargetConfigure c){
      Building t = e.nearby(Point2.x(c.offsetPos), Point2.y(c.offsetPos));
      if(t == null) return;

      if(c.isClear()){
        if(t instanceof IOPointComp){
          ((IOPointComp) t).gridConfig(null);
        }
        TargetConfigure oldCfg = entity.configMap.remove(c.offsetPos);
        if (oldCfg != null) {
          entity.configs.remove(oldCfg);
        }
        if (!(t instanceof IOPointComp)) {
          entity.grid.remove(t);
        }
      }
      else{
        if(t instanceof IOPointComp){
          ((IOPointComp) t).applyConfig(c);
        }

        TargetConfigure old = entity.configMap.put(c.offsetPos, c);
        if(old != null){
          entity.configs.remove(old);
        }
        entity.configs.add(c);
        entity.grid.remove(t);
        entity.grid.addConfig(c);
      }
  
      entity.shouldUpdateTask = true;
    }
    else if(obj instanceof MatrixGridBuild.PosCfgPair pair){
      entity.grid.clear();
      entity.ioPoints.clear();
      entity.configs.clear();
      entity.configMap.clear();
      
      for(TargetConfigure cfg : pair.configs.values()){
        Building b;

        if((b = e.nearby(Point2.x(cfg.offsetPos), Point2.y(cfg.offsetPos))) == null){
          if(cfg.isContainer()) continue;
          Tile tile = world.tile(e.tileX() + Point2.x(cfg.offsetPos), e.tileY() + Point2.y(cfg.offsetPos));
          if(tile == null) continue;
          Sgl.ioPoint.setCurrPlacement(entity);
          tile.setBlock(Sgl.ioPoint, entity.team, 0);
          b = tile.build;
          ((IOPointComp)b).applyConfig(cfg);
          entity.ioPointConfigBackEntry((IOPointComp) b);
        }
        else{
          if(b.pos() != Point2.pack(e.tileX() + Point2.x(cfg.offsetPos), e.tileY() + Point2.y(cfg.offsetPos))) continue;
          entity.configMap.put(cfg.offsetPos, cfg);
          entity.configs.add(cfg);
          entity.grid.addConfig(cfg);
        }
      }
      
      Sgl.ioPoint.resetCurrPlacement();
      entity.shouldUpdateTask = true;

      Pools.free(pair);
    }
  }

  @Override
  public void onPlanRotate(BuildPlan plan, int direction) {
    if (plan.config instanceof byte[] data && DataPackable.readObject(data) instanceof MatrixGridBuild.PosCfgPair posPair){
      for (TargetConfigure cfg : posPair.configs.values()) {
        cfg.rotateDir(this, direction);
      }

      plan.config = posPair.pack();
    }
  }
  
  @SuppressWarnings("rawtypes")
  @Annotations.ImplEntries
  public class MatrixGridBuild extends DistNetBuild implements DistMatrixUnitBuildComp, SecondableConfigBuildComp {
    public MatrixGrid grid = new MatrixGrid(this);
    
    protected TreeSeq<TargetConfigure> configs = new TreeSeq<>((a, b) -> b.priority - a.priority);
    protected IntMap<TargetConfigure> configMap = new IntMap<>();
    protected IntMap<IOPointComp> ioPoints = new IntMap<>();

    protected ObjectMap<DistRequestBase, RequestHandler<?>> requestHandlerMap = new ObjectMap<>();

    public boolean configIOPoint = false, shouldUpdateTask = true;
    
    public ObjectSet<DistRequestBase> requests = new ObjectSet<>();

    private boolean added;
    
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      initBuffers();

      items = getBuffer(DistBufferType.itemBuffer).generateBindModule();
      liquids = getBuffer(DistBufferType.liquidBuffer).generateBindModule();
      return this;
    }
  
    @Override
    public void networkValided(){
      shouldUpdateTask = true;
    }
  
    @Override
    public void onProximityAdded(){
      super.onProximityAdded();
      if(!added){
        added = true;

        for(TargetConfigure config: configMap.values()){
          Building other = nearby(Point2.x(config.offsetPos), Point2.y(config.offsetPos));
          if(other == null || Point2.pack(Point2.x(other.pos()) - tileX(), Point2.y(other.pos()) - tileY()) != config.offsetPos){
            configMap.remove(config.offsetPos);
            continue;
          }

          if(other instanceof IOPointComp){
            ((IOPointComp) other).parent(this);
            ((IOPointComp) other).applyConfig(config);
            ioPointConfigBackEntry((IOPointComp) other);
          }
          else{
            grid.addConfig(config);
            configs.add(config);
          }
        }
      }
    }
  
    @Override
    public boolean gridValid(){
      return added && distributor.network.netValid();
    }
  
    @Override
    public void ioPointConfigBackEntry(IOPointComp ioPoint){
      ioPoints.put(ioPoint.getBuilding().pos(), ioPoint);
      configMap.put(ioPoint.gridConfig().offsetPos, ioPoint.gridConfig());
      configs.add(ioPoint.gridConfig());
      grid.addConfig(ioPoint.gridConfig());
      shouldUpdateTask = true;
    }
  
    @Override
    public void buildSecondaryConfig(Table table, Building target){
      GridChildType[] config = target instanceof IOPointComp point?
          point.configTypes():
          new GridChildType[]{GridChildType.container};
      int off = Point2.pack(target.tileX() - tileX(), target.tileY() - tileY());
      table.add(new DistTargetConfigTable(
          off,
          configMap.get(off),
          config,
          target instanceof IOPointComp point? point.configContentTypes()
              : getAcceptType(target.block),
          target instanceof IOPointBlock.IOPoint,
          c -> configure(c.pack()),
          UncCore.secConfig::hideConfig
      ));
    }

    private ContentType[] getAcceptType(Block block){
      Seq<ContentType> res = new Seq<>();
      for(ObjectMap.Entry<DistBufferType<?>, Float> entry: Sgl.matrixContainers.getContainer(block).capacities){
        if(entry.value > 0) res.add(entry.key.targetType());
      }
      return res.toArray(ContentType.class);
    }

    public void releaseRequest(){
      for(DistRequestBase request : requests){
        request.kill();
      }
      requests.clear();

      resetFactories();
      
      for(TargetConfigure config : configs){
        config.eachChildType((type, map) -> {
          for(ContentType contType : map.keys()){
            addConfig(type, contType, config);
          }
        });
      }
  
      for(ObjectMap.Entry<GridChildType, ObjectMap<ContentType, RequestHandler>> entry : tempFactories()){
        for(ObjectMap.Entry<ContentType, RequestHandler> e: entry.value){
          DistRequestBase request = createRequest(entry.key, e.key);
          if(request == null) continue;
          requests.add(request);
          distributor.assign(request);

          requestHandlerMap.put(request, e.value);
        }
      }
  
      for(DistRequestBase request : requests){
        request.init(distributor.network);
      }
    }
  
    @Override
    public void drawConfigure(){
      drawValidRange();
    }
  
    @Override
    public boolean tileValid(Tile tile){
      return false;
    }
    
    @Override
    public void drawValidRange(){}
    
    @Override
    public void removeIO(int pos){
      if(isAdded()){
        DistMatrixUnitBuildComp.super.removeIO(pos);
        TargetConfigure cfg = configMap.remove(pos);
        if(cfg != null) configs.remove(cfg);
        shouldUpdateTask = true;
      }
    }
  
    @Override
    public void updateTile(){
      if(gridValid()){
        for(TargetConfigure cfg: configMap.values()){
          IOPointComp b = ioPoints.get(cfg.offsetPos);
          if(b != null && !b.getBuilding().isAdded()){
            TargetConfigure c = configMap.remove(cfg.offsetPos);
            if (c != null) {
              configs.remove(c);
            }
            shouldUpdateTask = true;
          }
        }

        for(MatrixGrid.BuildingEntry<Building> entry: grid.<Building>get(GridChildType.container, (b, c) -> true)){
          if(nearby(Point2.x(entry.config.offsetPos), Point2.y(entry.config.offsetPos)) != entry.entity){
            TargetConfigure c = configMap.remove(entry.config.offsetPos);
            if (c != null) {
              configs.remove(c);
            }
            grid.remove(entry.entity);
            shouldUpdateTask = true;
          }
        }

        if(shouldUpdateTask){
          releaseRequest();
          shouldUpdateTask = false;
        }

        for(DistRequestBase request : requests){
          RequestHandler handler = requestHandlerMap.get(request);
          request.update(
              t -> handler.preCallBack(this, request, t),
              t -> handler.callBack(this, request, t),
              t -> handler.afterCallBack(this, request, t)
          );
        }
      }

      super.updateTile();
      
      if((control.input.config.getSelected() != this && configIOPoint && control.input.block == Sgl.ioPoint)
          || (configIOPoint && control.input.block != Sgl.ioPoint)){
        configIOPoint =  false;
        Sgl.ioPoint.resetCurrPlacement();
        if(control.input.block == Sgl.ioPoint) control.input.block = null;
      }
    }
  
    @Override
    public boolean onConfigureBuildTapped(Building other){
      if(other == this){
        if(control.input.block == Sgl.ioPoint){
          configIOPoint = false;
          Sgl.ioPoint.resetCurrPlacement();
          control.input.block = null;
        }
        else{
          configIOPoint = true;
          Sgl.ioPoint.setCurrPlacement(this);
          control.input.block = Sgl.ioPoint;
        }
        return false;
      }
      else{
        if(tileValid(other.tile) && gridValid()){
          if(configValid(other)){
            UncCore.secConfig.showOn(other);
          }
          return false;
        }
        else{
          return true;
        }
      }
    }
  
    @Override
    public byte[] config(){
      PosCfgPair pair = new PosCfgPair();
      pair.configs = configMap;
      return pair.pack();
    }

    @Override
    public boolean acceptItem(Building source, Item item){
      return ioPoints.containsKey(source.pos());
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return ioPoints.containsKey(source.pos());
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      PosCfgPair pair = new PosCfgPair();
      int len = read.i();
      byte[] bytes = read.b(len);
      pair.read(bytes);

      configMap = pair.configs;

      Pools.free(pair);
    }

    @Override
    public void write(Writes write){
      super.write(write);
      PosCfgPair pair = new PosCfgPair();
      pair.configs = configMap;
      byte[] bytes = pair.pack();
      write.i(bytes.length);
      write.b(bytes);

      Pools.free(pair);
    }

    protected static class PosCfgPair implements DataPackable, Pool.Poolable{
      public static final long typeID = 1679658234266591164L;
    
      IntMap<TargetConfigure> configs;
    
      @Override
      public long typeID(){
        return typeID;
      }
    
      @Override
      public void write(Writes write){
        write.i(configs.size);
        for(TargetConfigure cfg : configs.values()){
          byte[] bytes = cfg.pack();
          write.i(bytes.length);
          write.b(bytes);
        }
      }
    
      @Override
      public void read(Reads read){
        int length = read.i();
        configs = new IntMap<>();
        for(int i = 0; i < length; i++){
          TargetConfigure cfg = new TargetConfigure();
          int len = read.i();
          cfg.read(read.b(len));
          configs.put(cfg.offsetPos, cfg);
        }
      }

      @Override
      public void reset(){
        configs = null;
      }
    }
  }
}
