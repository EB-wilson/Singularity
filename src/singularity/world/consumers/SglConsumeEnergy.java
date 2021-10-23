package singularity.world.consumers;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.world.meta.Stat;
import mindustry.world.meta.Stats;
import singularity.world.blockComp.NuclearEnergyBuildComp;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeType;

public class SglConsumeEnergy extends BaseConsume<NuclearEnergyBuildComp>{
  public boolean buffer = false;
  public final float usage;

  public SglConsumeEnergy(float usage){
    this.usage = usage;
  }
  
  public void buffer(){
    this.buffer = true;
  }
  
  @Override
  public UncConsumeType<SglConsumeEnergy> type(){
    return SglConsumeType.energy;
  }
  
  @Override
  public void consume(NuclearEnergyBuildComp entity){
    if(buffer) entity.handleEnergy(-usage*60);
  }

  @Override
  public void update(NuclearEnergyBuildComp entity) {
    if(!buffer){
      entity.handleEnergy(-usage*entity.getBuilding().edelta());
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
  public void build(NuclearEnergyBuildComp entity, Table table) {
    table.row();
  }

  @Override
  public boolean valid(NuclearEnergyBuildComp entity){
    if(buffer){
      return entity.energy().getEnergy() >= usage*60*entity.getBuilding().edelta();
    }
    return entity.energy().getEnergy() >= usage;
  }
  
  @Override
  public Object[] filter(NuclearEnergyBuildComp entity) {
    return null;
  }
}
