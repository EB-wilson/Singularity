package singularity.world.distribution.buffers;

import arc.math.WindowedMean;
import arc.struct.Seq;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.world.distribution.DistributeNetwork;

public class GasesBuffer extends BaseBuffer<GasStack, GasesBuffer.GasPacket>{
  public void put(Gas gas, float amount){
    put(new GasPacket(gas, amount));
  }
  
  public void remove(Gas gas, float amount){
    remove(new GasPacket(gas, amount));
  }
  
  public void remove(Gas gas){
    remove(gas.id);
  }
  
  public float get(Gas gas){
    GasPacket p = get(gas.id);
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
  public void containerRequire(DistributeNetwork network, Seq<GasStack> requires){
  
  }
  
  public static class GasPacket extends Packet<GasStack>{
    WindowedMean putMean = new WindowedMean(6), readMean = new WindowedMean(6);
    float putCaching, readCaching;
    float putRate = -1, readRate= -1;
    
    public GasPacket(Gas gas, float amount){
      obj = new GasStack(gas, amount);
      putCaching += amount;
    }
    
    public GasPacket(GasStack stack){
      obj = stack;
      putCaching += obj.amount;
    }
  
    @Override
    public int unit(){
      return 2;
    }
  
    @Override
    public int id(){
      return obj.gas.id;
    }
    
    @Override
    public int occupation(){
      return (int)Math.ceil(obj.amount/8);
    }
    
    @Override
    public void merge(Packet<GasStack> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += obj.amount;
      }
    }
    
    @Override
    public void remove(Packet<GasStack> other){
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
