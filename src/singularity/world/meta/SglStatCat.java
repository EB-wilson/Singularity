package singularity.world.meta;

import mindustry.world.meta.StatCat;
import universeCore.util.handler.EnumHandler;

public class SglStatCat{
  private static final EnumHandler<StatCat> handler = new EnumHandler<>(StatCat.class);
  
  public static final StatCat gas = handler.addEnumItem("gas", 4),
      generator = handler.addEnumItem("generator", 7);
}
