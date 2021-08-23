package singularity.world.blockComp;

import universeCore.entityComps.blockComps.FieldGetter;

/**Consume组件，为方块添加可标记消耗的功能
 * 必须创建的变量：
 * <pre>{@code
 *   boolean [hasEnergy]
 *   boolean [hasEnergyGroup]
 *   boolean [outputEnergy]
 *   boolean [consumeEnergy]
 *   boolean [energyBuffered]
 *   float [resident]
 *   float [energyCapacity]
 *   float [basicPotentialEnergy]
 *   float [maxEnergyPressure]
 * }<pre/>
 * 若使用非默认命名则需要重写调用方法*/
public interface NuclearEnergyBlockComp extends FieldGetter{
  default boolean hasEnergy(){
    return getField(boolean.class, "hasEnergy");
  }
  
  default boolean hasEnergyGroup(){
    return getField(boolean.class, "hasEnergyGroup");
  }
  
  default float resident(){
    return getField(float.class, "resident");
  }
  
  default float energyCapacity(){
    return getField(float.class, "energyCapacity");
  }
  
  default boolean outputEnergy(){
    return getField(boolean.class, "outputEnergy");
  }
  
  default boolean consumeEnergy(){
    return getField(boolean.class, "consumeEnergy");
  }
  
  default boolean energyBuffered(){
    return getField(boolean.class, "energyBuffered");
  }
  
  default float basicPotentialEnergy(){
    return getField(float.class, "basicPotentialEnergy");
  }
  
  default float maxEnergyPressure(){
    return getField(float.class, "maxEnergyPressure");
  }
}
