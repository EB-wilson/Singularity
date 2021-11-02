package singularity.world.meta;

import mindustry.world.meta.StatCat;
import universeCore.util.handler.EnumHandler;

public class SglStatCat{
  private static final EnumHandler<StatCat> handler = new EnumHandler<>(StatCat.class);
  
  public static final StatCat
      nuclear = handler.addEnumItem("nuclear", 2),
      gas = handler.addEnumItem("gas", 5),
      heat = handler.addEnumItem("heat", 6),
      reaction = handler.addEnumItemTail("reaction"),
      generator = handler.addEnumItemTail("generator");
}
