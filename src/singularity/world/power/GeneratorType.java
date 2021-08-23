package singularity.world.power;

import singularity.world.blocks.product.NormalCrafter.NormalCrafterBuild;
import arc.Core;

import java.lang.reflect.InvocationTargetException;

public enum GeneratorType{
  normalGenerator(NormalGenerator.class),
  nuclearImpact(NuclearImpact.class);
  
  final Class<?> type;
  
  GeneratorType(Class<?> type){
    this.type = type;
  }
  
  @SuppressWarnings("unchecked")
  public <T extends BaseGenerator> T applied(NormalCrafterBuild entity){
    try{
      return (T)type.getConstructor(NormalCrafterBuild.class).newInstance(entity);
    }catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e){
      return null;
    }
  }
  
  public String localized(){
    return Core.bundle.get("generator." + name() + ".localized");
  }
  
  public String describe(){
    return Core.bundle.get("generator." + name() + ".describe");
  }
}
