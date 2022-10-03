package singularity.world.components.distnet;

import singularity.world.modules.DistCoreModule;
import universecore.annotations.Annotations;

public interface DistNetworkCoreComp extends DistElementBuildComp, DistComponent{
  @Annotations.BindField("distCore")
  default DistCoreModule distCore(){
    return null;
  }

  @Annotations.BindField("distCore")
  default void distCore(DistCoreModule value){}
  
  default boolean updateState(){
    return false;
  }

  @Annotations.MethodEntry(entryMethod = "updateTile")
  default void updateDistNetwork(){
    distCore().update();
  }
}
