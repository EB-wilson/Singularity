package singularity.world.components.distnet;

import mindustry.world.meta.Stats;
import universecore.annotations.Annotations;

public interface DistElementBlockComp{
  @Annotations.BindField(value = "frequencyUse", initialize = "1")
  default int frequencyUse(){
    return 0;
  }

  @Annotations.BindField("matrixEnergyUse")
  default float matrixEnergyUse(){
    return 0;
  }

  @Annotations.BindField("matrixEnergyCapacity")
  default float matrixEnergyCapacity(){
    return 0;
  }

  @Annotations.BindField("isNetLinker")
  default boolean isNetLinker(){
    return false;
  }
  
  default void setDistNetStats(Stats stats){}
}
