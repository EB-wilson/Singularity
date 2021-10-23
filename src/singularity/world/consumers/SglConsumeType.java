package singularity.world.consumers;

import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeType;

public class SglConsumeType<T extends BaseConsume<?>> extends UncConsumeType<T>{
  public SglConsumeType(Class<T> type){
    super(type);
  }
  
  public static final UncConsumeType<SglConsumeGases> gas = add(SglConsumeGases.class);
  public static final UncConsumeType<SglConsumeEnergy> energy = add(SglConsumeEnergy.class);
}
