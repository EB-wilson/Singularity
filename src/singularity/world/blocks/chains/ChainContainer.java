package singularity.world.blocks.chains;

import arc.Core;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import singularity.world.blockComp.ChainsBuildComp;

public class ChainContainer{
  public ObjectMap<Class<? extends ChainsEvents.ChainsEvent>, Seq<Cons<ChainsEvents.ChainsEvent>>> listeners = new ObjectMap<>();
  
  private static final Queue<ChainsBuildComp> findQueue = new Queue<>();
  private static final ObjectSet<ChainsBuildComp> added = new ObjectSet<>();
  
  public ObjectMap<String, Object> localVars = new ObjectMap<>();
  
  public final ObjectSet<ChainsBuildComp> all = new ObjectSet<>();
  
  private long lastFrameUpdated;
  
  public ChainContainer(ChainsBuildComp entity){
    all.add(entity);
    entity.chains().container = this;
  }
  
  public void putVar(String key, Object obj){
    localVars.put(key, obj);
  }
  
  @SuppressWarnings("unchecked")
  public <T> T getVar(String key){
    return (T)localVars.get(key);
  }
  
  public void add(ChainContainer other){
    for(ChainsBuildComp next : other.all){
      add(next);
    }
  }
  
  public void handle(ChainsEvents.ChainsEvent event){
    for(Cons<ChainsEvents.ChainsEvent> listeners: listeners.get(event.getClass(), new Seq<>())){
      listeners.get(event);
    }
  }
  
  @SuppressWarnings("unchecked")
  public <T extends ChainsEvents.ChainsEvent> void listen(Class<T> event, Cons<T> cons){
    Seq<Cons<ChainsEvents.ChainsEvent>> list = listeners.get(event);
    if(list == null){
      list = new Seq<>();
      list.add((Cons<ChainsEvents.ChainsEvent>) cons);
      listeners.put(event, list);
    }
  }
  
  public void add(ChainsBuildComp other){
    if(!all.add(other)) return;
    ChainContainer oldContainer = other.chains().container;
    other.chains().container = this;
    other.chains().handle(new ChainsEvents.AddedBlockEvent(other, this, oldContainer));
  }
  
  public void update(){
    if(Core.graphics.getFrameId() == lastFrameUpdated) return;
    lastFrameUpdated = Core.graphics.getFrameId();
    
    handle(new ChainsEvents.UpdateEvent());
  }
  
  public void reconstruct(ChainsBuildComp source, Seq<ChainsBuildComp> excludes){
    findQueue.clear();
    added.clear();
    
    findQueue.addFirst(source);
    added.add(source);
    all.clear();
    while(!findQueue.isEmpty()){
      ChainsBuildComp other = findQueue.removeLast();
      for(ChainsBuildComp next : other.chainBuilds()){
        if(added.add(next) && !excludes.contains(next)) findQueue.addFirst(next);
      }
      all.add(other);
      ChainContainer oldContainer = other.chains().container;
      other.chains().container = this;
      other.chains().handle(new ChainsEvents.ConstructFlowEvent(other, this, oldContainer));
    }
  }
  
  public void remove(ChainsBuildComp target){
    for(ChainsBuildComp other : target.chainBuilds()){
      if(other.chains().container != this) continue;
      
      other.chains().newContainer();
      other.chains().container.reconstruct(other, Seq.with(target));
    }
    target.chains().handle(new ChainsEvents.RemovedBlockEvent(target));
  }
}
