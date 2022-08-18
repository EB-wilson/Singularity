package singularity.world.components;

import arc.Core;
import arc.util.Strings;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.meta.Stats;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universecore.annotations.Annotations;

import static singularity.world.components.HeatBuildComp.getTemperature;

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
  @Annotations.BindField("baseHeatCapacity")
  default float baseHeatCapacity(){
    return 0;
  }

  @Annotations.MethodEntry(entryMethod = "setBars")
  default void setHeatBars(){
    if(this instanceof Block b){
      b.addBar("temperature", entity -> {
        HeatBuildComp ent = (HeatBuildComp) entity;
        return new Bar(
            () -> Core.bundle.get("misc.temperature") + ":" + Strings.autoFixed(ent.temperature(), 2) + SglStatUnit.temperature.localized() +
                "-" + Core.bundle.get("misc.heat") + ":" + Strings.autoFixed(ent.heat()/1000, 0) + SglStatUnit.kHeat.localized(),
            () -> Pal.bar,
            () -> ent.absTemperature()/maxTemperature()
        );
      });
    }
  }
  
  /**设置Stats统计信息*/
  @Annotations.MethodEntry(entryMethod = "setStats", context = "stats -> stats")
  default void setHeatStats(Stats stats){
    stats.add(SglStat.maxTemperature, getTemperature(maxTemperature()), SglStatUnit.temperature);
    stats.add(SglStat.baseHeatCapacity, baseHeatCapacity()/1000, SglStatUnit.heatCapacity);
    stats.add(SglStat.heatCoefficient, heatCoefficient());
  }
}
