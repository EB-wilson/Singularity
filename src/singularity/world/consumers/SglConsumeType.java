package singularity.world.consumers;

import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeType;

@SuppressWarnings("unchecked")
public class SglConsumeType<T extends BaseConsume<?>> extends UncConsumeType<T>{
  public SglConsumeType(Class<T> type){
    super(type);
  }
  
  public static final UncConsumeType<SglConsumeGases<?>> gas = (UncConsumeType<SglConsumeGases<?>>) add(SglConsumeGases.class);
  public static final UncConsumeType<SglConsumeEnergy<?>> energy = (UncConsumeType<SglConsumeEnergy<?>>) add(SglConsumeEnergy.class);
}
