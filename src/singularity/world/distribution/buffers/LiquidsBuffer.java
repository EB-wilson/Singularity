package singularity.world.distribution.buffers;

import arc.math.WindowedMean;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.world.modules.BlockModule;
import singularity.world.distribution.DistributeNetwork;

public class LiquidsBuffer extends BaseBuffer<LiquidStack, Liquid, LiquidsBuffer.LiquidPacket>{
  public void put(Liquid liquid, float amount){
    put(new LiquidPacket(liquid, amount));
  }
  
  public void remove(Liquid liquid, float amount){
    remove(new LiquidPacket(liquid, amount));
  }
  
  public void remove(Liquid liquid){
    remove(liquid.id);
  }
  
  public float get(Liquid liquid){
    LiquidPacket p = get(liquid.id);
    return p != null? p.obj.amount: 0;
  }
  
  @Override
  public Float remainingCapacity(){
    return space()/4f;
  }
  
  @Override
  public void bufferContAssign(DistributeNetwork network){
  
  }
  
  @Override
  public int unit(){
    return 4;
  }
  
  @Override
  public BlockModule generateBindModule(){
    return null;
  }
  
  public class LiquidPacket extends Packet<LiquidStack, Liquid>{
    WindowedMean putMean = new WindowedMean(6), readMean = new WindowedMean(6);
    float putCaching, readCaching;
    float putRate = -1, readRate= -1;
    
    public LiquidPacket(Liquid liquid, float amount){
      obj = new LiquidStack(liquid, amount);
      putCaching += amount;
    }
    
    public LiquidPacket(LiquidStack stack){
      obj = stack;
      putCaching += obj.amount;
    }
  
    @Override
    public int id(){
      return obj.liquid.id;
    }
  
    @Override
    public Liquid get(){
      return obj.liquid;
    }
  
    @Override
    public int occupation(){
      return (int)Math.ceil(obj.amount*unit());
    }
  
    @Override
    public Float amount(){
      return obj.amount;
    }
  
    @Override
    public void merge(Packet<LiquidStack, Liquid> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += obj.amount;
      }
    }
  
    @Override
    public void remove(Packet<LiquidStack, Liquid> other){
      if(other.id() == id()){
        obj.amount -= other.obj.amount;
        readCaching += obj.amount;
      }
    }
  
    @Override
    public void calculateDelta(){
      putMean.add(putCaching);
      putCaching = 0;
      if(putMean.hasEnoughData()) putRate = putMean.mean();
      
      readMean.add(readCaching);
      readCaching = 0;
      if(readMean.hasEnoughData()) readRate = readMean.mean();
    }
  
    @Override
    public float delta(){
      return 0;
    }
  }
}
