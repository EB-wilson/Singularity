package singularity.world.blockComp;

import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universeCore.entityComps.blockComps.FieldGetter;

import static singularity.world.blockComp.HeatBuildComp.getTemperature;

public interface HeatBlockComp extends FieldGetter{
  default float maxTemperature(){
    return getField(float.class, "maxTemperature");
  }
  
  default float heatCoefficient(){
    return getField(float.class, "heatCoefficient");
  }
  
  default float baseHeatCapacity(){
    return getField(float.class, "baseHeatCapacity");
  }
  
  default void setHeatStats(Stats stats){
    stats.add(SglStat.maxTemperature, getTemperature(maxTemperature()), SglStatUnit.temperature);
    stats.add(SglStat.baseHeatCapacity, baseHeatCapacity()/1000, SglStatUnit.heatCapacity);
    stats.add(SglStat.heatCoefficient, heatCoefficient());
  }
}
