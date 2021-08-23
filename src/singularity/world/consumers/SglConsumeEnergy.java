package singularity.world.consumers;

import singularity.world.blockComp.NuclearEnergyBuildComp;
import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.world.meta.Stat;
import mindustry.world.meta.Stats;
import universeCore.entityComps.blockComps.ConsumerBuildComp;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeType;

public class SglConsumeEnergy extends BaseConsume{
  public boolean buffer = false;
  public final float usage;

  public SglConsumeEnergy(float usage){
    this.usage = usage;
  }
  
  public void buffer(){
    this.buffer = true;
  }
  
  @Override
  public UncConsumeType<SglConsumeEnergy, NuclearEnergyBuildComp> type(){
    return SglConsumeType.energy;
  }
  
  @Override
  public void consume(ConsumerBuildComp entity){
    if(buffer) entity.getBuilding(type()).handleEnergy(-usage*60);
  }

  @Override
  public void update(ConsumerBuildComp entity) {
    if(!buffer){
      entity.getBuilding(type()).handleEnergy(-usage*entity.getBuilding().edelta());
    }
  }

  @Override
  public void display(Stats stats) {
    stats.add(Stat.input, table -> {
      table.row();
      table.defaults().left();
      table.add(Core.bundle.get("misc.nuclearEnergy") + ":");
    });
  }

  @Override
  public void build(ConsumerBuildComp entity, Table table) {
    
    table.row();
  }

  @Override
  public boolean valid(ConsumerBuildComp entity){
    if(buffer){
      return entity.getBuilding(type()).energy().getIncluded() >= usage*60*entity.getBuilding().edelta();
    }
    return entity.getBuilding(type()).energy().getIncluded() >= usage;
  }
  
  @Override
  public Object[] filter(ConsumerBuildComp entity) {
    return null;
  }
}
