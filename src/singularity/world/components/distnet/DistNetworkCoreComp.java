package singularity.world.components.distnet;

import singularity.world.modules.DistCoreModule;

public interface DistNetworkCoreComp extends DistElementBuildComp{
  DistCoreModule distCore();
  
  default boolean updateState(){
    return false;
  }
  
  default void updateDistNetwork(){
    distCore().update();
  }
}
