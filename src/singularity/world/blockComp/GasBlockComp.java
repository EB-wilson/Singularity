package singularity.world.blockComp;

import mindustry.world.meta.StatUnit;
import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;

/**Gases组件，表明此方块可以拥有气体
 * 若使用非默认命名则需要重写调用方法*/
public interface GasBlockComp{
  boolean hasGases();
  
  boolean outputGases();
  
  float maxGasPressure();
  
  float gasCapacity();
  
  boolean compressProtect();
  
  default void setGasStats(Stats stats){
    stats.add(SglStat.gasCapacity, gasCapacity(), StatUnit.none);
    stats.add(SglStat.maxGasPressure, maxGasPressure()*100, SglStatUnit.kPascal);
  }
}
