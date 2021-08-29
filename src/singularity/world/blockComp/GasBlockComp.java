package singularity.world.blockComp;

import universeCore.entityComps.blockComps.FieldGetter;

/**Gases组件，表明此方块可以拥有气体
 * 必须创建的变量：
 * <pre>{@code
 *   boolean [hasGases]
 *   float [maxGasPressure]
 *   float [gasCapacity]
 * }<pre/>
 * 若使用非默认命名则需要重写调用方法*/
public interface GasBlockComp extends FieldGetter{
  default boolean hasGases(){
    return getField(boolean.class, "hasGases");
  }
  
  default boolean outputGases(){
    return getField(boolean.class, "outputGases");
  }
  
  default float maxGasPressure(){
    return getField(float.class, "maxGasPressure");
  }
  
  default float gasCapacity(){
    return getField(float.class, "gasCapacity");
  }
}
