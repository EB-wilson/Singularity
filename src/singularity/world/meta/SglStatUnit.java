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
      neutronFluxSecond = handler.addEnumItemTail("neutronFluxSecond"),
      pascal = handler.addEnumItemTail("pascal"),
      heat = handler.addEnumItemTail("heat"),
      absTemperature = handler.addEnumItemTail("absTemperature"),
      kHeat = handler.addEnumItemTail("kHeat"),
      temperature = handler.addEnumItemTail("temperature"),
      heatCapacity = handler.addEnumItemTail("heatCapacity"),
      kPascal = handler.addEnumItemTail("kPascal"),
      MPascal = handler.addEnumItemTail("MPascal");
}
