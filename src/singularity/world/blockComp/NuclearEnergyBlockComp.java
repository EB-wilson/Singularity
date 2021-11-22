package singularity.world.blockComp;

import mindustry.world.meta.Stats;

/**Consume组件，为方块添加可标记消耗的功能
 * 若使用非默认命名则需要重写调用方法*/
public interface NuclearEnergyBlockComp{
  boolean hasEnergy();
  
  float resident();
  
  float energyCapacity();
  
  boolean outputEnergy();
  
  boolean consumeEnergy();
  
  boolean energyBuffered();
  
  float basicPotentialEnergy();
  
  float maxEnergyPressure();
  
  default void setNuclearStats(Stats stats){
  
  }
}
