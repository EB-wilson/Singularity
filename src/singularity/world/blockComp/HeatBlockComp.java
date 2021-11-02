package singularity.world.blockComp;

import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universeCore.entityComps.blockComps.FieldGetter;

import static singularity.world.blockComp.HeatBuildComp.getTemperature;

public interface HeatBlockComp extends FieldGetter{
  /**最大容许温度，超过此温度则触发高温触发器*/
  default float maxTemperature(){
    return getField(float.class, "maxTemperature");
  }
  
  /**导热系数，决定方块与大气交换热量的速度*/
  default float heatCoefficient(){
    return getField(float.class, "heatCoefficient");
  }
  
  /**基础热容，之后热容量的变化基于此值进行*/
  default float baseHeatCapacity(){
    return getField(float.class, "baseHeatCapacity");
  }
  
  /**设置Stats统计信息*/
  default void setHeatStats(Stats stats){
    stats.add(SglStat.maxTemperature, getTemperature(maxTemperature()), SglStatUnit.temperature);
    stats.add(SglStat.baseHeatCapacity, baseHeatCapacity()/1000, SglStatUnit.heatCapacity);
    stats.add(SglStat.heatCoefficient, heatCoefficient());
  }
}
