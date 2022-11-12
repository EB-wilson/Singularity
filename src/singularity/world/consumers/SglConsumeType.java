package singularity.world.consumers;

import mindustry.ctype.ContentType;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.ConsumeType;

@SuppressWarnings("unchecked")
public class SglConsumeType<T extends BaseConsume<?>> extends ConsumeType<T>{
  public SglConsumeType(Class<T> type, ContentType cType){
    super(type, cType);
  }
  
  public static final ConsumeType<SglConsumeEnergy<?>> energy = (ConsumeType<SglConsumeEnergy<?>>) add(SglConsumeEnergy.class, null);
  public static final ConsumeType<SglConsumeMedium<?>> medium = (ConsumeType<SglConsumeMedium<?>>) add(SglConsumeMedium.class, null);
}
