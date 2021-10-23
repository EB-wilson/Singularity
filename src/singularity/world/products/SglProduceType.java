package singularity.world.products;

import universeCore.world.producers.BaseProduce;
import universeCore.world.producers.ProduceType;

public class SglProduceType<T extends BaseProduce<?>> extends ProduceType<T>{
  public SglProduceType(Class<T> type){
    super(type);
  }
  
  public static final ProduceType<ProduceGases> gas = add(ProduceGases.class);
  public static final ProduceType<ProduceEnergy> energy = add(ProduceEnergy.class);
}
