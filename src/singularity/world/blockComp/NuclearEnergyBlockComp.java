package singularity.world.blockComp;

import mindustry.world.meta.Stats;
import universeCore.annotations.Annotations;

/**Consume组件，为方块添加可标记消耗的功能
 * 若使用非默认命名则需要重写调用方法*/
public interface NuclearEnergyBlockComp{
  @Annotations.BindField("hasEnergy")
  default boolean hasEnergy(){
    return false;
  }
  
  @Annotations.BindField("resident")
  default float resident(){
    return 0;
  }
  
  @Annotations.BindField("energyCapacity")
  default float energyCapacity(){
    return 0;
  }
  
  @Annotations.BindField("outputEnergy")
  default boolean outputEnergy(){
    return false;
  }
  
  @Annotations.BindField("consumeEnergy")
  default boolean consumeEnergy(){
    return false;
  }
  
  @Annotations.BindField("energyBuffered")
  default boolean energyBuffered(){
    return false;
  }
  
  @Annotations.BindField("basicPotentialEnergy")
  default float basicPotentialEnergy(){
    return 0;
  }
  
  @Annotations.BindField("maxEnergyPressure")
  default float maxEnergyPressure(){
    return 0;
  }
  
  default void setNuclearStats(Stats stats){
  
  }
}
