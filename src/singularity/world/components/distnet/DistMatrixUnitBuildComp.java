package singularity.world.components.distnet;

import arc.math.geom.Point2;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.OrderedMap;
import arc.struct.OrderedSet;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.world.blocks.distribute.TargetConfigure;
import singularity.world.blocks.distribute.matrixGrid.RequestHandlers.RequestHandler;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import universecore.annotations.Annotations;
import universecore.util.Empties;
import universecore.util.colletion.TreeSeq;

@SuppressWarnings("rawtypes")
public interface DistMatrixUnitBuildComp extends DistElementBuildComp{
  @Annotations.BindField(value = "tempFactories", initialize = "new arc.struct.ObjectMap<>()")
  default ObjectMap<GridChildType, ObjectMap<ContentType, RequestHandler>> tempFactories(){
    return null;
  }

  @Annotations.BindField(value = "requests", initialize = "new arc.struct.OrderedSet<>()")
  default OrderedSet<DistRequestBase> requests(){
    return null;
  }

  @Annotations.BindField(value = "grid", initialize = "new singularity.world.distribution.MatrixGrid(this)")
  default MatrixGrid matrixGrid(){
    return null;
  }

  @Annotations.BindField(value = "configs", initialize = "new universecore.util.colletion.TreeSeq<>((a, b) -> b.priority - a.priority)")
  default TreeSeq<TargetConfigure> configs(){
    return null;
  }

  @Annotations.BindField(value = "buffers", initialize = "new arc.struct.OrderedMap<>()")
  default OrderedMap<DistBufferType<?>, BaseBuffer<?, ?, ?>> buffers(){
    return null;
  }

  @Annotations.BindField(value = "requestHandlerMap", initialize = "new arc.struct.ObjectMap()")
  default ObjectMap<DistRequestBase, RequestHandler<?>>  requestHandlerMap(){
    return null;
  }
  
  @Annotations.BindField("ioPoints")
  default ObjectSet<IOPointComp> ioPoints(){
    return null;
  }

  @Annotations.MethodEntry(entryMethod = "update")
  default void updateGrid(){
    if(gridValid()) matrixGrid().update();
  }

  @SuppressWarnings("unchecked")
  default <T extends BaseBuffer<?, ?, ?>> T getBuffer(DistBufferType<T> buff){
    return (T) buffers().get(buff);
  }
  
  default void initBuffers(){
    for(DistBufferType<?> buffer : DistBufferType.all){
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

  default void releaseRequest(){
    for(DistRequestBase request : requests()){
      request.kill();
    }
    requests().clear();

    resetFactories();

    for(TargetConfigure config : configs()){
      config.eachChildType((type, map) -> {
        for(ContentType contType : map.keys()){
          addConfig(type, contType, config);
        }
      });
    }

    requestHandlerMap().clear();
    for(ObjectMap.Entry<GridChildType, ObjectMap<ContentType, RequestHandler>> entry : tempFactories()){
      for(ObjectMap.Entry<ContentType, RequestHandler> e: entry.value){
        DistRequestBase request = createRequest(entry.key, e.key);
        if(request == null) continue;
        requests().add(request);
        distributor().assign(request, false);

        requestHandlerMap().put(request, e.value);
      }
    }

    for(DistRequestBase request : requests()){
      request.init(distributor().network);
    }
  }

  default boolean configValid(Building entity){
    if(entity instanceof IOPointComp io && (io.gridConfig() == null || io.parent() == this)) return true;
    return Sgl.matrixContainers.getContainer(entity.block) != null;
  }

  default void resetFactories(){
    for (ObjectMap.Entry<GridChildType, ObjectMap<ContentType, RequestHandler>> fac : tempFactories()) {
      for (RequestHandler handler : fac.value.values()) {
        handler.reset();
      }
    }
    tempFactories().clear();
  }

  default void addConfig(GridChildType type, ContentType contType, TargetConfigure cfg){
    Building build = Vars.world.build(getTile().x + Point2.x(cfg.offsetPos), getTile().y + Point2.y(cfg.offsetPos));

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

  default void addIO(IOPointComp io){
    ioPoints().add(io);
    matrixGrid().addConfig(io.gridConfig());
  }
  
  default void removeIO(IOPointComp io){
    ioPoints().remove(io);
    matrixGrid().remove(io.getBuilding());
  }
}
