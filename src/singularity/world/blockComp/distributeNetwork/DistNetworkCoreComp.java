package singularity.world.blockComp.distributeNetwork;

import singularity.world.modules.DistCoreModule;

public interface DistNetworkCoreComp extends DistElementBuildComp{
  @Override
  DistCoreModule distributor();
  
  default void updateDistNetwork(){
    distributor().update();
  }
}
