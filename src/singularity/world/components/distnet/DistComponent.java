package singularity.world.components.distnet;

import arc.struct.ObjectMap;
import singularity.world.distribution.DistBufferType;

public interface DistComponent{
  ObjectMap<DistBufferType<?>, Integer> bufferSize();
  
  default int computingPower(){
    return 0;
  }
  
  default int topologyCapacity(){
    return 0;
  }

  boolean componentValid();
}
