package singularity.world.components;

import arc.Core;
import arc.util.Strings;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universecore.annotations.Annotations;

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

  @Annotations.MethodEntry(entryMethod = "setStats", context = "stats -> stats")
  default void setGasStats(Stats stats){
    if(!hasGases()) return;
    stats.add(SglStat.gasCapacity, gasCapacity(), StatUnit.none);
    stats.add(SglStat.maxGasPressure, maxGasPressure()*100, SglStatUnit.kPascal);
  }

  @Annotations.MethodEntry(entryMethod = "setBars")
  default void setGasBars(){
    if(!hasGases()) return;
    if(this instanceof Block b){
      b.addBar("gasPressure", ent -> {
        GasBuildComp entity = (GasBuildComp) ent;

        return new Bar(
            () -> Core.bundle.get("fragment.bars.gasPressure") + ":" + Strings.autoFixed(entity.smoothPressure()*100, 0) + "kPa",
            () -> Pal.accent,
            () -> Math.min(entity.smoothPressure()/maxGasPressure(), 1));
      });
    }
  }
}
