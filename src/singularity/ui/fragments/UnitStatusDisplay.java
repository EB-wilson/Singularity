package singularity.ui.fragments;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.GlyphLayout;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;
import mindustry.ui.Fonts;
import singularity.Sgl;

public class UnitStatusDisplay<T extends Unit> extends EntityInfoDisplay<T>{
  float tmp;

  @Override
  public float draw(EntityInfoFrag.EntityEntry<T> entry, Team team, float maxWight, float dy, float alpha, float scl) {
    T entity = entry.entity;
    float size = Sgl.config.statusSize;

    Draw.alpha(alpha);

    trnsPos(entity.x, entity.y, (x, y) -> {
      float offsetX = 0, offsetY = 0;
      float originX = x - maxWight/2f, originY = y + dy;
      tmp = 0;
      for (StatusEffect effect : Vars.content.statusEffects()) {
        if (!effect.show) continue;

        if (entry.entity.hasEffect(effect)) {
          Draw.rect(effect.uiIcon, originX + offsetX + size/2, originY + offsetY + size/2, size, size);

          if (Sgl.config.showStatusTime) {
            int sec = (int) (entry.entity.getDuration(effect) / 60);
            int min = sec / 60;
            int rem = sec - 60 * min;

            String str;
            if (min > 60) {
              str = "";
            } else {
              str = Integer.toString(min);
              if (str.length() == 1) {
                str = "0" + str;
              }
              str += ":";
              String s = Integer.toString(rem);
              if (s.length() == 1) {
                s = "0" + s;
              }
              str += s;
            }
            GlyphLayout layout = Fonts.outline.draw(str, originX + offsetX, originY + offsetY + size, Tmp.c1.set(Pal.accent).a(alpha), 0.7f, false, Align.topLeft);

            offsetX += Math.max(layout.width, size) + 1;
          }
          else offsetX += size + 1;

          if (offsetX >= maxWight){
            offsetX = 0;
            offsetY += size + 1;
            tmp = offsetY;
          }
        }
      }
    });

    return size + tmp;
  }

  @Override
  public void updateVar(EntityInfoFrag.EntityEntry<T> entry, float delta) {}

  @Override
  public float wight(float scl) {
    return 0;
  }
}
