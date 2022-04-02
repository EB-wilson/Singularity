package singularity.world.products;

import universecore.world.producers.BaseProduce;
import universecore.world.producers.ProduceType;

@SuppressWarnings("unchecked")
public class SglProduceType<T extends BaseProduce<?>> extends ProduceType<T>{
  public SglProduceType(Class<T> type){
    super(type);
  }
  
  public static final ProduceType<ProduceGases<?>> gas = (ProduceType<ProduceGases<?>>) add(ProduceGases.class);
  public static final ProduceType<ProduceEnergy<?>> energy = (ProduceType<ProduceEnergy<?>>) add(ProduceEnergy.class);
  public static final ProduceType<ProduceMedium<?>> medium = (ProduceType<ProduceMedium<?>>) add(ProduceMedium.class);
}
