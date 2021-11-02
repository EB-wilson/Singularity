package singularity.world.blockComp;

import mindustry.world.meta.StatUnit;
import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universeCore.entityComps.blockComps.FieldGetter;

/**Gases组件，表明此方块可以拥有气体
 * 必须创建的变量：
 * <pre>{@code
 *   boolean [hasGases]
 *   boolean [outputGases]
 *   float [maxGasPressure]
 *   float [gasCapacity]
 *   boolean [compressProtect]
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
  
  default boolean compressProtect(){
    return getField(boolean.class, "compressProtect");
  }
  
  default boolean classicDumpGas(){
    return getField(boolean.class, "classicDumpGas");
  }
  
  default void setGasStats(Stats stats){
    stats.add(SglStat.gasCapacity, gasCapacity(), StatUnit.none);
    stats.add(SglStat.maxGasPressure, maxGasPressure()*100, SglStatUnit.kPascal);
  }
}
