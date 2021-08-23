package singularity.world.products;

import mindustry.gen.Building;
import singularity.world.blockComp.GasBuildComp;
import universeCore.world.producers.*;

public class SglProduceType<T extends BaseProduce, R>{
  public static final ProduceType<ProduceItems, Building> item = ProduceType.item;
  public static final ProduceType<ProduceLiquids, Building> liquid = ProduceType.liquid;
  public static final ProduceType<ProducePower, Building> power = ProduceType.power;
  public static final ProduceType<ProduceGases, GasBuildComp> gas = new ProduceType<>(ProduceGases.class, GasBuildComp.class);
  //public static final ProduceType<ProduceEnergy> energy = new ProduceType<>(ProduceEnergy.class);
}
