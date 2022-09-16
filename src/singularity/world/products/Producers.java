package singularity.world.products;

import universecore.world.producers.BaseProducers;


public class Producers extends BaseProducers{
  public ProduceEnergy<?> energy(float prod){
    return add(new ProduceEnergy<>(prod));
  }

  public ProduceMedium<?> medium(float prod){
    return add(new ProduceMedium<>(prod));
  }
}
