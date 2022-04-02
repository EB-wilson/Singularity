package singularity.world.modules;

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.Writes;
import mindustry.world.modules.BlockModule;
import singularity.world.components.ChainsBuildComp;
import singularity.world.blocks.chains.ChainContainer;
import singularity.world.blocks.chains.ChainsEvents;

import java.util.LinkedHashMap;

public class ChainsModule extends BlockModule{
  public ChainsBuildComp entity;
  public ChainContainer container;
  
  protected ObjectMap<ChainsEvents.ChainsTrigger, Seq<Runnable>> listeners = new ObjectMap<>();
  protected ObjectMap<Class<? extends ChainsEvents.ChainsEvent>, LinkedHashMap<String, Cons<ChainsEvents.ChainsEvent>>> globalListeners = new ObjectMap<>();
  
  public ChainsModule(ChainsBuildComp entity){
    this.entity = entity;
  }
  
  public ChainContainer newContainer(){
    ChainContainer result = new ChainContainer(entity);
    result.globalListener = new ObjectMap<>(globalListeners);
    handle(new ChainsEvents.InitChainContainerEvent(entity, result));
    
    return result;
  }
  
  public void each(Cons<ChainsBuildComp> cons){
    for(ChainsBuildComp other: container.all){
      cons.get(other);
    }
  }
  
  public void handle(ChainsEvents.ChainsEvent event){
    container.handle(event);
  }
  
  public void handle(ChainsEvents.ChainsTrigger trigger){
    for(Runnable listener : listeners.get(trigger, new Seq<>())){
      listener.run();
    }
  }

  public void setListeners(ObjectMap<ChainsEvents.ChainsTrigger, Seq<Runnable>> listeners){
    this.listeners = listeners;
  }

  public void setGlobalListeners(ObjectMap<Class<? extends ChainsEvents.ChainsEvent>, LinkedHashMap<String, Cons<ChainsEvents.ChainsEvent>>> listeners){
    this.globalListeners = listeners;
  }

  @SuppressWarnings("unchecked")
  public <T extends ChainsEvents.ChainsEvent> void listenGlobal(Class<T> event, String symbol, Cons<T> cons){
    globalListeners.get(event, LinkedHashMap::new).put(symbol, (Cons<ChainsEvents.ChainsEvent>) cons);
    if(container != null) container.globalListener.get(event, LinkedHashMap::new).put(symbol, (Cons<ChainsEvents.ChainsEvent>) cons);
  }
  
  public void listen(ChainsEvents.ChainsTrigger trigger, Runnable listener){
    listeners.get(trigger, Seq::new).add(listener);
  }
  
  public void putVar(String key, Object obj){
    container.putVar(key, obj);
  }
  
  public <T> T getVar(String key){
    return container.getVar(key);
  }
  
  public void update(){
  
  }
  
  @Override
  public void write(Writes write){
  
  }
  
}
