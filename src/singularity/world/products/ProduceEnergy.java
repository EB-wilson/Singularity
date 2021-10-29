package singularity.world.products;

import mindustry.gen.Building;
import mindustry.world.meta.Stats;
import singularity.world.blockComp.NuclearEnergyBuildComp;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universeCore.entityComps.blockComps.ProducerBuildComp;
import universeCore.world.producers.BaseProduce;
import universeCore.world.producers.ProduceType;

public class ProduceEnergy<T extends Building & NuclearEnergyBuildComp & ProducerBuildComp> extends BaseProduce<T>{
  public float product;
  
  public ProduceEnergy(float product){
    this.product = product;
  }
  
  @Override
  public ProduceType<ProduceEnergy<?>> type(){
    return SglProduceType.energy;
  }
  
  @Override
  public void produce(T entity){
  }
  
  @Override
  public void update(T entity){
    entity.handleEnergy(product*entity.consDelta(parent)*entity.productMultiplier(this));
    if(entity.getEnergy() > entity.getNuclearBlock().energyCapacity()) entity.energy().set(entity.getNuclearBlock().energyCapacity());
  }
  
  @Override
  public boolean valid(T entity){
    return true;
  }
  
  @Override
  public void dump(T entity){
    entity.dumpEnergy();
  }
  
  @Override
  public void display(Stats stats){
    stats.add(SglStat.productEnergy, product*60, SglStatUnit.neutronFluxSecond);
  }
}
