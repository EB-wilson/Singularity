package singularity.world.products;

import singularity.world.blockComp.GasBuildComp;
import universeCore.world.producers.BaseProduce;
import universeCore.world.producers.ProduceType;

public class SglProduceType<T extends BaseProduce, R> extends ProduceType<T, R>{
  public SglProduceType(Class<T> type, Class<R> requireEntityType){
    super(type, requireEntityType);
  }
  
  public static final ProduceType<ProduceGases, GasBuildComp> gas = new SglProduceType<>(ProduceGases.class, GasBuildComp.class);
  //public static final ProduceType<ProduceEnergy> energy = new ProduceType<>(ProduceEnergy.class);
}
