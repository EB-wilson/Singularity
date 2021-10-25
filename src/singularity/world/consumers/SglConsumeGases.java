package singularity.world.consumers;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.ui.ReqImage;
import mindustry.world.meta.Stat;
import mindustry.world.meta.Stats;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.ui.tables.GasValue;
import singularity.world.blockComp.GasBuildComp;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeType;

public class SglConsumeGases extends BaseConsume<GasBuildComp>{
  public GasStack[] gases;
  
  public SglConsumeGases(GasStack[] stack){
    gases = stack;
  }
  
  @Override
  public UncConsumeType<SglConsumeGases> type(){
    return SglConsumeType.gas;
  }
  
  @Override
  public void consume(GasBuildComp entity){
    //无触发器，update消耗
  }
  
  @Override
  public void update(GasBuildComp entity){
    for(GasStack stack: gases){
      entity.gases().remove(stack.gas, stack.amount*entity.getBuilding().edelta());
    }
  }
  
  @Override
  public void display(Stats stats){
    stats.add(Stat.input, table -> {
      table.row();
      table.table(t -> {
        t.defaults().left().fill().padLeft(6);
        t.add(Core.bundle.get("misc.gas") + ":").left();
        for(GasStack stack: gases){
          t.add(new GasValue(stack.gas, stack.amount*60));
        }
      }).left().padLeft(5);
    });
  }
  
  @Override
  public void build(GasBuildComp entity, Table table){
    for(GasStack stack : gases){
      table.add(new ReqImage(stack.gas.uiIcon,
        () -> entity.gases() != null && entity.gases().get(stack.gas) > stack.amount*entity.getBuilding().edelta() + 0.0001f)).padRight(8);
    }
    table.row();
  }
  
  @Override
  public boolean valid(GasBuildComp entity){
  
    for(GasStack stack: gases){
      if(entity.gases() == null || entity.gases().get(stack.gas) < stack.amount*(entity.getBlock().hasPower && entity.getBuilding().power.status != 0?
        entity.getBuilding().edelta(): entity.getBuilding().delta())) return false;
    }
    return true;
  }
  
  @Override
  public Object[] filter(GasBuildComp entity){
    int i = 0;
    Gas[] acceptGases = new Gas[gases.length];
    for(GasStack stack: gases){
      acceptGases[i++] = stack.gas;
    }
    return acceptGases;
  }
}
