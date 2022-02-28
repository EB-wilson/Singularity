package singularity.world.distribution.buffers;

import arc.math.WindowedMean;
import arc.struct.IntMap;
import arc.util.Interval;
import mindustry.world.modules.BlockModule;
import org.jetbrains.annotations.NotNull;
import singularity.world.distribution.DistributeNetwork;

import java.util.Iterator;
import java.util.LinkedList;

public abstract class BaseBuffer<C, CType, T extends BaseBuffer.Packet<C, CType>> implements Iterable<T>{
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
  
  public Number remainingCapacity(){
    return space()/unit();
  }
  
  public Number usedCapacity(){
    return used/unit();
  }
  
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
  
  public void set(T packet){
    int put;
    Packet existed = memory.get(packet.id());
    if(existed == null){
      put = packet.occupation();
    }
    else{
      put = packet.occupation() - existed.occupation();
    }
    
    putCaching += put;
    used += put;
    memory.put(packet.id(), packet);
  }
  
  @SuppressWarnings("unchecked")
  public <Type extends T> Type get(int id){
    return (Type)memory.get(id);
  }
  
  public void remove(T packet){
    T p = memory.get(packet.id());
    if(p != null){
      if(p.occupation() > packet.occupation()){
        p.remove(packet);
        used -= packet.occupation();
        readCaching += packet.occupation();
      }
      else{
        remove(packet.id());
      }
    }
  }
  
  public void remove(int id){
    T packet = memory.remove(id);
    if(packet != null){
      used -= packet.occupation();
      readCaching += packet.occupation();
    }
  }
  
  public abstract void bufferContAssign(DistributeNetwork network);
  
  public abstract int unit();
  
  public abstract BlockModule generateBindModule();
  
  @NotNull
  @Override
  public Iterator<T> iterator(){
    return memory.values().iterator();
  }
  
  public static abstract class Packet<Obj, Type>{
    public Obj obj;
    
    public abstract int id();
    
    public abstract Type get();
    
    public abstract int occupation();
    
    public abstract Number amount();
    
    public abstract void merge(Packet<Obj, Type> other);
    
    public abstract void remove(Packet<Obj, Type> other);
    
    public abstract void calculateDelta();
    
    public abstract float delta();
  }
}
