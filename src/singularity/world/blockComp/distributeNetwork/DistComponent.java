package singularity.world.blockComp.distributeNetwork;

import arc.struct.ObjectMap;
import singularity.world.distribution.DistBuffers;

public interface DistComponent extends DistElementBlockComp{
  default ObjectMap<DistBuffers<?>, Integer> bufferSize(){
    return DistBuffers.defBufferCapacity;
  }
  
  default int computingPower(){
    return 8;
  }
  
  default int frequencyOffer(){
    return 8;
  }
}
