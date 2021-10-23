package singularity.world.meta;

import mindustry.world.meta.StatUnit;
import universeCore.util.handler.EnumHandler;
import universeCore.util.handler.FieldHandler;

public class SglStatUnit{
  private static final EnumHandler<StatUnit> handler = new EnumHandler<>(StatUnit.class){
    @Override
    public void constructor(StatUnit instance, Object... param){
      if(param.length == 0){
        FieldHandler.setValue(StatUnit.class, "space", instance, true);
      }
      else{
        FieldHandler.setValue(StatUnit.class, "space", instance, param[0]);
      }
    }
  };
  
  public static final StatUnit neutronFlux = handler.addEnumItemTail("neutronFlux"),
      neutronFlow = handler.addEnumItemTail("neutronFlow"),
      pascal = handler.addEnumItemTail("pascal"),
      kPascal = handler.addEnumItemTail("kPascal"),
      MPascal = handler.addEnumItemTail("MPascal");
}
