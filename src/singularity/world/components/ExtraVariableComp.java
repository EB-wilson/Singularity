package singularity.world.components;

import universecore.annotations.Annotations;

import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public interface ExtraVariableComp{
  @Annotations.BindField(value = "extraVar", initialize = "new java.util.HashMap<>()")
  default Map<String, Object> extra(){
    return null;
  }

  default <T> T getVar(String field){
    return (T) extra().get(field);
  }

  default <T> T getVar(String field, T def){
    return (T) extra().getOrDefault(field, def);
  }

  default <T> T getVarThr(String field){
    return (T) extra().computeIfAbsent(field, e -> {
      throw new NoSuchFieldError("no such field with name: " + e);
    });
  }

  default <T> T removeVar(String field){
    return (T) extra().remove(field);
  }

  default <T> T setVar(String field, Object obj){
    return (T) extra().put(field, obj);
  }

  default <T> void handleVar(String field, Function<T, T> cons, T def){
    setVar(field, cons.apply(getVar(field, def)));
  }
}
