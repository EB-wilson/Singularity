package singularity.world.meta;

import mindustry.world.meta.StatUnit;
import universecore.util.handler.EnumHandler;

public class SglStatUnit{
  private static final EnumHandler<StatUnit> handler = new EnumHandler<>(StatUnit.class);
  
  public static final StatUnit neutronFlux = handler.addEnumItemTail("neutronFlux"),
      neutronFluxSecond = handler.addEnumItemTail("neutronFluxSecond"),
      pascal = handler.addEnumItemTail("pascal"),
      heat = handler.addEnumItemTail("heat"),
      absTemperature = handler.addEnumItemTail("absTemperature"),
      kHeat = handler.addEnumItemTail("kHeat"),
      temperature = handler.addEnumItemTail("temperature"),
      heatCapacity = handler.addEnumItemTail("heatCapacity"),
      kPascal = handler.addEnumItemTail("kPascal"),
      MPascal = handler.addEnumItemTail("MPascal"),
      byteUnit = handler.addEnumItemTail("byte"),
      bytePreSecond = handler.addEnumItemTail("bytePreSec");
}
