package singularity.world.blockComp;

import mindustry.world.meta.StatUnit;
import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universeCore.annotations.Annotations;

/**Gases组件，表明此方块可以拥有气体
 * 若使用非默认命名则需要重写调用方法*/
public interface GasBlockComp{
  @Annotations.BindField("hasGases")
  default boolean hasGases(){
    return false;
  }
  
  @Annotations.BindField("outputGases")
  default boolean outputGases(){
    return false;
  }
  
  @Annotations.BindField("maxGasPressure")
  default float maxGasPressure(){
    return 0;
  }
  
  @Annotations.BindField("gasCapacity")
  default float gasCapacity(){
    return 0;
  }
  
  @Annotations.BindField("compressProtect")
  default boolean compressProtect(){
    return false;
  }

  default float realCapacity(){
    return gasCapacity()*maxGasPressure();
  }
  
  default void setGasStats(Stats stats){
    stats.add(SglStat.gasCapacity, gasCapacity(), StatUnit.none);
    stats.add(SglStat.maxGasPressure, maxGasPressure()*100, SglStatUnit.kPascal);
  }
}
