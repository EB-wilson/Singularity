package singularity.world.meta;

import arc.struct.Seq;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import universecore.util.handler.FieldHandler;

public class SglStat{
  public static final Stat
      componentBelongs = create("componentBelongs", SglStatCat.structure),
      maxStructureSize = create("maxStructureSize", SglStatCat.structure),
      maxChildrenNodes = create("maxChildrenNodes", SglStatCat.structure),
      linkDirections = create("linkDirections", SglStatCat.structure),

      energyCapacity = create("energyCapacity", SglStatCat.neutron),
      energyResident = create("energyResident", SglStatCat.neutron),
      basicPotentialEnergy = create("basicPotentialEnergy", SglStatCat.neutron),
      maxEnergyPressure = create("maxEnergyPressure", SglStatCat.neutron),
      consumeEnergy = create("consumeEnergy", SglStatCat.neutron),
      productEnergy = create("productEnergy", SglStatCat.neutron),

      matrixEnergyUse = create("matrixEnergyUse", SglStatCat.matrix),
      matrixEnergyCapacity = create("matrixEnergyCapacity", SglStatCat.matrix),
      topologyUse = create("topologyUse", SglStatCat.matrix),
      maxMatrixLinks = create("maxMatrixLinks", SglStatCat.matrix),

      bufferSize = create("bufferSize", SglStatCat.matrix),
      computingPower = create("computingPower", SglStatCat.matrix),
      topologyCapacity = create("topologyCapacity", SglStatCat.matrix),

      heatProduct = create("heatProduct", SglStatCat.heat),
      maxHeat = create("maxHeat", SglStatCat.heat),

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
