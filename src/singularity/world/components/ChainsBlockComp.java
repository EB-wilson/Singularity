package singularity.world.components;

import universecore.annotations.Annotations;

public interface ChainsBlockComp{
  @Annotations.BindField("maxChainsWidth")
  default int maxWidth(){
    return 0;
  }

  @Annotations.BindField("maxChainsHeight")
  default int maxHeight(){
    return 0;
  }

  default boolean chainable(ChainsBlockComp other){
    return getClass().isAssignableFrom(other.getClass());
  }
}
