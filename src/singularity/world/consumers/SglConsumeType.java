package singularity.world.consumers;

import mindustry.gen.Building;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blockComp.NuclearEnergyBuildComp;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeItems;
import universeCore.world.consumers.UncConsumeLiquids;
import universeCore.world.consumers.UncConsumePower;
import universeCore.world.consumers.UncConsumeType;

public class SglConsumeType<T extends BaseConsume, R>{
  public static final UncConsumeType<UncConsumeItems, Building> item = UncConsumeType.item;
  public static final UncConsumeType<UncConsumeLiquids, Building> liquid = UncConsumeType.liquid;
  public static final UncConsumeType<UncConsumePower, Building> power = UncConsumeType.power;
  public static final UncConsumeType<SglConsumeGases, GasBuildComp> gas = new UncConsumeType<>(SglConsumeGases.class, GasBuildComp.class);
  public static final UncConsumeType<SglConsumeEnergy, NuclearEnergyBuildComp> energy = new UncConsumeType<>(SglConsumeEnergy.class, NuclearEnergyBuildComp.class);
}
