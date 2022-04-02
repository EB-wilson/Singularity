package singularity.world.meta;

import mindustry.world.meta.StatUnit;
import universecore.util.handler.EnumHandler;
import universecore.util.handler.FieldHandler;

public class SglStatUnit{
  private static final EnumHandler<StatUnit> handler = new EnumHandler<>(StatUnit.class, (inst, param) -> {
    if(param.length == 0){
      FieldHandler.setValue(StatUnit.class, "space", inst, true);
    }
    else{
      FieldHandler.setValue(StatUnit.class, "space", inst, param[0]);
    }
  });
  
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
