package singularity.world.blockComp.distributeNetwork;

import arc.func.Boolf;
import arc.func.Func;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import mindustry.ctype.ContentType;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.request.DistRequestBase;
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
  
  @Annotations.BindField("transBackFactories")
  default OrderedMap<Boolf<? extends DistMatrixUnitBuildComp>, Func<DistMatrixUnitBuildComp, ? extends DistRequestBase<?>>> transBackFactories(){
    return null;
  }
  
  default void setFactory(GridChildType type, ContentType contType, RequestFactories.RequestFactory factory){
    requestFactories().get(type, ObjectMap::new).put(contType, factory);
  }
  
  default void setTransBackFactory(Boolf<? extends DistMatrixUnitBuildComp> weaker, Func<DistMatrixUnitBuildComp, ? extends DistRequestBase<?>> prov){
    transBackFactories().put(weaker, prov);
  }
}
