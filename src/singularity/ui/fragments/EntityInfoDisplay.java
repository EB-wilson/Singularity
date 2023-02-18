package singularity.ui.fragments;

import arc.Core;
import arc.func.Floatc2;
import arc.util.Tmp;
import mindustry.game.Team;
import mindustry.gen.Entityc;

public abstract class EntityInfoDisplay<T extends Entityc>{
  public static void trnsPos(float worldX, float worldY, Floatc2 posCons){
    Core.camera.project(Tmp.v1.set(worldX, worldY));
    posCons.get(Tmp.v1.x, Tmp.v1.y);
  }

  public abstract float draw(EntityInfoFrag.EntityEntry<T> entry, Team team, float maxWight, float dy, float alpha, float scl);

  public abstract void updateVar(EntityInfoFrag.EntityEntry<T> entry, float delta);

  public abstract float wight(float scl);
}
