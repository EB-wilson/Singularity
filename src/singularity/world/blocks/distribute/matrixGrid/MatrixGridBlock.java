package singularity.world.blocks.distribute.matrixGrid;

import arc.scene.ui.layout.Table;
import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.modules.ItemModule;
import singularity.Sgl;
import singularity.type.SglContents;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.ui.tables.DistTargetConfigTable.TargetConfigure;
import singularity.world.blockComp.SecondableConfigBuildComp;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitComp;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.blocks.distribute.IOPointBlock;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import singularity.world.distribution.request.RequestFactories;
import singularity.world.distribution.request.RequestFactories.RequestFactory;
import universeCore.annotations.Annotations;

import java.util.PriorityQueue;

import static mindustry.Vars.control;

@Annotations.ImplEntries
public class MatrixGridBlock extends DistNetBlock implements DistMatrixUnitComp{
  @Annotations.FieldKey("requestFactories") public ObjectMap<GridChildType, ObjectMap<ContentType, RequestFactory>> reqFact = new ObjectMap<>();
  public int bufferCapacity = 256;
  
  public MatrixGridBlock(String name){
    super(name);
    displayFlow = false;
    showGasFlow = false;
    hasItems = hasLiquids = hasGases = true;
    configurable = true;
    
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
  public void appliedConfig(){
    super.appliedConfig();
    config(byte[].class, (MatrixGridBuild e, byte[] code) -> {
      TargetConfigure c = new TargetConfigure();
      c.read(code);
      
      Building t = Vars.world.build(c.position);
      
      if(c.isClear){
        if(t instanceof IOPointBlock.IOPoint){
          ((IOPointBlock.IOPoint<?>) t).applyConfig(null);
        }
        TargetConfigure oldCfg = e.configMap.remove(c.position);
        if(oldCfg != null) e.configs.remove(oldCfg);
        if(!(t instanceof IOPointBlock.IOPoint)){
          e.gridRemove(t);
        }
      }
      else{
        if(t instanceof IOPointBlock.IOPoint){
          ((IOPointBlock.IOPoint<?>) t).applyConfig(c);
        }
        TargetConfigure oldCfg = e.configMap.put(c.position, c);
        if(oldCfg != null) e.configs.remove(oldCfg);
        e.configs.add(c);
        e.grid.remove(t);
        c.eachChildType((type, map) -> e.gridAdd(t, type, c.priority));
      }
      
      e.releaseRequest();
    });
  }
  
  public void assignFactory(){
    setFactory(GridChildType.output, ContentType.item, new RequestFactories.ReadItemRequestFactory());
    setFactory(GridChildType.input, ContentType.item, new RequestFactories.PutItemRequestFactory());
    setFactory(GridChildType.acceptor, ContentType.item, new RequestFactories.AcceptItemRequestFactory());
  }
  
  @SuppressWarnings("rawtypes")
  @Annotations.ImplEntries
  public class MatrixGridBuild extends DistNetBuild implements DistMatrixUnitBuildComp, SecondableConfigBuildComp{
    public MatrixGrid grid = new MatrixGrid(this);
    
    protected PriorityQueue<TargetConfigure> configs = new PriorityQueue<>((a, b) -> a.priority - b.priority);
    protected IntMap<TargetConfigure> configMap = new IntMap<>();
    protected IntMap<IOPointBlock.IOPoint> ioPoints = new IntMap<>();
    
    public boolean configIOPoint = false;
  
    public ObjectMap<DistBuffers<?>, BaseBuffer<?, ?, ?>> buffers = new ObjectMap<>();
    
    public ObjectSet<DistRequestBase> requests = new ObjectSet<>();
    
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      initBuffers();
      
      items = (ItemModule) buffers.get(DistBuffers.itemBuffer).generateBindModule();
      return this;
    }
  
    @Override
    public boolean gridValid(){
      return distributor.network.netValid();
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
  
      for(ObjectMap.Entry<GridChildType, ObjectMap<ContentType, RequestFactory>> entry : requestFactories()){
        for(ContentType cType : entry.value.keys()){
          DistRequestBase request = createRequest(entry.key, cType);
          if(request == null) continue;
          requests.add(request);
          distributor.assign(request);
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
        releaseRequest();
      }
    }
  
    @Override
    public void updateTile(){
      if(gridValid()){
        for(ObjectMap<ContentType, RequestFactory> value: requestFactories().values()){
          for(RequestFactory factory : value.values()){
            factory.updateIO(this);
          }
        }
        
        for(DistRequestBase request : requests){
          request.update();
        }
      }
      
      if(configIOPoint && control.input.block != Sgl.ioPoint) configIOPoint = false;
    }
  
    @Override
    public boolean onConfigureTileTapped(Building other){
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
        if(tileValid(other.tile) && gridValid() && configValid(other)){
          Sgl.ui.secConfig.showOn(other);
          return false;
        }
        else{
          configIOPoint = false;
          Sgl.ioPoint.resetCurrPlacement();
          control.input.block = null;
          return true;
        }
      }
    }
  }
}
