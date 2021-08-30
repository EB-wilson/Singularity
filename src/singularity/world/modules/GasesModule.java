package singularity.world.modules;

import arc.func.Cons;
import arc.func.Cons2;
import arc.math.WindowedMean;
import arc.struct.Bits;
import arc.struct.IntSet;
import arc.struct.ObjectSet;
import arc.util.Interval;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.MappableContent;
import mindustry.entities.Puddles;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.modules.BlockModule;
import mindustry.world.modules.LiquidModule;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.type.SglContentType;
import singularity.world.blockComp.GasBuildComp;

import java.util.Arrays;

public class GasesModule extends BlockModule{
  private static final int windowSize = 3, updateInterval = 60;
  private static final int gasesLength = Vars.content.getBy(SglContentType.gas.value).size;
  private static final Interval flowTimer = new Interval(2);
  private static final float pollScl = 20f;
  
  protected final GasBuildComp entity;
  protected final Building tile;
  protected float[] itemComp = new float[Vars.content.items().size];
  protected float[] gases = new float[gasesLength];
  protected float total = 0f;
  
  protected float[] cacheFlow = new float[gasesLength];
  protected WindowedMean[] flowMeans = new WindowedMean[gasesLength];
  protected float[] flowRate = new float[gasesLength];
  protected IntSet flows = new IntSet();
  
  public GasesModule(GasBuildComp entity){
    this.entity = entity;
    tile = entity.getBuilding();
  }
  
  public void update(boolean showFlow){
    each(stack -> {
      float pressure = entity.pressure();
      Gas gas = stack.gas;
      if(gas.compressible()){
        if(gas.compLiquid()){
          Gas.CompressLiquid liquid = gas.getCompressLiquid();
          if(pressure > liquid.requirePressure){
            gases[gas.id] -= liquid.consumeGas;
      
            produce(liquid.liquid);
          }
        }
  
        if(gas.compItem()){
          Gas.CompressItem item = gas.getCompressItem();
    
          if(pressure > item.requirePressure){
            if(gas.multiComp() && tile.block.hasLiquids){
              if(tile.liquids.get(item.liquid) > item.consumeLiquid/item.compTime){
                tile.liquids.remove(item.liquid, item.consumeLiquid/item.compTime);
                itemComp[item.item.id] += item.consumeLiquid/item.compTime;
          
                if(itemComp[item.item.id] >= item.consumeLiquid){
                  produce(item.item);
                  itemComp[item.item.id] = 0;
                }
              }
            }
            else{
              remove(item.consumeGas/item.compTime);
              itemComp[item.item.id] += item.consumeLiquid/item.compTime;
        
              if(itemComp[item.item.id] >= item.consumeGas){
                produce(item.item);
                itemComp[item.item.id] = 0;
              }
            }
          }
        }
      }
    });
  
    if(showFlow){
      if(flowTimer.get(1, pollScl)){
        boolean inTime = flowTimer.get(updateInterval);
        
        for(int id=0; id<gases.length; id++){
          if(flowMeans[id] == null) flowMeans[id] = new WindowedMean(windowSize);
          flowMeans[id].add(cacheFlow[id]);
          if(cacheFlow[id] > 0) flows.add(id);
          cacheFlow[id] = 0;
  
          if(inTime){
            flowRate[id] = flowMeans[id].hasEnoughData()? flowMeans[id].mean()*3: -1;
          }
        }
      }
    }
    else{
      flowMeans = new WindowedMean[gasesLength];
      flows.clear();
    }
  }
  
  public void produce(MappableContent object){
    if(object instanceof Item){
      if(tile.block.hasItems && tile.items.get((Item)object) < tile.block.itemCapacity){
        tile.items.add((Item)object, 1);
      }
    }
    else if(object instanceof Liquid){
      if(tile.block.hasLiquids && tile.liquids.get((Liquid)object) < tile.block.liquidCapacity){
        tile.liquids.add((Liquid)object, 1);
      }
      else{
        Puddles.deposit(tile.tile, (Liquid)object, 1);
      }
    }
  }
  
  public float getFlowRate(Gas gas){
    return flowRate[gas.id];
  }
  
  public void eachFlow(Cons2<Gas, Float> cons){
    for(int i=0; i<gases.length; i++){
      if(flows.contains(i)) cons.get(Vars.content.getByID(SglContentType.gas.value, i), flowRate[i]);
    }
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
    cacheFlow[gas.id] += Math.max(amount, 0);
  }
  
  public void set(Gas gas, float amount){
    float delta = amount - gases[gas.id];
    add(gas, delta);
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
    return total / entity.getGasBlock().gasCapacity();
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
