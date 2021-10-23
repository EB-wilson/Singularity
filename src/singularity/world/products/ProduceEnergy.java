package singularity.world.products;

import mindustry.world.meta.Stats;
import singularity.world.blockComp.NuclearEnergyBuildComp;
import universeCore.world.producers.BaseProduce;
import universeCore.world.producers.ProduceType;

public class ProduceEnergy extends BaseProduce<NuclearEnergyBuildComp>{
  public float product;
  
  public ProduceEnergy(float product){
    this.product = product;
  }
  
  @Override
  public ProduceType<ProduceEnergy> type(){
    return SglProduceType.energy;
  }
  
  @Override
  public void produce(NuclearEnergyBuildComp entity){
  }
  
  @Override
  public void update(NuclearEnergyBuildComp entity){
    entity.handleEnergy(0);
  }
  
  @Override
  public boolean valid(NuclearEnergyBuildComp entity){
    return entity.getEnergy() != 0;
  }
  
  @Override
  public void dump(NuclearEnergyBuildComp entity){
    entity.dumpEnergy();
  }
  
  @Override
  public void display(Stats stats){
  
  }
}
