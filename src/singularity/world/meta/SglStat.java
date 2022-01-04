package singularity.world.meta;

import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import universeCore.util.handler.EnumHandler;
import universeCore.util.handler.FieldHandler;

public class SglStat{
  private static final EnumHandler<Stat> handler = new EnumHandler<>(Stat.class){
    @Override
    public void constructor(Stat instance, Object... param){
      if(param.length == 0){
        FieldHandler.setValue(Stat.class, "category", instance, StatCat.general);
      }
      else{
        FieldHandler.setValue(Stat.class, "category", instance, param[0]);
      }
    }
  };
  
  public static final Stat compressible = handler.addEnumItemTail("compressible", SglStatCat.gas),
      compressor = handler.addEnumItemTail("compressor", SglStatCat.gas),
      gasCapacity = handler.addEnumItemTail("gasCapacity", SglStatCat.gas),
      maxGasPressure = handler.addEnumItemTail("maxGasPressure", SglStatCat.gas),
  
      consumeEnergy = handler.addEnumItemTail("consumeEnergy", SglStatCat.nuclear),
      productEnergy = handler.addEnumItemTail("productEnergy", SglStatCat.nuclear),
  
      heatProduct = handler.addEnumItemTail("heatProduct", SglStatCat.heat),
      baseHeatCapacity = handler.addEnumItemTail("baseHeatCapacity", SglStatCat.heat),
      maxTemperature = handler.addEnumItemTail("maxTemperature", SglStatCat.heat),
      heatCoefficient = handler.addEnumItemTail("heatCoefficient", SglStatCat.heat),
  
      deltaHeat = handler.addEnumItemTail("deltaHeat", SglStatCat.reaction),
      requirePressure = handler.addEnumItemTail("requirePressure", SglStatCat.reaction),
      requireTemperature = handler.addEnumItemTail("requireTemperature", SglStatCat.reaction),
      consume = handler.addEnumItemTail("consume", SglStatCat.reaction),
      product = handler.addEnumItemTail("product", SglStatCat.reaction),
  
      generatorType = handler.addEnumItemTail("generator", SglStatCat.generator),
      
      autoSelect = handler.addEnumItem("autoSelect", 46, StatCat.crafting),
      controllable = handler.addEnumItem("controllable", 47, StatCat.crafting),
  
      effect = handler.addEnumItemTail("effect", StatCat.function);
}
