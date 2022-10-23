package singularity.contents;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Items;
import mindustry.game.EventType;
import mindustry.gen.Bullet;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.type.AtomSchematic;
import singularity.world.meta.SglStat;
import universecore.UncCore;
import universecore.util.aspect.EntityAspect;
import universecore.util.aspect.triggers.TriggerEntry;

public class OtherContents implements ContentList{
  public static AtomSchematic copper_schematic,
  lead_schematic,
  silicon_schematic,
  titanium_schematic,
  thorium_schematic,
  uranium_schematic,
  iridium_schematic;

  public static StatusEffect electric_disturb,
  locking;

  static final ObjectMap<Unit, float[]> lastHealth = new ObjectMap<>();

  @Override
  public void load(){
    copper_schematic = new AtomSchematic(Items.copper, 14000){{
      request.medium(0.23f);
      request.time(30);
    }};

    lead_schematic = new AtomSchematic(Items.lead, 14000){{
      request.medium(0.26f);
      request.time(30);
    }};

    silicon_schematic = new AtomSchematic(Items.silicon, 18000){{
      request.medium(0.41f);
      request.item(Items.sand, 1);
      request.time(45);
    }};

    electric_disturb = new StatusEffect("electric_disturb"){
      {
        color = Pal.accent;
      }

      @Override
      public void setStats(){
        stats.addPercent(Stat.damageMultiplier, 0.8f);
        stats.addPercent(Stat.speedMultiplier, 0.6f);
        stats.addPercent(Stat.reloadMultiplier, 0.75f);
        stats.add(Stat.damage, 12f, StatUnit.perSecond);
        stats.add(SglStat.special, t -> {
          t.row();
          t.add(Core.bundle.format("data.bulletDeflectAngle", 12.4f + StatUnit.degrees.localized()));
          t.row();
          t.add("[lightgray]" + Core.bundle.get("infos,attenuationWithTime") + "[]").padLeft(15);
        });
      }

      @Override
      public void update(Unit unit, float time){
        super.update(unit, time);
        float scl = Mathf.clamp(time/120);
        unit.damageContinuousPierce(0.2f*Mathf.clamp(scl)*Time.delta);
        unit.speedMultiplier *= (0.6f + 0.4f*(1 - scl));
        unit.damageMultiplier *= (0.8f + 0.2f*(1 - scl));
        unit.reloadMultiplier *= (0.75f + 0.25f*(1 - scl));
      }
    };

    locking = new StatusEffect("locking"){
      {
        color = Pal.remove;
      }

      @Override
      public void draw(Unit unit){
        super.draw(unit);
        Draw.z(Layer.overlayUI);
        Draw.color(Pal.gray);
        Fill.square(unit.x, unit.y, 2);
        Draw.color(Pal.remove);
        Fill.square(unit.x, unit.y, 1);
        Drawf.square(unit.x, unit.y, unit.hitSize, Pal.remove);
        Tmp.v1.set(unit.hitSize + 4, 0);
        Tmp.v2.set(unit.hitSize + 12, 0);

        for(int i = 0; i < 4; i++){
          Drawf.line(Pal.remove,
              unit.x + Tmp.v1.x, unit.y + Tmp.v1.y,
              unit.x + Tmp.v2.x, unit.y + Tmp.v2.y
          );
          Tmp.v1.rotate90(1);
          Tmp.v2.rotate90(1);
        }
      }
    };
  }

  static {
    UncCore.aspects.addAspect(new EntityAspect<Bullet>(EntityAspect.Group.bullet, r -> true)
        .setEntryTrigger(bullet -> {
          if(bullet.owner instanceof Unit u && u.hasEffect(electric_disturb)){
            float deflect = 16.4f*Mathf.clamp(u.getDuration(electric_disturb)/120);
            bullet.vel.rotate(Mathf.random(-deflect, deflect));
          }
        }).setExitTrigger(e -> {}));

    UncCore.aspects.addAspect(new EntityAspect<Unit>(EntityAspect.Group.unit, u -> true)
        .setTrigger(new TriggerEntry<>(EventType.Trigger.update, unit -> {
          if(unit.hasEffect(locking)){
            float str = unit.getDuration(locking);

            float[] health = lastHealth.get(unit, () -> new float[]{unit.health});
            if(health[0] != unit.health){
              if(health[0] - 6 > unit.health){
                float damageBase = health[0] - unit.health;
                if(Mathf.chance(0.1f + str/100)){
                  unit.damage(damageBase*12*str/100f, false);
                }
              }
              health[0] = unit.health;
            }
          }
        })).setExitTrigger(e -> {}).setEntryTrigger(e -> {}));
  }
}
