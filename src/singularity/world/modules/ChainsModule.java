package singularity.world.modules;

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.Writes;
import mindustry.world.modules.BlockModule;
import singularity.world.blockComp.ChainsBuildComp;
import singularity.world.blocks.chains.ChainContainer;
import singularity.world.blocks.chains.ChainsEvents;

public class ChainsModule extends BlockModule{
  public ChainsBuildComp entity;
  public ChainContainer container;
  
  protected ObjectMap<Class<? extends ChainsEvents.ChainsEvent>, Seq<Cons<ChainsEvents.ChainsEvent>>> listener = new ObjectMap<>();
  
  public ChainsModule(ChainsBuildComp entity){
    this.entity = entity;
  }
  
  public ChainContainer newContainer(){
    ChainContainer result = new ChainContainer(entity);
    result.listeners = listener;
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
  
  @SuppressWarnings("unchecked")
  public <T extends ChainsEvents.ChainsEvent> void listen(Class<T> event, Cons<T> cons){
    Seq<Cons<ChainsEvents.ChainsEvent>> list = listener.get(event);
    if(list == null){
      list = new Seq<>();
      list.add((Cons<ChainsEvents.ChainsEvent>) cons);
      listener.put(event, list);
    }
  }
  
  public void putVar(Object obj){
    container.putVar(obj);
  }
  
  public <T> T getVar(Class<T> type){
    return container.getVar(type);
  }
  
  public void update(){
  
  }
  
  @Override
  public void write(Writes write){
  
  }
  
}
