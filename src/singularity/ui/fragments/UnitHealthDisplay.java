package singularity.ui.fragments;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import mindustry.game.Team;
import mindustry.gen.Unit;
import singularity.Sgl;

public class UnitHealthDisplay<T extends Unit> extends EntityInfoDisplay<T>{
  float tmp;

  @Override
  public float draw(EntityInfoFrag.EntityEntry<T> entry, Team team, float maxWight, float dy, float alpha, float scl) {
    T entity = entry.entity;

    Draw.alpha(alpha);

    trnsPos(entity.x, entity.y, (x, y) -> {
      tmp = Sgl.config.healthBarStyle.draw(
          x, y + dy,
          entry,
          team,
          dy,
          alpha,
          scl
      );

      Draw.reset();
    });

    return tmp;
  }

  @Override
  public void updateVar(EntityInfoFrag.EntityEntry<T> entry, float delta) {
    entry.handleVar("over", f -> Mathf.lerp(f, entry.entity.health, delta*0.04f), entry.entity.health);
  }

  @Override
  public float wight(float scl) {
    return Sgl.config.healthBarStyle.getWidth(scl);
  }
}
