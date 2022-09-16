package singularity.world.consumers;

import arc.struct.ObjectMap;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.UncConsumeType;

public class SglConsumers extends BaseConsumers{
  public SglConsumers(boolean optional){
    super(optional);
  }
  
  public SglConsumeEnergy<?> energy(float usage){
    return add(new SglConsumeEnergy<>(usage));
  }

  public SglConsumeMedium<?> medium(float cons){
    return add(new SglConsumeMedium<>(cons));
  }
  
  public BaseConsume<?> first(){
    for(ObjectMap.Entry<UncConsumeType<?>, BaseConsume<?>> con: cons){
      if(con.value != null) return con.value;
    }
    return null;
  }
}
