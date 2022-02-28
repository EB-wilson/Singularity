package singularity.world.blockComp.distributeNetwork;

import arc.struct.ObjectMap;
import mindustry.ctype.ContentType;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.request.RequestFactories;
import universeCore.annotations.Annotations;

public interface DistMatrixUnitComp{
  @Annotations.BindField("bufferCapacity")
  default int bufferCapacity(){
    return 0;
  }
  
  @Annotations.BindField("requestFactories")
  default ObjectMap<GridChildType, ObjectMap<ContentType, RequestFactories.RequestFactory>> requestFactories(){
    return null;
  }
  
  default void setFactory(GridChildType type, ContentType contType, RequestFactories.RequestFactory factory){
    requestFactories().get(type, ObjectMap::new).put(contType, factory);
  }
}
