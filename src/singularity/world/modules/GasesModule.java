package singularity.world.modules;

import arc.Events;
import arc.func.Cons2;
import arc.graphics.Color;
import arc.math.WindowedMean;
import arc.struct.IntSet;
import arc.util.Interval;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Puddles;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.modules.BlockModule;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.type.SglContents;
import singularity.world.atmosphere.Atmosphere;
import singularity.world.components.GasBuildComp;
import universecore.util.handler.FieldHandler;

import java.util.Arrays;

public class GasesModule extends BlockModule{
  static {
    Events.run(EventType.Trigger.update, () -> {
      Building nextFlowBuild = FieldHandler.getValueDefault(Vars.ui.hudfrag.blockfrag, "nextFlowBuild");

      if(nextFlowBuild instanceof GasBuildComp gasBuild){
        if(gasBuild.gases() != null) gasBuild.gases().updateFlow();
      }
    });
  }

  private static final Color tempColor = new Color();
  private static final int windowSize = 3, updateInterval = 60;
  private static final int gasesLength = SglContents.gases().size;
  private static final Interval flowTimer = new Interval(2);
  private static final float pollScl = 20f;
  
  protected final GasBuildComp entity;
  
  protected float[] itemComp = new float[Vars.content.items().size];
  protected float[] gases = new float[gasesLength];
  protected float total = 0f;
  
  protected float[] cacheFlow = new float[gasesLength];
  protected WindowedMean[] flowMeans = new WindowedMean[gasesLength];
  protected float[] flowRate = new float[gasesLength];
  protected IntSet flows = new IntSet();
  
  protected Color gasColor = new Color(), smoothColor = Color.white.cpy();
  
  public GasesModule(GasBuildComp entity, boolean initContains){
    this.entity = entity;
    
    if(initContains) distributeAtmo();
  }
  
  public GasesModule(GasBuildComp entity){
    this(entity, true);
  }

  public void updateFlow(){
    if(flowTimer.get(1, pollScl)){
      if(flowMeans == null) flowMeans = new WindowedMean[gasesLength];
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
  
  public void update(boolean compress){
    if(compress) each((gas, amount) -> {
      if(gas.compressible()){
        doCompress(gas);
      }
    });
  
    gasColor.set(Color.black);
    each((gas, amount) -> {
      gasColor.add(tempColor.set(gas.color).mul(amount/total()));
    });
    gasColor.a(getPressure()/entity.getGasBlock().maxGasPressure()*0.75f);
    
    smoothColor.lerp(gasColor, 0.15f*Time.delta);
  }
  
  public void doCompress(Gas gas){
    float delta = Time.delta;
    if(gas.compLiquid()){
      Gas.CompressLiquid liquid = gas.getCompressLiquid();
      if(getPressure() > liquid.requirePressure){
        gases[gas.id] -= liquid.consumeGas*delta;
  
        produce(liquid.liquid);
      }
    }
    
    if(gas.compItem()){
      Gas.CompressItem item = gas.getCompressItem();

      if(getPressure() > item.requirePressure){
        if(gas.multiComp() && entity.liquids() != null){
          if(entity.liquids().get(item.liquid) > item.consumeLiquid/item.compTime){
            entity.liquids().remove(item.liquid, item.consumeLiquid/item.compTime*delta);
            itemComp[item.item.id] += item.consumeLiquid/item.compTime*delta;
      
            if(itemComp[item.item.id] >= item.consumeLiquid){
              produce(item.item);
              itemComp[item.item.id] %= 1;
            }
          }
        }
        else{
          remove(gas, item.consumeGas/item.compTime*delta);
          itemComp[item.item.id] += item.consumeGas/item.compTime*delta;
    
          if(itemComp[item.item.id] >= item.consumeGas){
            produce(item.item);
            itemComp[item.item.id] %= 1;
          }
        }
      }
    }
  }
  
  public void produce(UnlockableContent object){
    float delta = Time.delta;
    if(object instanceof Item){
      if(entity.items() != null && entity.items().get((Item)object) < entity.getBlock().itemCapacity){
        entity.items().add((Item)object, 1);
      }
    }
    else if(object instanceof Liquid){
      if(entity.liquids() != null && entity.liquids().get((Liquid)object) < entity.getBlock().liquidCapacity){
        entity.liquids().add((Liquid)object, delta);
      }
      else{
        Puddles.deposit(entity.getBuilding().tile, (Liquid)object, delta);
      }
    }
  }
  
  public float getFlowRate(Gas gas){
    return flowRate[gas.id];
  }
  
  public void eachFlow(Cons2<Gas, Float> cons){
    for(int i=0; i<gases.length; i++){
      if(flows.contains(i)) cons.get(SglContents.gas(i), flowRate[i]);
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
  
  public void add(Gas gas, float amount){
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
    if(Vars.state.isCampaign()) each((gas, amount) -> Sgl.atmospheres.current.add(gas, amount));
    gases = new float[SglContents.gases().size];
    total = 0;
  }
  
  public float total(){
    return total;
  }
  
  public float getPressure(){
    return total/entity.getGasBlock().gasCapacity();
  }

  public void each(GasConsumer cons){
    for(int id = 0; id<gases.length; id++){
      if(gases[id] > 0.001) cons.get(SglContents.gas(id), gases[id]);
    }
  }
  
  public void distributeAtmo(){
    distributeAtmo(entity.getGasBlock().gasCapacity()*Sgl.atmospheres.current.getCurrPressure());
  }
  
  public void distributeAtmo(float total){
    Atmosphere curr = Sgl.atmospheres.current;
    
    curr.eachPresent((gas, rate) ->{
      add(gas, rate*total);
      if(Vars.state.isCampaign()) Sgl.atmospheres.current.remove(gas, rate*total);
    });
  }
  
  public Color color(){
    return smoothColor;
  }
  
  @Override
  public void read(Reads read){
    Arrays.fill(gases, 0);
    total = 0f;
    int count = read.s();
  
    for(int j = 0; j < count; j++){
      Gas gas = SglContents.gas(read.s());
      float amount = read.f();
      add(gas, amount);
    }
  }
  
  @Override
  public void write(Writes write){
    int amount = 0;
    for(float gas: gases){
      if(gas > 0) amount++;
    }
  
    write.s(amount);
  
    for(int i = 0; i < gases.length; i++){
      if(gases[i] > 0){
        write.s(i);
        write.f(gases[i]);
      }
    }
  }

  public interface GasConsumer{
    void get(Gas gas, float amount);
  }
}
