package singularity.world.consumers;

import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.ctype.Content;
import mindustry.gen.Building;
import mindustry.world.meta.Stats;
import singularity.world.components.NuclearEnergyBuildComp;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.ConsumeType;

public class SglConsumeEnergy<T extends Building & NuclearEnergyBuildComp & ConsumerBuildComp> extends BaseConsume<T>{
  public boolean buffer = false;
  public float usage;

  public SglConsumeEnergy(float usage){
    this.usage = usage;
  }
  
  public void buffer(){
    this.buffer = true;
  }
  
  @Override
  public ConsumeType<SglConsumeEnergy<?>> type(){
    return SglConsumeType.energy;
  }

  @Override
  public void merge(BaseConsume<T> baseConsume){
    if(baseConsume instanceof SglConsumeEnergy cons){
      buffer |= cons.buffer;
      usage += cons.usage;

      return;
    }
    throw new IllegalArgumentException("only merge consume with same type");
  }

  @Override
  public void consume(T entity){
    if(buffer) entity.handleEnergy(-usage*60*multiple(entity));
  }

  @Override
  public void update(T entity) {
    if(!buffer){
      entity.handleEnergy(-usage*parent.delta(entity));
    }
  }

  @Override
  public void display(Stats stats) {
    stats.add(SglStat.consumeEnergy, usage*60, SglStatUnit.neutronFluxSecond);
  }

  @Override
  public void build(T entity, Table table) {
    table.row();
  }

  @Override
  public float efficiency(T entity){
    if(entity.energy() == null) return 0;
    if(buffer){
      return entity.energy().getEnergy() >= usage*60*multiple(entity)? 1: 0;
    }
    return Mathf.clamp(entity.energy().getEnergy()/(usage*12.5f*multiple(entity)));
  }

  @Override
  public Seq<Content> filter(){
    return null;
  }
}
