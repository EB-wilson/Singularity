package singularity.world.products;

import arc.scene.ui.layout.Table;
import mindustry.gen.Building;
import mindustry.world.meta.Stats;
import singularity.world.components.NuclearEnergyBuildComp;
import singularity.world.consumers.SglConsumeEnergy;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universecore.components.blockcomp.ProducerBuildComp;
import universecore.world.producers.BaseProduce;
import universecore.world.producers.ProduceType;

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
  public void buildIcons(Table table) {
    SglConsumeEnergy.buildNuclearIcon(table, product);
  }

  @Override
  public void merge(BaseProduce<T> baseProduce){
    if(baseProduce instanceof ProduceEnergy cons){
      product += cons.product;

      return;
    }
    throw new IllegalArgumentException("only merge product with same type");
  }

  @Override
  public void produce(T entity){
  }
  
  @Override
  public void update(T entity){
    entity.handleEnergy(product*parent.cons.delta(entity)*multiple(entity));
    if(entity.getEnergy() > entity.energyCapacity()) entity.energy().set(entity.energyCapacity());
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
