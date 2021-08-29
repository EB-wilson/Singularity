package singularity.world.consumers;

import singularity.world.blockComp.GasBuildComp;
import singularity.world.blockComp.NuclearEnergyBuildComp;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeType;

public class SglConsumeType<T extends BaseConsume, R> extends UncConsumeType<T, R>{
  public SglConsumeType(Class<T> type, Class<R> requireEntityType){
    super(type, requireEntityType);
  }
  
  public static final UncConsumeType<SglConsumeGases, GasBuildComp> gas = new SglConsumeType<>(SglConsumeGases.class, GasBuildComp.class);
  public static final UncConsumeType<SglConsumeEnergy, NuclearEnergyBuildComp> energy = new SglConsumeType<>(SglConsumeEnergy.class, NuclearEnergyBuildComp.class);
}
