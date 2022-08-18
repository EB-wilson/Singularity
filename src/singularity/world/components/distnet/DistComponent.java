package singularity.world.components.distnet;

import arc.struct.ObjectMap;
import singularity.world.distribution.DistBuffers;

public interface DistComponent{
  default ObjectMap<DistBuffers<?>, Integer> bufferSize(){
    return DistBuffers.defBufferCapacity;
  }
  
  default int computingPower(){
    return 0;
  }
  
  default int frequencyOffer(){
    return 0;
  }
}
