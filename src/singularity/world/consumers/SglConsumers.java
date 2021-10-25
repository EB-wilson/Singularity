package singularity.world.consumers;

import singularity.type.Gas;
import singularity.type.GasStack;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.BaseConsumers;

import java.util.concurrent.atomic.AtomicReference;

public class SglConsumers extends BaseConsumers{
  public SglConsumers(boolean optional){
    super(optional);
  }
  
  public SglConsumeGases gas(Gas gas, float amount){
    return add(new SglConsumeGases(new GasStack[]{new GasStack(gas, amount)}));
  }
  
  public SglConsumeGases gases(GasStack[] stack){
    return add(new SglConsumeGases(stack));
  }
  
  public SglConsumeEnergy energy(float usage){
    return add(new SglConsumeEnergy(usage));
  }
  
  public BaseConsume<?> first(){
    AtomicReference<BaseConsume<?>> result = new AtomicReference<>();
    cons.forEach((t, c) -> {
      if(result.get() == null && c != null){
        result.set(c);
      }
    });
    
    return result.get();
  }
}
