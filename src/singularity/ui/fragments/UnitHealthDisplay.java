package singularity.ui.fragments;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import mindustry.game.Team;
import mindustry.gen.Unit;
import singularity.Sgl;

public class UnitHealthDisplay<T extends Unit> extends EntityInfoDisplay<T>{
  public HealthBarStyle style = HealthBarStyle.defaultt;

  float tmp;

  @Override
  public float draw(EntityInfoFrag.EntityEntry<T> entry, Team team, float maxWight, float dy, float alpha) {
    T entity = entry.entity;

    Draw.alpha(alpha);

    trnsPos(entity.x, entity.y + dy, (x, y) -> {
      tmp = style.draw(
          x, y,
          entry,
          team,
          Sgl.config.healthBarWidth, Sgl.config.healthBarHeight,
          Sgl.config.empBarWidth, Sgl.config.empBarHeight,
          dy,
          alpha
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
  public float wight() {
    return Math.max(Sgl.config.healthBarWidth, Sgl.config.empBarWidth);
  }
}
