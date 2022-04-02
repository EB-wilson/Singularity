package singularity.world.consumers;

import mindustry.ctype.ContentType;
import singularity.type.SglContents;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.UncConsumeType;

@SuppressWarnings("unchecked")
public class SglConsumeType<T extends BaseConsume<?>> extends UncConsumeType<T>{
  public SglConsumeType(Class<T> type, ContentType cType){
    super(type, cType);
  }
  
  public static final UncConsumeType<SglConsumeGases<?>> gas = (UncConsumeType<SglConsumeGases<?>>) add(SglConsumeGases.class, SglContents.gas);
  public static final UncConsumeType<SglConsumeEnergy<?>> energy = (UncConsumeType<SglConsumeEnergy<?>>) add(SglConsumeEnergy.class, null);
  public static final UncConsumeType<SglConsumeMedium<?>> medium = (UncConsumeType<SglConsumeMedium<?>>) add(SglConsumeMedium.class, null);
}
