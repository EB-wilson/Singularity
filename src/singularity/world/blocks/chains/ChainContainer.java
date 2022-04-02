package singularity.world.blocks.chains;

import arc.Core;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import singularity.world.components.ChainsBuildComp;

import java.util.LinkedHashMap;

public class ChainContainer{
  public ObjectMap<Class<? extends ChainsEvents.ChainsEvent>, LinkedHashMap<String, Cons<ChainsEvents.ChainsEvent>>> globalListener = new ObjectMap<>();
  
  private static final Queue<ChainsBuildComp> findQueue = new Queue<>();
  private static final ObjectSet<ChainsBuildComp> added = new ObjectSet<>();
  
  public ObjectMap<String, Object> localVars = new ObjectMap<>();
  
  public final ObjectSet<ChainsBuildComp> all = new ObjectSet<>();

  private int minX, minY;
  private int maxX, maxY;
  
  private long lastFrameUpdated;

  public ChainContainer(ChainsBuildComp entity){
    all.add(entity);
    minX = maxX = entity.tileX();
    minY = maxY = entity.tileY();
    entity.chains().container = this;
  }

  public boolean inlerp(int x, int y){
    return x <= maxX && x >= minX && y <= maxY && y >= minY;
  }

  public int minX(){
    return minX;
  }

  public int maxX(){
    return maxX;
  }

  public int minY(){
    return minY;
  }

  public int maxY(){
    return maxY;
  }

  public int width(){
    return maxX - minX + 1;
  }

  public int height(){
    return maxY - minY + 1;
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
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void handle(ChainsEvents.ChainsEvent event){
    for(Cons listeners: globalListener.get(event.getClass(), new LinkedHashMap<>()).values()){
      listeners.get(event);
    }
  }
  
  public void handle(ChainsEvents.ChainsTrigger event){
    for(ChainsBuildComp comp : all){
      comp.chains().handle(event);
    }
  }

  private void updateEdge(ChainsBuildComp other){
    float offset = (other.getBlock().size + other.getBlock().offset)/2;
    minX = Math.min(minX, (int)(other.tileX() - offset));
    minY = Math.min(minY, (int)(other.tileY() - offset));
    maxX = Math.max(maxX, (int)(other.tileX() + offset));
    maxY = Math.max(maxY, (int)(other.tileY() + offset));
  }
  
  public void add(ChainsBuildComp other){
    if(!all.add(other)) return;

    updateEdge(other);

    ChainContainer oldContainer = other.chains().container;
    other.chains().container = this;
    
    LinkedHashMap<String, Cons<ChainsEvents.ChainsEvent>> listeners;
    for(ObjectMap.Entry<Class<? extends ChainsEvents.ChainsEvent>, LinkedHashMap<String, Cons<ChainsEvents.ChainsEvent>>> entry : oldContainer.globalListener){
      listeners = globalListener.get(entry.key, LinkedHashMap::new);
      listeners.putAll(entry.value);
    }
    handle(new ChainsEvents.AddedBlockEvent(other, this, oldContainer));
  }
  
  public void update(){
    if(Core.graphics.getFrameId() == lastFrameUpdated) return;
    lastFrameUpdated = Core.graphics.getFrameId();
    
    handle(ChainsEvents.ChainsTrigger.update);
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
      updateEdge(other);
      ChainContainer oldContainer = other.chains().container;
      other.chains().container = this;
      handle(new ChainsEvents.ConstructFlowEvent(other, this, oldContainer));
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
