package singularity.world.blockComp;

import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universeCore.annotations.Annotations;

import static singularity.world.blockComp.HeatBuildComp.getTemperature;

public interface HeatBlockComp{
  /**最大容许温度，超过此温度则触发高温触发器*/
  @Annotations.BindField("maxTemperature")
  default float maxTemperature(){
    return 0;
  }
  
  /**导热系数，决定方块与大气交换热量的速度*/
  @Annotations.BindField("heatCoefficient")
  default float heatCoefficient(){
    return 0;
  }
  
  @Annotations.BindField("blockHeatCoff")
  default float blockHeatCoff(){
    return 0;
  }
  
  /**基础热容，之后热容量的变化基于此值进行*/
  default float baseHeatCapacity(){
    return 0;
  }
  
  /**设置Stats统计信息*/
  default void setHeatStats(Stats stats){
    stats.add(SglStat.maxTemperature, getTemperature(maxTemperature()), SglStatUnit.temperature);
    stats.add(SglStat.baseHeatCapacity, baseHeatCapacity()/1000, SglStatUnit.heatCapacity);
    stats.add(SglStat.heatCoefficient, heatCoefficient());
  }
}
