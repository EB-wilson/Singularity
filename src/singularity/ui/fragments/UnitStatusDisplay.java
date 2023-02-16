package singularity.ui.fragments;

import mindustry.game.Team;
import mindustry.gen.Unit;
import singularity.Sgl;

public class UnitStatusDisplay<T extends Unit> extends EntityInfoDisplay<T>{
  @Override
  public float draw(EntityInfoFrag.EntityEntry<T> entry, Team team, float maxWight, float dy, float alpha) {
    float size = Sgl.config.statusSize;
    entry.entity.statusBits();

    return 0;
  }

  @Override
  public void updateVar(EntityInfoFrag.EntityEntry<T> entry, float delta) {

  }

  @Override
  public float wight() {
    return 0;
  }
}
