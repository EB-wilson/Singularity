package singularity.world.products;

import arc.Core;
import mindustry.world.meta.Stat;
import mindustry.world.meta.Stats;
import singularity.type.GasStack;
import singularity.ui.tables.GasValue;
import singularity.world.blockComp.GasBuildComp;
import universeCore.world.producers.BaseProduce;
import universeCore.world.producers.ProduceType;

public class ProduceGases extends BaseProduce<GasBuildComp>{
  public GasStack[] gases;
  
  public ProduceGases(GasStack[] stacks){
    this.gases = stacks;
  }
  
  @Override
  public ProduceType<ProduceGases> type(){
    return SglProduceType.gas;
  }
  
  @Override
  public void produce(GasBuildComp entity){
    //无触发器，update进行更新
  }
  
  @Override
  public void update(GasBuildComp entity){
    for(GasStack stack: gases){
      entity.gases().add(stack.gas, stack.amount*entity.getBuilding().edelta());
    }
  }
  
  @Override
  public void display(Stats stats){
    stats.add(Stat.output, table -> {
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
  public boolean valid(GasBuildComp entity){
    float amount = 0;
    for(GasStack stack: gases){
      amount += stack.amount;
    }
    return entity.pressure() + amount/entity.getGasBlock().gasCapacity() < entity.getGasBlock().maxGasPressure();
  }
  
  @Override
  public void dump(GasBuildComp entity){
    if(entity.getGasBlock().classicDumpGas()){
      for(GasStack stack: gases){
        entity.dumpGas(stack.gas);
      }
    }
    else entity.dumpGas();
  }
}
