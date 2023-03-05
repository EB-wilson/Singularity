package singularity.world.blocks.distribute.matrixGrid;

import arc.Core;
import arc.func.Cons;
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
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.blocks.distribute.GenericIOPoint;
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

@SuppressWarnings({"unchecked", "rawtypes"})
@Annotations.ImplEntries
public class MatrixGridBlock extends DistNetBlock implements DistMatrixUnitComp{
  public int bufferCapacity = 256;
  
  public MatrixGridBlock(String name){
    super(name);
    displayFlow = false;
    hasItems = hasLiquids = true;
    outputItems = outputsLiquid = false;
    configurable = true;
    independenceInventory = false;
    independenceLiquidTank = false;
  }
  
  @Override
  public void init(){
    super.init();
    itemCapacity = bufferCapacity/8;
    liquidCapacity = bufferCapacity/4f;

    if (size < 3)
      throw new RuntimeException("matrix grid core size must >= 3, curr: " + size);
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
        if(t instanceof IOPointComp io){
          io.gridConfig(null);
          io.parent(null);
          entity.ioPoints.remove(io);
        }
        TargetConfigure oldCfg = entity.configMap.remove(c.offsetPos);
        if (oldCfg != null) {
          entity.configs.remove(oldCfg);
        }
        entity.grid.remove(t);
      }
      else{
        if(t instanceof IOPointComp io){
          io.gridConfig(c);
          entity.ioPointConfigBackEntry(io);
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

        if((b = e.nearby(Point2.x(cfg.offsetPos), Point2.y(cfg.offsetPos))) != null){
          if(b.pos() != Point2.pack(e.tileX() + Point2.x(cfg.offsetPos), e.tileY() + Point2.y(cfg.offsetPos))) continue;
          entity.configMap.put(cfg.offsetPos, cfg);
          entity.configs.add(cfg);
          entity.grid.addConfig(cfg);
        }
      }
      entity.shouldUpdateTask = true;

      Pools.free(pair);
    }
  }

  @Override
  public Object pointConfig(Object config, Cons<Point2> transformer){
    if(config instanceof byte[] b && DataPackable.readObject(b) instanceof MatrixGridBuild.PosCfgPair cfg){
      cfg.handleConfig(transformer);
      return cfg.pack();
    }
    return config;
  }

  @SuppressWarnings("rawtypes")
  @Annotations.ImplEntries
  public class MatrixGridBuild extends DistNetBuild implements DistMatrixUnitBuildComp, SecondableConfigBuildComp {
    public MatrixGrid grid = new MatrixGrid(this);
    
    protected TreeSeq<TargetConfigure> configs = new TreeSeq<>((a, b) -> b.priority - a.priority);
    protected IntMap<TargetConfigure> configMap = new IntMap<>();
    protected ObjectSet<IOPointComp> ioPoints = new ObjectSet<>();

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

          if(other instanceof IOPointComp io){
            io.gridConfig(config);
            ioPointConfigBackEntry(io);
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
      ioPoint.parent(this);
      ioPoints.add(ioPoint);
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
          target instanceof GenericIOPoint.GenericIOPPointBuild,
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
    public void addIO(IOPointComp io) {
      if (isAdded()){
        ioPointConfigBackEntry(io);
      }
    }

    @Override
    public void removeIO(IOPointComp io){
      if(isAdded()){
        ioPoints.remove(io);
        grid.remove(io.getBuilding());
        TargetConfigure cfg = configMap.remove(Point2.pack(io.getTile().x - tileX(), io.getTile().y - tileY()));
        if(cfg != null) configs.remove(cfg);
        shouldUpdateTask = true;
      }
    }
  
    @Override
    public void updateTile(){
      if(gridValid()){
        for (GridChildType value : GridChildType.values()) {
          for (MatrixGrid.BuildingEntry<Building> entry : grid.<Building>get(value, (b, c) -> true)) {
            Building b = nearby(Point2.x(entry.config.offsetPos), Point2.y(entry.config.offsetPos));
            if (b == null || !b.isAdded() || b != entry.entity) {
              if (b instanceof IOPointComp io) {
                removeIO(io);
              } else {
                TargetConfigure c = configMap.remove(entry.config.offsetPos);
                if (c != null) {
                  configs.remove(c);
                }
                grid.remove(entry.entity);
                shouldUpdateTask = true;
              }
            }
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
    }
  
    @Override
    public boolean onConfigureBuildTapped(Building other){
      if(other == this){
        configIOPoint = !configIOPoint;
        return false;
      }
      else if(tileValid(other.tile) && gridValid()){
        if(configValid(other)){
          UncCore.secConfig.showOn(other);
        }
        return false;
      }
      else{
        configIOPoint = false;
        return true;
      }
    }
  
    @Override
    public byte[] config(){
      PosCfgPair pair = new PosCfgPair();
      pair.configs.clear();
      for (IntMap.Entry<TargetConfigure> entry : configMap) {
        Building build = nearby(Point2.x(entry.key), Point2.y(entry.key));
        if (build != null && !(build instanceof IOPointComp io && !ioPoints.contains(io))){
          pair.configs.put(entry.key, entry.value);
        }
      }

      return pair.pack();
    }

    @Override
    public boolean acceptItem(Building source, Item item){
      return source instanceof IOPointComp io && ioPoints.contains(io);
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source instanceof IOPointComp io && ioPoints.contains(io);
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
      pair.configs.clear();
      for (IntMap.Entry<TargetConfigure> entry : configMap) {
        Building build = nearby(Point2.x(entry.key), Point2.y(entry.key));
        if (build != null && !(build instanceof IOPointComp io && !ioPoints.contains(io))){
          pair.configs.put(entry.key, entry.value);
        }
      }

      byte[] bytes = pair.pack();
      write.i(bytes.length);
      write.b(bytes);

      Pools.free(pair);
    }

    public static class PosCfgPair implements DataPackable, Pool.Poolable{
      public static final long typeID = 1679658234266591164L;
    
      IntMap<TargetConfigure> configs = new IntMap<>();
    
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
        configs.clear();
        for(int i = 0; i < length; i++){
          TargetConfigure cfg = new TargetConfigure();
          int len = read.i();
          cfg.read(read.b(len));
          configs.put(cfg.offsetPos, cfg);
        }
      }

      @Override
      public void reset(){
        configs.clear();
      }

      public void handleConfig(Cons<Point2> handler){
        IntMap<TargetConfigure> c = new IntMap<>();
        for(IntMap.Entry<TargetConfigure> entry: configs){
          entry.value.configHandle(handler);
          c.put(entry.value.offsetPos, entry.value);
        }

        configs = c;
      }
    }
  }
}
