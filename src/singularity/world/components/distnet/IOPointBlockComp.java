package singularity.world.components.distnet;

import arc.struct.ObjectMap;
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

  @SuppressWarnings("rawtypes")
  default void setFactory(GridChildType type, ContentType contType, RequestHandlers.RequestHandler factory){
    requestFactories().get(type, ObjectMap::new).put(contType, factory);
  }

  default Block getBlock(){
    return (Block) this;
  }
}
