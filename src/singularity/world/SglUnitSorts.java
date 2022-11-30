package singularity.world;

import mindustry.entities.Units;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

public class SglUnitSorts {
  public static Units.Sortf denser = (e, x, y) -> {
    float h = e.maxHealth;
    for (Unit unit : Groups.unit.intersect(e.x - 64, e.y - 64, 128, 128)) {
      if (unit.team == e.team) h += unit.maxHealth;
    }
    return h;
  };
}
