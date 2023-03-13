package singularity.world.components.distnet;

import arc.struct.ObjectMap;
import arc.struct.OrderedSet;
import mindustry.ctype.ContentType;
import mindustry.world.Block;
import singularity.world.blocks.distribute.matrixGrid.RequestHandlers;
import singularity.world.distribution.GridChildType;
import universecore.annotations.Annotations;

public interface IOPointBlockComp{
  @SuppressWarnings("rawtypes")
  @Annotations.BindField(value = "requestFactories", initialize = "new arc.struct.ObjectMap<>()")
  default ObjectMap<GridChildType, ObjectMap<ContentType, RequestHandlers.RequestHandler>> requestFactories(){
    return null;
  }

  @Annotations.BindField(value = "configTypes", initialize = "new arc.struct.OrderedSet<>()")
  default OrderedSet<GridChildType> configTypes(){
    return null;
  }

  @Annotations.BindField(value = "supportContentType", initialize = "new arc.struct.OrderedSet<>()")
  default OrderedSet<ContentType> supportContentType(){
    return null;
  }

  @SuppressWarnings("rawtypes")
  default void setFactory(GridChildType type, ContentType contType, RequestHandlers.RequestHandler factory){
    requestFactories().get(type, ObjectMap::new).put(contType, factory);
    configTypes().add(type);
    supportContentType().add(contType);
  }

  default Block getBlock(){
    return (Block) this;
  }
}
