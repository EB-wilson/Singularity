package singularity.world.consumers;

import mindustry.ctype.MappableContent;
import singularity.type.Gas;
import singularity.type.GasStack;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.BaseConsumers;

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
  
  public BaseConsume first(){
    for(BaseConsume c: cons){
      if(c != null) return c;
    }
    return null;
  }
}
