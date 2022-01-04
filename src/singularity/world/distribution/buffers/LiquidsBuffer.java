package singularity.world.distribution.buffers;

import arc.math.WindowedMean;
import arc.struct.Seq;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import singularity.world.distribution.DistributeNetwork;

public class LiquidsBuffer extends BaseBuffer<LiquidStack, LiquidsBuffer.LiquidPacket>{
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
  public void containerPut(DistributeNetwork network){
  
  }
  
  @Override
  public void containerRequire(DistributeNetwork network, Seq<LiquidStack> requires){
  
  }
  
  public static class LiquidPacket extends Packet<LiquidStack>{
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
    public int unit(){
      return 4;
    }
  
    @Override
    public int id(){
      return obj.liquid.id;
    }
  
    @Override
    public int occupation(){
      return (int)Math.ceil(obj.amount/8);
    }
  
    @Override
    public void merge(Packet<LiquidStack> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += obj.amount;
      }
    }
  
    @Override
    public void remove(Packet<LiquidStack> other){
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
