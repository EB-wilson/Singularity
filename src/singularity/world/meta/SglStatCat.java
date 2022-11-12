package singularity.world.meta;

import arc.struct.Seq;
import mindustry.world.meta.StatCat;
import universecore.util.handler.FieldHandler;

public class SglStatCat{
  public static final StatCat
      neutron = create("neutron", 3),
      matrix = create("matrix", 5),
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
