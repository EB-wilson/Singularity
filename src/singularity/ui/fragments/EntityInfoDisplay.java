package singularity.ui.fragments;

import arc.Core;
import arc.func.Floatc2;
import arc.util.Tmp;
import mindustry.game.Team;
import mindustry.gen.Entityc;
import mindustry.gen.Posc;

public abstract class EntityInfoDisplay<T extends Entityc & Posc>{
  public abstract float draw(EntityInfoFrag.EntityEntry<T> entry, Team team, float maxWight, float dy, float alpha, float scl);

  public abstract void updateVar(EntityInfoFrag.EntityEntry<T> entry, float delta);

  public abstract float wight(float scl);
}
