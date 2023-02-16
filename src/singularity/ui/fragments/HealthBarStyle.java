package singularity.ui.fragments;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.ScissorStack;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.scene.style.NinePatchDrawable;
import arc.util.Align;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.game.Team;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import singularity.Sgl;

public enum HealthBarStyle {
  defaultt{
    @Override
    public <T extends Unit> float draw(float x, float y, EntityInfoFrag.EntityEntry<T> entry, Team team, float healthWidth, float healthHeight, float empWidth, float empHeight, float dy, float alpha) {
      T entity = entry.entity;
      float present = Mathf.clamp(entity.health/entity.maxHealth);

      healthDrawable.draw(x - healthWidth/2, y + empHeight, healthWidth, healthHeight);
      float drawWidth = healthWidth*(entry.getVar("over", 0f)/entity.maxHealth);
      if(ScissorStack.push(scissor.set(x - healthWidth/2, y + empHeight, drawWidth, healthHeight))){
        Draw.color(Pal.lightishGray, alpha);
        healthInnerDrawable.draw(x - healthWidth/2, y + empHeight, healthWidth, healthHeight);
        ScissorStack.pop();
      }

      float realWidth = healthWidth*present;
      if(ScissorStack.push(scissor.set(x - healthWidth/2, y + empHeight, realWidth, healthHeight))){
        Draw.color(entity.team.color, alpha);
        healthInnerDrawable.draw(x - healthWidth/2, y + empHeight, healthWidth, healthHeight);
        ScissorStack.pop();
      }

      float shieldWidth = realWidth*Mathf.clamp(entity.shield/entity.maxHealth);
      if(ScissorStack.push(scissor.set(x - healthWidth/2, y + empHeight, shieldWidth, healthHeight))){
        Draw.color(Pal.accent, Mathf.absin(Time.globalTime, 10, 0.5f) + alpha*0.5f);
        healthInnerDrawable.draw(x - healthWidth/2, y + empHeight, healthWidth, healthHeight);
        ScissorStack.pop();
      }

      Draw.color(Color.white, alpha);
      empDrawable.draw(x + healthWidth/2 - empWidth, y, empWidth, empHeight);
      float empPresent = Sgl.empHealth.healthPresent(entity);
      float realEmpWidth = empWidth*(1 - empPresent);
      if(ScissorStack.push(scissor.set(x + healthWidth/2 - empWidth, y, realEmpWidth, empHeight))){
        Draw.color(Tmp.c1.set(Pal.remove).lerp(Color.white, empPresent + Mathf.absin(Time.globalTime%360, 1 + empPresent*4, 1 - empPresent)), alpha);
        empInnerDrawable.draw(x + healthWidth/2 - empWidth, y, empWidth, empHeight);
        ScissorStack.pop();
      }

      String str = entity.type.localizedName + " " + Mathf.round(Mathf.maxZero(entity.health)) + "/" + Mathf.round(entity.maxHealth);
      if (entity.shield > 0) str += " " + Iconc.defense + Mathf.round(entity.shield);
      Fonts.def.draw(str, x - healthWidth/2 + 6, y + empHeight + healthHeight/2 + 11*0.9f/2, Tmp.c1.set(Color.white).a(alpha), 0.9f*(healthHeight/22), false, Align.left);

      return healthHeight + empHeight;
    }
  };

  final Rect scissor = new Rect();

  public NinePatchDrawable healthDrawable, healthInnerDrawable;
  public NinePatchDrawable empDrawable, empInnerDrawable;

  public void load(){
    healthDrawable = Core.atlas.getDrawable(Sgl.modName + "-health_bar_" + name());
    healthInnerDrawable = Core.atlas.getDrawable(Sgl.modName + "-health_bar_" + name() + "_inner");
    empDrawable = Core.atlas.getDrawable(Sgl.modName + "-emp_bar_" + name());
    empInnerDrawable = Core.atlas.getDrawable(Sgl.modName + "-emp_bar_" + name() + "_inner");
  }

  abstract public <T extends Unit> float draw(float x, float y, EntityInfoFrag.EntityEntry<T> entity, Team team, float healthWight, float healthHeight, float empWidth, float empHeight, float dy, float alpha);
}
