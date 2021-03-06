package singularity.world.blocks.distribute.matrixGrid;

import arc.Core;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.type.SglContents;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.ui.tables.DistTargetConfigTable.TargetConfigure;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.blocks.distribute.IOPointBlock;
import singularity.world.blocks.distribute.matrixGrid.RequestHandlers.RequestHandler;
import singularity.world.components.GasBuildComp;
import singularity.world.components.SecondableConfigBuildComp;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.components.distnet.DistMatrixUnitComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.request.DistRequestBase;
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
    DataPackable.assignType(typeID, param -> ((MatrixGridBuild)param[0]).new PosCfgPair());
  }

  public int bufferCapacity = 256;
  
  public MatrixGridBlock(String name){
    super(name);
    displayFlow = false;
    showGasFlow = false;
    hasItems = hasLiquids = hasGases = true;
    outputItems = outputsLiquid = outputGases = false;
    configurable = true;
    independenceInventory = false;
    independenceLiquidTank = false;

    schematicPriority = -20;
    
    assignFactory();
  }
  
  @Override
  public void init(){
    super.init();
    itemCapacity = bufferCapacity/8;
    liquidCapacity = bufferCapacity/4f;
    gasCapacity = bufferCapacity/2f/maxGasPressure;
  }
  
  @Override
  public void parseConfigObjects(SglBuilding e, Object obj){
    MatrixGridBuild entity = (MatrixGridBuild) e;
    if(obj instanceof TargetConfigure c){

      Building t = Vars.world.build(c.position);
      if(t == null) return;

      if(c.isClear()){
        if(t instanceof IOPointBlock.IOPoint){
          ((IOPointBlock.IOPoint) t).config = null;
        }
        TargetConfigure oldCfg = entity.configMap.remove(c.position);
        if(oldCfg != null) entity.configs.remove(oldCfg);
        if(!(t instanceof IOPointBlock.IOPoint)){
          entity.grid.remove(t);
        }
      }
      else{
        if(t instanceof IOPointBlock.IOPoint){
          ((IOPointBlock.IOPoint) t).applyConfig(c);
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
      for(IntMap.Entry<IOPointBlock.IOPoint> ent : entity.ioPoints){
        ent.value.remove();
      }
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
          ((IOPointBlock.IOPoint)b).applyConfig(entry.value);
          entity.ioPointConfigBackEntry((IOPointBlock.IOPoint) b);
        }
        else{
          if(b.pos() != entry.key) continue;
          entity.configMap.put(entry.value.position, entry.value);
          entity.configs.add(entry.value);
          entity.grid.addConfig(entry.value);
        }
      }
      
      Sgl.ioPoint.resetCurrPlacement();
      entity.shouldUpdateTask = true;
    }
  }
  
  public void assignFactory(){
    //items
    setFactory(GridChildType.output, ContentType.item, new RequestHandlers.ReadItemRequestHandler());
    setFactory(GridChildType.input, ContentType.item, new RequestHandlers.PutItemRequestHandler());
    setFactory(GridChildType.acceptor, ContentType.item, new RequestHandlers.AcceptItemRequestHandler());

    //liquids
    setFactory(GridChildType.output, ContentType.liquid, new RequestHandlers.ReadLiquidRequestHandler());
    setFactory(GridChildType.input, ContentType.liquid, new RequestHandlers.PutLiquidRequestHandler());
    setFactory(GridChildType.acceptor, ContentType.liquid, new RequestHandlers.AcceptLiquidRequestHandler());
  }
  
  @SuppressWarnings("rawtypes")
  @Annotations.ImplEntries
  public class MatrixGridBuild extends DistNetBuild implements DistMatrixUnitBuildComp, SecondableConfigBuildComp{
    public MatrixGrid grid = new MatrixGrid(this);
    
    protected TreeSeq<TargetConfigure> configs = new TreeSeq<>((a, b) -> b.priority - a.priority);
    protected IntMap<TargetConfigure> configMap = new IntMap<>();
    protected IntMap<IOPointBlock.IOPoint> ioPoints = new IntMap<>();

    protected ObjectMap<DistRequestBase<?>, RequestHandler<?>> requestHandlerMap = new ObjectMap<>();

    public boolean configIOPoint = false, shouldUpdateTask = true;
    
    public ObjectSet<DistRequestBase> requests = new ObjectSet<>();

    private double frameID;
    
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      initBuffers();
      
      items = getBuffer(DistBuffers.itemBuffer).generateBindModule();
      liquids = getBuffer(DistBuffers.liquidBuffer).generateBindModule();
      return this;
    }
  
    @Override
    public void networkValided(){
      shouldUpdateTask = true;
    }
  
    @Override
    public void onProximityAdded(){
      super.onProximityAdded();
      if(frameID != Core.graphics.getFrameId()){
        frameID = Core.graphics.getFrameId();

        for(IntMap.Entry<TargetConfigure> config: configMap){
          Building other = world.build(config.value.position);
          if(other == null || other.pos() != config.value.position) return;
          if(other instanceof IOPointBlock.IOPoint){
            ((IOPointBlock.IOPoint) other).parent = this;
            ((IOPointBlock.IOPoint) other).applyConfig(config.value);
            ioPointConfigBackEntry((IOPointBlock.IOPoint) other);
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
      return distributor.network.netValid();
    }
  
    @Override
    public void ioPointConfigBackEntry(IOPointBlock.IOPoint ioPoint){
      ioPoints.put(ioPoint.pos(), ioPoint);
      configMap.put(ioPoint.pos(), ioPoint.config);
      configs.add(ioPoint.config);
      grid.addConfig(ioPoint.config);
      shouldUpdateTask = true;
    }
  
    @Override
    public void buildSecondaryConfig(Table table, Building target){
      GridChildType[] config = target instanceof IOPointBlock.IOPoint?
          new GridChildType[]{GridChildType.output, GridChildType.input, GridChildType.acceptor}:
          new GridChildType[]{GridChildType.container};
      table.add(new DistTargetConfigTable(
          target,
          configMap.get(target.pos()),
          config,
          new ContentType[]{ContentType.item, ContentType.liquid, SglContents.gas},
          c -> configure(c.pack()),
          Sgl.ui.secConfig::hideConfig
      ));
    }
  
    @Override
    public void remove(){
      super.remove();
      for(IntMap.Entry<IOPointBlock.IOPoint> entry : ioPoints){
        entry.value.remove();
      }
    }
  
    public void releaseRequest(){
      for(DistRequestBase request : requests){
        request.kill();
      }
      requests.clear();
      
      for(TargetConfigure config : configs){
        config.eachChildType((type, map) -> {
          for(ContentType contType : map.keys()){
            addConfig(type, contType, config);
          }
        });
      }
  
      for(ObjectMap.Entry<GridChildType, ObjectMap<ContentType, RequestHandler>> entry : requestFactories()){
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
          Building b = ioPoints.get(entry.key);
          if(b != null && !b.isAdded()){
            TargetConfigure c = configMap.remove(entry.key);
            configs.remove(c);
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
      
      if((control.input.frag.config.getSelectedTile() != this && configIOPoint && control.input.block == Sgl.ioPoint)
          || (configIOPoint && control.input.block != Sgl.ioPoint)){
        configIOPoint =  false;
        Sgl.ioPoint.resetCurrPlacement();
        if(control.input.block == Sgl.ioPoint) control.input.block = null;
      }
    }
  
    @Override
    public boolean onConfigureTileTapped(Building other){
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
        if(tileValid(other.tile) && gridValid() && configValid(other)){
          Sgl.ui.secConfig.showOn(other);
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
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return ioPoints.containsKey(source.getBuilding().pos());
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      PosCfgPair pair = new PosCfgPair();
      int len = read.i();
      byte[] bytes = read.b(len);
      pair.read(bytes);

      configMap = pair.configs;
    }

    @Override
    public void write(Writes write){
      super.write(write);
      PosCfgPair pair = new PosCfgPair();
      pair.configs = configMap;
      byte[] bytes = pair.pack();
      write.i(bytes.length);
      write.b(bytes);
    }

    protected class PosCfgPair implements DataPackable{
      public static final long typeID = 1679658234266591164L;
    
      IntMap<TargetConfigure> configs;
    
      @Override
      public long typeID(){
        return typeID;
      }
    
      @Override
      public void write(Writes write){
        write.i(configs.size);
        for(IntMap.Entry<TargetConfigure> entry : configs){
          Point2 p = Point2.unpack(entry.key);
          int pos = Point2.pack(p.x - tile.x, p.y - tile.y);
          write.i(pos);
          entry.value.position = pos;
          byte[] bytes = entry.value.pack();
          write.i(bytes.length);
          write.b(bytes);
        }
      }
    
      @Override
      public void read(Reads read){
        int length = read.i();
        configs = new IntMap<>();
        for(int i = 0; i < length; i++){
          Point2 p = Point2.unpack(read.i());
          int pos = Point2.pack(tile.x + p.x, tile.y + p.y);
          TargetConfigure cfg = new TargetConfigure();
          int len = read.i();
          cfg.read(read.b(len));
          cfg.position = pos;
          configs.put(pos, cfg);
        }
      }
    }
  }
}
