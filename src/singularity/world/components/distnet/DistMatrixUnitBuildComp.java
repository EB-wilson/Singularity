package singularity.world.components.distnet;

import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.blocks.distribute.matrixGrid.RequestHandlers.RequestHandler;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import universecore.annotations.Annotations;
import universecore.util.Empties;

@SuppressWarnings("rawtypes")
public interface DistMatrixUnitBuildComp extends DistElementBuildComp{
  @Annotations.BindField(value = "tempFactories", initialize = "new arc.struct.ObjectMap<>()")
  default ObjectMap<GridChildType, ObjectMap<ContentType, RequestHandler>> tempFactories(){
    return null;
  }

  @Annotations.BindField(value = "grid", initialize = "new singularity.world.distribution.MatrixGrid(this)")
  default MatrixGrid matrixGrid(){
    return null;
  }

  @Annotations.BindField(value = "buffers", initialize = "new arc.struct.OrderedMap()")
  default OrderedMap<DistBuffers<?>, BaseBuffer<?, ?, ?>> buffers(){
    return null;
  }
  
  @Annotations.BindField("ioPoints")
  default IntMap<IOPointComp> ioPoints(){
    return null;
  }

  @Annotations.MethodEntry(entryMethod = "update")
  default void updateGrid(){
    if(gridValid()) matrixGrid().update();
  }

  @SuppressWarnings("unchecked")
  default <T extends BaseBuffer<?, ?, ?>> T getBuffer(DistBuffers<T> buff){
    return (T) buffers().get(buff);
  }
  
  default void initBuffers(){
    for(DistBuffers<?> buffer : DistBuffers.all){
      buffers().put(buffer, buffer.get(getMatrixBlock().bufferCapacity()));
    }
  }
  
  default boolean gridValid(){
    return true;
  }
  
  @Override
  default int priority(){
    return matrixGrid().priority;
  }
  
  @Override
  default void priority(int priority){
    matrixGrid().priority = priority;
    distributor().network.priorityModified(this);
  }
  
  default boolean configValid(Building entity){
    if(entity instanceof IOPointComp && ((IOPointComp) entity).parent() == this) return true;
    return entity.block.hasItems || entity.block.hasLiquids;
  }

  default void resetFactories(){
    tempFactories().clear();
  }

  default void addConfig(GridChildType type, ContentType contType, DistTargetConfigTable.TargetConfigure cfg){
    Building build = Vars.world.build(cfg.position);

    RequestHandler factory = build instanceof IOPointComp comp?
        comp.getIOBlock().requestFactories().get(type, Empties.nilMapO()).get(contType): null;

    if(factory != null){
      ObjectMap<ContentType, RequestHandler> map = tempFactories().get(type, ObjectMap::new);
      if(!map.containsKey(contType)) map.put(contType, factory);
      factory.addParseConfig(cfg);
    }
  }

  default DistRequestBase createRequest(GridChildType type, ContentType contType){
    RequestHandler factory = tempFactories().get(type, Empties.nilMapO()).get(contType);
    if(factory == null) return null;
    DistRequestBase result = factory.makeRequest(this);
    factory.reset();
    return result;
  }
  
  default DistMatrixUnitComp getMatrixBlock(){
    return getBlock(DistMatrixUnitComp.class);
  }
  
  void ioPointConfigBackEntry(IOPointComp ioPoint);
  
  boolean tileValid(Tile tile);
  
  void drawValidRange();
  
  default void removeIO(int pos){
    ioPoints().remove(pos);
    matrixGrid().remove(Vars.world.build(pos));
  }
}
