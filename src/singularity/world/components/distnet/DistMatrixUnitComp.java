package singularity.world.components.distnet;

import universecore.annotations.Annotations;

public interface DistMatrixUnitComp{
  @Annotations.BindField("bufferCapacity")
  default int bufferCapacity(){
    return 0;
  }
}
