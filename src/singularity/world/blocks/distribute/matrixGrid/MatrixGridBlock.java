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
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.ui.tables.DistTargetConfigTable.TargetConfigure;
import singularity.util.NumberStrify;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.blocks.distribute.matrixGrid.RequestHandlers.RequestHandler;
import singularity.world.components.SecondableConfigBuildComp;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.components.distnet.DistMatrixUnitComp;
import singularity.world.components.distnet.IOPointComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.request.DistRequestBase;
import singularity.world.meta.SglStat;
import universecore.annotations.Annotations;
import universecore.util.DataPackable;
import universecore.util.colletion.TreeSeq;

import static mindustry.Vars.control;
import static mindustry.Vars.world;
import static singularity.world.blocks.distribute.matrixGrid.MatrixGridBlock.MatrixGridBuild.PosCfgPair.typeID;

@SuppressWarnings({"unchecked", "rawtypes"})
@Annotations.ImplEntries
public class MatrixGridBlock extends DistNetBlock implements DistMatrixUnitComp{
  static {
    DataPackable.assignType(typeID, param -> {
      MatrixGridBuild.PosCfgPair res = Pools.obtain(MatrixGridBuild.PosCfgPair.class, MatrixGridBuild.PosCfgPair::new);
      if(param[0] instanceof MatrixGridBuild build) res.tile = build.tile;
      else if(param[0] instanceof Tile tile) res.tile = tile;
      return res;
    });
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
      Building t = Vars.world.build(c.position);
      if(t == null) return;

      if(c.isClear()){
        if(t instanceof IOPointComp){
          ((IOPointComp) t).gridConfig(null);
        }
        TargetConfigure oldCfg = entity.configMap.remove(c.position);
        if(oldCfg != null) entity.configs.remove(oldCfg);
        if(!(t instanceof IOPointComp)){
          entity.grid.remove(t);
        }
      }
      else{
        if(t instanceof IOPointComp){
          ((IOPointComp) t).applyConfig(c);
        }

        TargetConfigure oldCfg = entity.configMap.put(c.position, c);
        if(oldCfg != null) entity.configs.remove(oldCfg);
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
      
      for(IntMap.Entry<TargetConfigure> entry : pair.configs){
        Building b;

        if((b = world.build(entry.key)) == null){
          if(entry.value.isContainer()) continue;
          Tile tile = world.tile(entry.key);
          if(tile == null) continue;
          Sgl.ioPoint.setCurrPlacement(entity);
          tile.setBlock(Sgl.ioPoint, entity.team, 0);
          b = tile.build;
          ((IOPointComp)b).applyConfig(entry.value);
          entity.ioPointConfigBackEntry((IOPointComp) b);
        }
        else{
          if(b.pos() != entry.key) continue;
          entity.configMap.put(entry.key, entry.value);
          entity.configs.add(entry.value);
          entity.grid.addConfig(entry.value);
        }
      }
      
      Sgl.ioPoint.resetCurrPlacement();
      entity.shouldUpdateTask = true;

      Pools.free(pair);
    }
  }
  
  @SuppressWarnings("rawtypes")
  @Annotations.ImplEntries
  public class MatrixGridBuild extends DistNetBuild implements DistMatrixUnitBuildComp, SecondableConfigBuildComp{
    public MatrixGrid grid = new MatrixGrid();
    
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

        for(IntMap.Entry<TargetConfigure> config: configMap){
          Building other = world.build(config.value.position);
          if(other == null || other.pos() != config.value.position){
            configMap.remove(config.key);
            continue;
          }

          if(other instanceof IOPointComp){
            ((IOPointComp) other).parent(this);
            ((IOPointComp) other).applyConfig(config.value);
            ioPointConfigBackEntry((IOPointComp) other);
          }
          else{
            grid.addConfig(config.value);
            configs.add(config.value);
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
      configMap.put(ioPoint.getBuilding().pos(), ioPoint.gridConfig());
      configs.add(ioPoint.gridConfig());
      grid.addConfig(ioPoint.gridConfig());
      shouldUpdateTask = true;
    }
  
    @Override
    public void buildSecondaryConfig(Table table, Building target){
      GridChildType[] config = target instanceof IOPointComp point?
          point.configTypes():
          new GridChildType[]{GridChildType.container};
      table.add(new DistTargetConfigTable(
          target,
          configMap.get(target.pos()),
          config,
          target instanceof IOPointComp point? point.configContentTypes()
              : getAcceptType(target.block),
          c -> configure(c.pack()),
          Sgl.ui.secConfig::hideConfig
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
        for(IntMap.Entry<TargetConfigure> entry: configMap){
          IOPointComp b = ioPoints.get(entry.key);
          if(b != null && !b.getBuilding().isAdded()){
            TargetConfigure c = configMap.remove(entry.key);
            configs.remove(c);
            shouldUpdateTask = true;
          }
        }

        for(MatrixGrid.BuildingEntry<Building> entry: grid.<Building>get(GridChildType.container, (b, c) -> true)){
          if(world.tile(entry.config.position).build != entry.entity){
            TargetConfigure c = configMap.remove(entry.config.position);
            configs.remove(c);
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
          return false;
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
            Sgl.ui.secConfig.showOn(other);
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
      pair.tile = tile;
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
      pair.tile = tile;
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
      pair.tile = tile;
      pair.configs = configMap;
      byte[] bytes = pair.pack();
      write.i(bytes.length);
      write.b(bytes);

      Pools.free(pair);
    }

    protected static class PosCfgPair implements DataPackable, Pool.Poolable{
      public static final long typeID = 1679658234266591164L;
    
      IntMap<TargetConfigure> configs;
      Tile tile;
    
      @Override
      public long typeID(){
        return typeID;
      }
    
      @Override
      public void write(Writes write){
        write.i(configs.size);
        for(IntMap.Entry<TargetConfigure> entry : configs){
          Point2 p = Point2.unpack(entry.key);
          entry.value.position = Point2.pack(p.x - tile.x, p.y - tile.y);
          byte[] bytes = entry.value.pack();
          entry.value.position = entry.key;
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
          Point2 p = Point2.unpack(cfg.position);
          int pos = Point2.pack(tile.x + p.x, tile.y + p.y);
          cfg.position = pos;
          configs.put(pos, cfg);
        }
      }

      @Override
      public void reset(){
        configs = null;
        tile = null;
      }
    }
  }
}
