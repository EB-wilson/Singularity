package singularity.world.components;

import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import universecore.annotations.Annotations;

public interface ChainsBlockComp{
  @Annotations.BindField("maxChainsWidth")
  default int maxWidth(){
    return 0;
  }

  @Annotations.BindField("maxChainsHeight")
  default int maxHeight(){
    return 0;
  }

  default boolean chainable(ChainsBlockComp other){
    return getClass().isAssignableFrom(other.getClass());
  }

  @Annotations.MethodEntry(entryMethod = "setStats", context = "stats -> stats")
  default void setChainsStats(Stats stats){
    stats.add(SglStat.maxStructureSize, "@x@", maxWidth(), maxHeight());
  }
}
