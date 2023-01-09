package singularity.type;

import arc.func.Func2;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.entities.Puddles;
import mindustry.gen.Puddle;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import universecore.util.Empties;

public class ReactLiquid extends Liquid {
  private final ObjectMap<Liquid, Seq<Func2<Puddle, Puddle, Float>>> reacts = new ObjectMap<>();

  public Runnable init = () -> {};

  public ReactLiquid(String name) {
    super(name);
  }

  public ReactLiquid(String name, Color color) {
    super(name, color);
  }

  public void reactWith(Liquid other, Func2<Puddle, Puddle, Float> reacting){
    reacts.get(other, Seq::new).add(reacting);
  }

  public void effectWith(Liquid other, Effect fx){
    effectWith(other, fx, 0.16f, color);
  }

  public void effectWith(Liquid other, Effect fx, float chance){
    effectWith(other, fx, chance, color);
  }

  public void effectWith(Liquid other, Effect fx, float chance, Color color){
    reactWith(other, (pa, pb) -> {
      if (Mathf.chanceDelta(chance*(pa.amount/50)*(pb.amount/50))) fx.at(pa.x + (pb.x - pa.x)/2, pa.y + (pb.y - pa.y)/2, color);
      return -0.1f;
    });
  }

  @Override
  public void init() {
    super.init();
    init.run();
  }

  @Override
  public void update(Puddle puddle) {
    Tile tile = puddle.tile;
    for (Point2 p : Geometry.d4) {
      Tile other = Vars.world.tile(tile.x + p.x, tile.y + p.y);
      if (other == null) continue;
      Puddle otherPuddle = Puddles.get(other);
      if (otherPuddle == null) continue;

      for(Func2<Puddle, Puddle, Float> cons: reacts.get(otherPuddle.liquid, Empties.nilSeq())) {
        puddle.amount += cons.get(puddle, otherPuddle);
      }
    }
  }
}
