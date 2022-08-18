package singularity.world.meta;

import arc.struct.Seq;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import universecore.util.handler.FieldHandler;

public class SglStat{
  public static final Stat
      compressible = create("compressible", SglStatCat.gas),
      compressor = create("compressor", SglStatCat.gas),
      gasCapacity = create("gasCapacity", SglStatCat.gas),
      maxGasPressure = create("maxGasPressure", SglStatCat.gas),
  
      consumeEnergy = create("consumeEnergy", SglStatCat.nuclear),
      productEnergy = create("productEnergy", SglStatCat.nuclear),
  
      heatProduct = create("heatProduct", SglStatCat.heat),
      baseHeatCapacity = create("baseHeatCapacity", SglStatCat.heat),
      maxTemperature = create("maxTemperature", SglStatCat.heat),
      heatCoefficient = create("heatCoefficient", SglStatCat.heat),
  
      deltaHeat = create("deltaHeat", SglStatCat.reaction),
      requirePressure = create("requirePressure", SglStatCat.reaction),
      requireTemperature = create("requireTemperature", SglStatCat.reaction),
      consume = create("consume", SglStatCat.reaction),
      product = create("product", SglStatCat.reaction),

      autoSelect = create("autoSelect", 46, StatCat.crafting),
      controllable = create("controllable", 47, StatCat.crafting),
      special = create("special", 50, StatCat.crafting),
  
      effect = create("effect", StatCat.function);

  private static Stat create(String name, StatCat cat){
    return create(name, Stat.all.size, cat);
  }

  private static Stat create(String name, int index, StatCat cat){
    Seq<Stat> all = Stat.all;
    Stat res = new Stat(name, cat);

    FieldHandler.setValueDefault(res, "id", index);
    all.insert(index, res);

    return res;
  }
}
