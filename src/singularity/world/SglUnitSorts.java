package singularity.world;

import mindustry.entities.Units;

public class SglUnitSorts {
  private static float temp;

  public static Units.Sortf denser = (e, x, y) -> {
    temp = e.maxHealth;
    e.team.data().unitTree.intersect(e.x - 64, e.y - 64, 128, 128, u -> {
      temp += u.maxHealth;
    });

    return -temp;
  };
}
