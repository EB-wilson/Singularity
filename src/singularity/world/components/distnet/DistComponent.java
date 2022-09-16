package singularity.world.components.distnet;

import arc.struct.ObjectMap;
import singularity.world.distribution.DistBuffers;

public interface DistComponent{
  ObjectMap<DistBuffers<?>, Integer> bufferSize();
  
  default int computingPower(){
    return 0;
  }
  
  default int frequencyOffer(){
    return 0;
  }

  boolean componentValid();
}
