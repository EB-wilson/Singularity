package singularity.world.modules;

import arc.func.Cons;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Puddles;
import mindustry.world.modules.BlockModule;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.type.SglContentType;
import singularity.world.blockComp.GasBuildComp;

public class GasesModule extends BlockModule{
  protected final GasBuildComp entity;
  protected float[] gases = new float[Vars.content.getBy(SglContentType.gas.value).size];
  protected float total = 0f;
  
  public GasesModule(GasBuildComp entity){
    this.entity = entity;
  }
  
  public void update(){
    each(stack -> {
      float pressure = getPressure();
      Gas gas = stack.gas;
      if(gas.compressible && pressure >= gas.criticalPressure){
        gases[gas.id] -= gas.compressRequire;
        if(gas.compItem()){
          if(entity.getBuilding().block.hasItems && entity.getBuilding().items.get(gas.compressItem) < entity.getBuilding().block.itemCapacity){
            entity.getBuilding().handleItem(entity.getBuilding(), gas.compressItem);
          }
        }
        else{
          if(entity.getBuilding().block.hasLiquids && entity.getBuilding().liquids.get(gas.compressLiquid) < entity.getBuilding().block.liquidCapacity){
            entity.getBuilding().handleLiquid(entity.getBuilding(), gas.compressLiquid, 1);
          }
          else{
            Puddles.deposit(entity.getBuilding().tile, gas.compressLiquid, 1);
          }
        }
      }
    });
  }
  
  public void add(GasesModule module){
    module.each(this::add);
  }
  
  public void add(Object... args){
    add(GasStack.with(args));
  }
  
  public void add(GasStack[] stacks){
    for(GasStack stack: stacks){
      add(stack);
    }
  }
  
  public void add(GasStack stack){
    add(stack.gas, stack.amount);
  }
  
  public final void add(Gas gas, float amount){
    gases[gas.id] += amount;
    total += amount;
  }
  
  public void remove(GasesModule module){
    module.each(this::remove);
  }
  
  public void remove(Object... args){
    remove(GasStack.with(args));
  }
  
  public void remove(GasStack[] stacks){
    for(GasStack stack: stacks){
      remove(stack);
    }
  }
  
  public void remove(GasStack stack){
    remove(stack.gas, stack.amount);
  }
  
  public void remove(Gas gas, float amount){
    add(gas, -amount);
  }
  
  public float get(Gas gas){
    return gases[gas.id];
  }
  
  public void clear(){
    gases = new float[Vars.content.getBy(SglContentType.gas.value).size];
  }
  
  public float total(){
    return total;
  }
  
  public float getPressure(){
    return (total / entity.getGasBlock().gasCapacity());
  }
  
  public void each(Cons<GasStack> cons){
    for(int id = 0; id<gases.length; id++){
      if(gases[id] > 0.001) cons.get(new GasStack(Vars.content.getByID(SglContentType.gas.value, id), gases[id]));
    }
  }
  
  @Override
  public void read(Reads read){
    super.read(read);
    int count = read.i();
    
    for(int id=0; id<count; id++){
      float amount = read.f();
      gases[id] = amount;
      total += amount;
    }
  }
  
  @Override
  public void write(Writes write){
    write.i(gases.length);
  
    for(float gas : gases){
      write.f(gas);
    }
  }
}
