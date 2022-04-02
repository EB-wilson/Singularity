package singularity.world.products;

import singularity.type.Gas;
import singularity.type.GasStack;
import universecore.world.producers.BaseProducers;


public class Producers extends BaseProducers{
  public ProduceEnergy<?> energy(float prod){
    return add(new ProduceEnergy<>(prod));
  }
  
  public ProduceGases<?> gas(Gas gas, float amount){
    return add(new ProduceGases<>(new GasStack[]{new GasStack(gas, amount)}));
  }
  
  public ProduceGases<?> gases(GasStack[] gases){
    return add(new ProduceGases<>(gases));
  }
  
  public ProduceGases<?> gases(Object... args){
    return add(new ProduceGases<>(GasStack.with(args)));
  }

  public ProduceMedium<?> medium(float prod){
    return add(new ProduceMedium<>(prod));
  }
}
