package singularity.world.consumers;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;
import arc.struct.Bits;
import mindustry.gen.Building;
import mindustry.ui.ReqImage;
import mindustry.world.meta.Stat;
import mindustry.world.meta.Stats;
import singularity.type.GasStack;
import singularity.type.SglContents;
import singularity.ui.tables.GasDisplay;
import singularity.world.components.GasBuildComp;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.UncConsumeType;

public class SglConsumeGases<T extends Building & GasBuildComp & ConsumerBuildComp> extends BaseConsume<T>{
  public GasStack[] gases;
  
  public SglConsumeGases(GasStack[] stack){
    gases = stack;
  }
  
  @Override
  public UncConsumeType<SglConsumeGases<?>> type(){
    return SglConsumeType.gas;
  }
  
  @Override
  public TextureRegion icon(){
    return gases[0].gas.uiIcon;
  }
  
  @Override
  public void consume(T entity){
    //无触发器，update消耗
  }
  
  @Override
  public void update(T entity){
    for(GasStack stack: gases){
      entity.gases().remove(stack.gas, stack.amount*entity.consDelta(parent)*entity.consumeMultiplier(this));
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
          t.add(new GasDisplay(stack.gas, stack.amount*60));
        }
      }).left().padLeft(5);
    });
  }
  
  @Override
  public void build(T entity, Table table){
    for(GasStack stack : gases){
      table.add(new ReqImage(stack.gas.uiIcon,
        () -> entity.gases() != null && entity.gases().get(stack.gas) > stack.amount*entity.consDelta(parent) + 0.0001f)).padRight(8);
    }
    table.row();
  }
  
  @Override
  public boolean valid(T entity){
    for(GasStack stack: gases){
      if(entity.gases() == null || entity.gases().get(stack.gas) < stack.amount*(entity.getBlock().hasPower && entity.getBuilding().power.status != 0?
          entity.delta()*entity.power.status: entity.getBuilding().delta())*entity.consumeMultiplier(this)) return false;
    }
    return true;
  }
  
  @Override
  public Bits filter(T entity){
    Bits result = new Bits(SglContents.gases().size);
    for(GasStack stack: gases){
      result.set(stack.gas.id);
    }
    return result;
  }
}
