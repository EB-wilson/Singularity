package singularity.world.components;

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import singularity.world.blocks.chains.ChainsEvents;
import universecore.annotations.Annotations;

import java.util.LinkedHashMap;

public interface ChainsBlockComp{
  @Annotations.BindField("maxChainsWidth")
  default int maxWidth(){
    return 0;
  }

  @Annotations.BindField("maxChainsHeight")
  default int maxHeight(){
    return 0;
  }

  @Annotations.BindField("chainsListeners")
  default ObjectMap<ChainsEvents.ChainsTrigger, Seq<Runnable>> listeners(){
    return null;
  }

  @Annotations.BindField("globalChainsListeners")
  default ObjectMap<Class<? extends ChainsEvents.ChainsEvent>, LinkedHashMap<String, Cons<ChainsEvents.ChainsEvent>>> globalListeners(){
    return null;
  }

  default boolean chainable(ChainsBlockComp other){
    return getClass().isAssignableFrom(other.getClass());
  }

  @SuppressWarnings("unchecked")
  default <T extends ChainsEvents.ChainsEvent> void listenGlobal(Class<T> eventType, Cons<T> listener, String name){
    globalListeners().get(eventType, LinkedHashMap::new).put(name, (Cons<ChainsEvents.ChainsEvent>) listener);
  }

  default void listen(ChainsEvents.ChainsTrigger trigger, Runnable listener){
    listeners().get(trigger, Seq::new).add(listener);
  }

  @Annotations.MethodEntry(entryMethod = "<init>", paramTypes = {"java.lang.String"})
  default void setListeners(){}
}
