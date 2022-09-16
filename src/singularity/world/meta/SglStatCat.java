package singularity.world.meta;

import arc.struct.Seq;
import mindustry.world.meta.StatCat;
import universecore.util.handler.FieldHandler;

public class SglStatCat{
  public static final StatCat
      nuclear = create("nuclear", 2),
      gas = create("gas", 5),
      heat = create("heat", 6),
      reaction = create("reaction");

  private static StatCat create(String name){
    return create(name, StatCat.all.size);
  }

  private static StatCat create(String name, int index){
    Seq<StatCat> all = StatCat.all;
    StatCat res = new StatCat(name);

    FieldHandler.setValueDefault(res, "id", index);
    all.insert(index, res);

    return res;
  }
}
