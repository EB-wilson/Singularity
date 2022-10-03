package singularity.world.components;

import universecore.annotations.Annotations;

public interface SpliceBlockComp extends ChainsBlockComp{
  @Annotations.BindField("interCorner")
  default boolean interCorner(){
    return false;
  }
}
