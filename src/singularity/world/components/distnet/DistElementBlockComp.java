package singularity.world.components.distnet;

import arc.Core;
import arc.util.Strings;
import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universecore.annotations.Annotations;

public interface DistElementBlockComp{
  @Annotations.BindField(value = "topologyUse", initialize = "1")
  default int topologyUse(){
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

  @Annotations.MethodEntry(entryMethod = "setStats", context = "stats -> stats")
  default void setDistNetStats(Stats stats){
    if(matrixEnergyUse() > 0) stats.add(SglStat.matrixEnergyUse,
        Strings.autoFixed(matrixEnergyUse()*60, 2) + SglStatUnit.matrixEnergy.localized() + Core.bundle.get("misc.perSecond"));
    if(matrixEnergyCapacity() > 0) stats.add(SglStat.matrixEnergyCapacity, matrixEnergyCapacity(), SglStatUnit.matrixEnergy);
    if(topologyUse() > 0) stats.add(SglStat.topologyUse, topologyUse());
  }
}
