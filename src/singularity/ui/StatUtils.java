package singularity.ui;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.BulletType;
import mindustry.world.meta.StatUnit;
import singularity.world.blocks.turrets.EmpBulletType;

import static mindustry.Vars.tilesize;

public class StatUtils{
  public static void buildAmmo(Table table, BulletType bullet){
    table.left().defaults().padRight(3).left();

    if(bullet.damage > 0 && (bullet.collides || bullet.splashDamage <= 0)){
      if(bullet.continuousDamage() > 0){
        table.add(Core.bundle.format("bullet.damage", bullet.continuousDamage()) + StatUnit.perSecond.localized());
      }else{
        table.add(Core.bundle.format("bullet.damage", bullet.damage));
      }
    }

    if (bullet instanceof EmpBulletType emp){
      sep(table, Core.bundle.format("bullet.empDamage", emp.empDamage, emp.empRange > 0? "[lightgray]~ [accent]" + emp.empRange/tilesize + "[lightgray]" + StatUnit.blocks.localized() : ""));
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

    if(bullet.status != StatusEffects.none){
      sep(table, (bullet.status.minfo.mod == null? bullet.status.emoji(): "") + "[stat]" + bullet.status.localizedName + "[lightgray] ~ " +
          "[stat]" + Strings.autoFixed(bullet.statusDuration/60f, 1) + "[lightgray] " + Core.bundle.get("unit.seconds"));
    }

    if(bullet.fragBullet != null){
      sep(table, Core.bundle.format("bullet.frags", bullet.fragBullets));
      table.row();
      table.table(st -> buildAmmo(st, bullet.fragBullet)).left().padLeft(15);
    }

    table.row();
  }

  private static void sep(Table table, String text){
    table.row();
    table.add(text);
  }
}
