package singularity.world.consumers;

import mindustry.ctype.ContentType;
import singularity.type.SglContents;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeType;

@SuppressWarnings("unchecked")
public class SglConsumeType<T extends BaseConsume<?>> extends UncConsumeType<T>{
  public SglConsumeType(Class<T> type, ContentType cType){
    super(type, cType);
  }
  
  public static final UncConsumeType<SglConsumeGases<?>> gas = (UncConsumeType<SglConsumeGases<?>>) add(SglConsumeGases.class, SglContents.gas);
  public static final UncConsumeType<SglConsumeEnergy<?>> energy = (UncConsumeType<SglConsumeEnergy<?>>) add(SglConsumeEnergy.class, null);
}
