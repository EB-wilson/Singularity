package singularity.world.blockComp.distributeNetwork;

import mindustry.world.meta.Stats;

public interface DistElementBlockComp{
  int frequencyUse();
  
  default void setDistNetStats(Stats stats){
  
  }
}
