package singularity.ui;

import arc.Core;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import arc.util.Strings;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;
import singularity.contents.OtherContents;
import singularity.world.blocks.turrets.EmpBulletType;
import singularity.world.blocks.turrets.HeatBulletType;

import static mindustry.Vars.tilesize;

public class StatUtils{
  public static void buildAmmo(Table table, BulletType bullet){
    table.left().defaults().padRight(3).left();

    if(bullet.damage > 0){
      if(bullet.continuousDamage() > 0){
        sep(table, Core.bundle.format("bullet.damage", bullet.continuousDamage()) + StatUnit.perSecond.localized());
      }else{
        sep(table, Core.bundle.format("bullet.damage", bullet.damage));
      }
    }

    if (bullet instanceof EmpBulletType emp){
      sep(table, Core.bundle.format("bullet.empDamage", emp.empDamage, emp.empRange > 0? "[lightgray]~ [accent]" + emp.empRange/tilesize + "[lightgray]" + StatUnit.blocks.localized() : ""));
    }

    if (bullet instanceof HeatBulletType heat){
      table.row();
      table.table(t -> {
        t.left().defaults().padRight(3).left();
        t.image(OtherContents.meltdown.uiIcon).size(25).scaling(Scaling.fit);
        t.add(Core.bundle.format("infos.heatAmmo", Strings.autoFixed(heat.meltDownTime/60, 1), Strings.autoFixed(heat.melDamageScl*60, 1), heat.maxExDamage > 0? heat.maxExDamage: Math.max(heat.damage, heat.splashDamage)));
      });
    }

    if(bullet.buildingDamageMultiplier != 1){
      sep(table, Core.bundle.format("bullet.buildingdamage", (int) (bullet.buildingDamageMultiplier*100)));
    }

    if(bullet.rangeChange != 0){
      sep(table, Core.bundle.format("bullet.range", (bullet.rangeChange > 0? "+": "-") + Strings.autoFixed(bullet.rangeChange/tilesize, 1)));
    }

    if(bullet.splashDamage > 0){
      sep(table, Core.bundle.format("bullet.splashdamage", (int) bullet.splashDamage, Strings.fixed(bullet.splashDamageRadius/tilesize, 1)));
    }

    if(bullet.knockback > 0){
      sep(table, Core.bundle.format("bullet.knockback", Strings.autoFixed(bullet.knockback, 2)));
    }

    if(bullet.healPercent > 0f){
      sep(table, Core.bundle.format("bullet.healpercent", Strings.autoFixed(bullet.healPercent, 2)));
    }

    if(bullet.healAmount > 0f){
      sep(table, Core.bundle.format("bullet.healamount", Strings.autoFixed(bullet.healAmount, 2)));
    }

    if(bullet.pierce || bullet.pierceCap != -1){
      sep(table, bullet.pierceCap == -1? "@bullet.infinitepierce": Core.bundle.format("bullet.pierce", bullet.pierceCap));
    }

    if(bullet.incendAmount > 0){
      sep(table, "@bullet.incendiary");
    }

    if(bullet.homingPower > 0.01f){
      sep(table, "@bullet.homing");
    }

    if(bullet.lightning > 0){
      sep(table, Core.bundle.format("bullet.lightning", bullet.lightning, bullet.lightningDamage < 0? bullet.damage: bullet.lightningDamage));
    }

    if(bullet.pierceArmor){
      sep(table, "@bullet.armorpierce");
    }

    if(bullet.status != StatusEffects.none && bullet.status != null){
      sep(table, (bullet.status.minfo.mod == null? bullet.status.emoji(): "") + "[stat]" + bullet.status.localizedName + "[lightgray] ~ " +
          "[stat]" + Strings.autoFixed(bullet.statusDuration/60f, 1) + "[lightgray] " + Core.bundle.get("unit.seconds"));
    }

    if(bullet.intervalBullet != null){
      table.row();

      Table ic = new Table();
      buildAmmo(ic, bullet.intervalBullet);
      Collapser coll = new Collapser(ic, true);
      coll.setDuration(0.1f);

      table.table(it -> {
        it.left().defaults().left();

        it.add(Core.bundle.format("bullet.interval", Strings.autoFixed(bullet.intervalBullets / bullet.bulletInterval * 60, 2)));
        it.button(Icon.downOpen, Styles.emptyi, () -> coll.toggle(false)).update(i -> i.getStyle().imageUp = (!coll.isCollapsed() ? Icon.upOpen : Icon.downOpen)).size(8).padLeft(16f).expandX();
      });
      table.row();
      table.add(coll).padLeft(16);
    }

    if(bullet.fragBullet != null){
      table.row();

      Table ic = new Table();
      buildAmmo(ic, bullet.fragBullet);
      Collapser coll = new Collapser(ic, true);
      coll.setDuration(0.1f);

      table.table(ft -> {
        ft.left().defaults().left();

        ft.add(Core.bundle.format("bullet.frags", bullet.fragBullets));
        ft.button(Icon.downOpen, Styles.emptyi, () -> coll.toggle(false)).update(i -> i.getStyle().imageUp = (!coll.isCollapsed() ? Icon.upOpen : Icon.downOpen)).size(8).padLeft(16f).expandX();
      });
      table.row();
      table.add(coll).padLeft(16);
    }

    table.row();
  }

  private static void sep(Table table, String text){
    table.row();
    table.add(text);
  }
}
