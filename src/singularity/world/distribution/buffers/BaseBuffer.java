package singularity.world.distribution.buffers;

import arc.math.WindowedMean;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Interval;
import singularity.world.distribution.DistributeNetwork;

import java.util.LinkedList;

public abstract class BaseBuffer<C, T extends BaseBuffer.Packet<C>>{
  public int capacity;
  protected int used;
  
  protected Interval timer = new Interval(2);
  protected WindowedMean putMean = new WindowedMean(6), readMean = new WindowedMean(6);
  protected int putCaching, readCaching;
  protected LinkedList<Float> putRate = new LinkedList<>(), readRate = new LinkedList<>();
  
  protected IntMap<T> memory = new IntMap<>();
  
  public int space(){
    return capacity - used;
  }
  
  public abstract Number remainingCapacity();
  
  public void update(boolean calculateDetail){
    if(timer.get(0, 10)){
      putMean.add(putCaching);
      readMean.add(readCaching);
      
      putCaching = readCaching = 0;
      
      if(putMean.hasEnoughData()){
        putRate.addFirst(putMean.mean());
        if(putRate.size() > 10) putRate.removeLast();
      }
  
      if(readMean.hasEnoughData()){
        readRate.addFirst(readMean.mean());
        if(readRate.size() > 10) readRate.removeLast();
      }
      
      if(calculateDetail && timer.get(1, 600)){
        for(IntMap.Entry<T> entry : memory){
          entry.value.calculateDelta();
        }
      }
    }
  }
  
  public void put(T packet){
    int useByte = packet.occupation();
    used += useByte;
    putCaching += useByte;
    
    T target;
    if((target = memory.get(packet.id())) != null){
      target.merge(packet);
    }
    else memory.put(packet.id(), packet);
  }
  
  @SuppressWarnings("unchecked")
  public <Type extends T> Type get(int id){
    return (Type)memory.get(id);
  }
  
  public void remove(T packet){
    T p = memory.get(packet.id());
    if(p != null){
      p.remove(packet);
      used -= packet.occupation();
      readCaching += packet.occupation();
    }
  }
  
  public void remove(int id){
    T packet = memory.remove(id);
    used -= packet.occupation();
    readCaching += packet.occupation();
  }
  
  public abstract void containerPut(DistributeNetwork network);
  
  public abstract void containerRequire(DistributeNetwork network, Seq<C> requires);
  
  public static abstract class Packet<Type>{
    public Type obj;
    
    public abstract int unit();
    
    public abstract int id();
    
    public abstract int occupation();
    
    public abstract void merge(Packet<Type> other);
    
    public abstract void remove(Packet<Type> other);
    
    public abstract void calculateDelta();
    
    public abstract float delta();
  }
}
