package singularity.world.components.distnet;

import arc.struct.ObjectMap;
import singularity.world.distribution.DistBufferType;
import singularity.world.modules.DistCoreModule;
import universecore.annotations.Annotations;

public interface DistNetworkCoreComp extends DistMatrixUnitBuildComp{
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
