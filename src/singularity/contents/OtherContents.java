package singularity.contents;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.game.EventType;
import mindustry.gen.Bullet;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.Sgl;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.type.AtomSchematic;
import singularity.world.SglFx;
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

  public static StatusEffect
  emp_damaged,
  electric_disturb,
  locking,
  spring_coming,
  wild_growth,
  frost,
  frost_freeze,
  meltdown;

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

    emp_damaged = new StatusEffect("emp_damaged"){
      {
        color = Pal.accent;

        speedMultiplier = 0.5f;
        buildSpeedMultiplier = 0.1f;
        reloadMultiplier = 0.6f;
      }

      @Override
      public void update(Unit unit, float time) {
        super.update(unit, time);

        if (Sgl.empHealth.empDamaged(unit)){
          if (unit.getDuration(this) <= 60){
            unit.apply(this, 60);
          }
          else{
            unit.speedMultiplier = 0.01f;
            unit.reloadMultiplier = 0;
            unit.buildSpeedMultiplier = 0;
          }

          unit.damagePierce(1, true);
          unit.health -= (1 - Sgl.empHealth.healthPresent(unit))*Sgl.empHealth.get(unit).model.empContinuousDamage;

          for (int i = 0; i < unit.abilities.length; i++) {
            if (!(unit.abilities[i] instanceof BanedAbility)){
              BanedAbility baned = Pools.obtain(BanedAbility.class, BanedAbility::new);
              baned.index = i;
              baned.masked = unit.abilities[i];
              unit.abilities[i] = baned;
            }
          }
        }
        else{
          unit.unapply(this);
        }
      }

      class BanedAbility extends Ability implements Pool.Poolable {
        Ability masked;
        int index;

        @Override
        public void update(Unit unit) {
          if (!unit.hasEffect(emp_damaged)) {
            unit.abilities[index] = masked;
            Pools.free(this);
          }
        }

        @Override
        public void reset() {
          masked = null;
          index = -1;
        }
      }
    };

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
          t.add("[lightgray]" + Core.bundle.get("infos.attenuationWithTime") + "[]").padLeft(15);
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

    spring_coming = new StatusEffect("spring_coming"){{
      color = Pal.heal;
      speedMultiplier = 1.23f;
      reloadMultiplier = 1.16f;
      damage = -2;
    }};

    wild_growth = new StatusEffect("wild_growth"){{
      color = Tmp.c1.set(Pal.heal).lerp(Color.black, 0.25f).cpy();
      speedMultiplier = 0.05f;
      reloadMultiplier = 0.7f;
      damage = 1.5f;
    }};

    frost = new StatusEffect("frost"){
      {
        color = SglDrawConst.frost;
        speedMultiplier = 0.5f;
        reloadMultiplier = 0.8f;
        effect = Fx.freezing;

        init(() -> {
          handleOpposite(StatusEffects.burning);
          handleOpposite(StatusEffects.melting);
        });
      }

      @Override
      public void update(Unit unit, float time){
        super.update(unit, time);
        if(time >= 30*unit.hitSize){
          if(unit.getDuration(frost_freeze) <= 0){
            unit.unapply(this);
            unit.apply(frost_freeze, Math.max(time/2, 180));
          }
        }
      }
    };

    frost_freeze = new StatusEffect("frost_freeze"){
      {
        speedMultiplier = 0.05f;
        reloadMultiplier = 0.05f;
        effect = SglFx.iceParticleSpread;

        init(() -> {
          handleOpposite(StatusEffects.burning);
          handleOpposite(StatusEffects.melting);
        });
      }

      @Override
      public void update(Unit unit, float time){
        super.update(unit, time);
        if(unit.getDuration(frost) >= 30*unit.hitSize){
          unit.apply(StatusEffects.freezing, 240);
          unit.unapply(frost);
          unit.unapply(frost_freeze);

          unit.damage(time/3.4f);
          Damage.damage(unit.x, unit.y, unit.hitSize, unit.getDuration(frost)/2.8f);
          Fx.pointShockwave.at(unit.x, unit.y);
          Effect.shake(0.75f, 0.75f, unit);
        }
      }

      @Override
      public void draw(Unit unit, float time){
        super.draw(unit, time);
        float ro = Mathf.randomSeed(unit.id, 360);
        Draw.color(SglDrawConst.frost);
        Draw.alpha(0.85f);
        Draw.z(Layer.flyingUnit);
        SglDraw.drawDiamond(unit.x, unit.y, unit.hitSize*2.35f, unit.hitSize*2, ro, 0.3f);

        Draw.alpha(0.7f);
        int n = Mathf.randomSeed(unit.id + 1, 4, 8);
        for(int i = 0; i < n; i++){
          float off = Mathf.randomSeed(unit.id + 2 + i, unit.hitSize*0.8f, unit.hitSize);
          float len = Mathf.randomSeed(unit.id + 3 + i, unit.hitSize);
          float wid = Mathf.randomSeed(unit.id + 4 + i, unit.hitSize*0.4f, unit.hitSize*0.8f);
          float rot = Mathf.randomSeed(unit.id + 5 + i, 360);

          SglDraw.drawDiamond(unit.x + Angles.trnsx(rot, off), unit.y + Angles.trnsy(rot, off), len, wid, rot, 0.2f);
        }
      }
    };

    meltdown = new StatusEffect("meltdown"){
      {
        damage = 1;
        effect = Fx.melting;

        init(() -> {
          opposite(StatusEffects.freezing, StatusEffects.wet, frost);

          affinity(StatusEffects.tarred, (unit, result, time) -> {
            unit.damagePierce(8f);
            Fx.burning.at(unit.x + Mathf.range(unit.bounds() / 2f), unit.y + Mathf.range(unit.bounds() / 2f));
            result.set(meltdown, 180 + result.time);
          });

          trans(frost_freeze, (e, s, t) -> {
            s.time -= t;
            e.apply(StatusEffects.blasted);
            e.damage(Math.max(e.getDuration(frost), t)/2f);
          });
        });
      }

      @Override
      public void update(Unit unit, float time) {
        super.update(unit, time);
        if (unit.shield > 0){
          unit.shieldAlpha = 1;
          unit.shield -= Time.delta*time/6;
        }
      }
    };
  }

  static {
    UncCore.aspects.addAspect(new EntityAspect<Bullet>(EntityAspect.Group.bullet, r -> true)
        .setEntryTrigger(bullet -> {
          if(bullet.owner instanceof Unit u){
            if(u.hasEffect(electric_disturb)){
              float deflect = 16.4f*Mathf.clamp(u.getDuration(electric_disturb)/120);
              float rot = Mathf.random(-deflect, deflect);
              bullet.rotation(bullet.rotation() + rot);
              Tmp.v1.set(bullet.aimX - bullet.x, bullet.aimY - bullet.y).rotate(rot);
              bullet.aimX = Tmp.v1.x;
              bullet.aimY = Tmp.v1.y;
            }
            if (u.hasEffect(emp_damaged)){
              float rot = Mathf.random(-45, 45);
              bullet.rotation(bullet.rotation() + rot);
              Tmp.v1.set(bullet.aimX - bullet.x, bullet.aimY - bullet.y).rotate(rot);
              bullet.aimX = Tmp.v1.x;
              bullet.aimY = Tmp.v1.y;
            }
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
