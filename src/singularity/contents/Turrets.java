package singularity.contents;

import arc.Core;
import arc.func.Boolf2;
import arc.func.Func;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Interp;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.WaveEffect;
import mindustry.entities.part.HaloPart;
import mindustry.entities.part.RegionPart;
import mindustry.entities.part.ShapePart;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawMulti;
import singularity.graphic.SglDraw;
import singularity.world.SglFx;
import singularity.world.blocks.turrets.SglTurret;
import singularity.world.draw.DrawSglTurret;
import singularity.world.lightnings.LightningContainer;
import singularity.world.lightnings.LightningVertex;
import singularity.world.lightnings.generator.CircleGenerator;
import singularity.world.lightnings.generator.LightningGenerator;
import singularity.world.lightnings.generator.RandomGenerator;
import singularity.world.lightnings.generator.VectorLightningGenerator;

import static arc.math.Angles.randLenVectors;
import static mindustry.entities.Damage.collideLine;
import static mindustry.entities.Damage.findPierceLength;

public class Turrets implements ContentList{
  /**石墨粉云团*/
  public static BulletType graphiteCloud,
  /**闪电（SGL）*/
  lightning;

  /**遮幕*/
  public static Block curtain,
  /**惊蛰*/
  thunder;

  @Override
  public void load(){
    graphiteCloud = new BulletType(0, 0){
      {
        lifetime = 360f;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;
        hitEffect = Fx.none;
        shootEffect = Fx.none;
        despawnEffect = Fx.none;
        smokeEffect = Fx.none;
        drawSize = 45;
      }

      @Override
      public void update(Bullet b){
        super.update(b);
        if(b.timer(0, 4)){
          Units.nearby(b.x - 38, b.y - 38, 76, 76, unit -> {
            if(unit.team != b.team && Tmp.cr1.set(b.x, b.y, 38).contains(Tmp.v1.set(unit.x, unit.y))){
              unit.apply(OtherContents.electric_disturb, Math.min(360 - b.time, 120));
            }
          });
        }
      }

      @Override
      public void draw(Bullet e){
        Draw.z(Layer.bullet - 5);
        Draw.color(Pal.stoneGray);
        Draw.alpha(0.6f);
        randLenVectors(e.id, 18, 45, (x, y) -> {
          float size = Mathf.randomSeed((int) (e.id+x), 14, 18);
          float i = e.fin(Interp.pow3Out);
          Fill.circle(e.x + x*i, e.y + y*i, size*e.fout(Interp.pow5Out));
        });
        Draw.z(Layer.effect);
        Draw.color(Items.graphite.color);
        randLenVectors(e.id + 1, 12, 43, (x, y) -> {
          float size = Mathf.randomSeed((int) (e.id + x), 7, 10);
          size *= e.fout(Interp.pow4In);
          size += Mathf.absin(Time.time + Mathf.randomSeed((int) (e.id + x), 2*Mathf.pi), 3.5f, 2f);
          float i = e.fin(Interp.pow3Out);
          SglDraw.drawLightEdge(e.x + x*i, e.y + y*i, size, size*0.15f, size, size*0.15f);
        });
      }
    };

    curtain = new SglTurret("curtain"){{
      requirements(Category.turret, ItemStack.with());
      itemCapacity = 20;
      range = 144;
      targetGround = false;

      newAmmo(new BasicBulletType(1.6f, 30, "missile"){
        {
          frontColor = Items.graphite.color.cpy().lerp(Color.white, 0.7f);
          backColor = Items.graphite.color;
          width = 7f;
          height = 12f;
          lifetime = 90f;
          ammoMultiplier = 1;
          hitShake = 0.35f;
          scaleLife = true;
          splashDamageRadius = 32;
          splashDamage = 12;
          collidesGround = false;
          collidesTiles = false;
          hitEffect = Fx.explosion;
          trailEffect = Fx.smoke;
          trailChance = 0.12f;
          trailColor = Items.graphite.color;

          hitSound = Sounds.explosion;

          fragOnHit = true;
          fragBullets = 1;
          fragVelocityMin = 0;
          fragVelocityMax = 0;
          fragBullet = graphiteCloud;
        }
      }, true, (bt, ammo) -> {
        bt.add(Core.bundle.format("bullet.damage", ammo.damage));
        bt.row();
        bt.add(Core.bundle.format("bullet.splashdamage", (int)ammo.splashDamage, Strings.fixed(ammo.splashDamageRadius/Vars.tilesize, 1)));
        bt.row();
        bt.add(Core.bundle.get("infos.curtainAmmo"));
        bt.row();
        bt.add(OtherContents.electric_disturb.emoji() + "[stat]" + OtherContents.electric_disturb.localizedName + "[lightgray] ~ [stat]2[lightgray] " + Core.bundle.get("unit.seconds"));
      });
      consume.item(Items.graphite, 5);
      consume.time(90);
    }};

    thunder = new SglTurret("thunder"){{
      requirements(Category.turret, ItemStack.with());
      float shootRan;
      size = 5;
      shootRan = range = 400;
      warmupSpeed = 0.016f;
      linearWarmup = false;
      fireWarmupThreshold = 0.9f;
      rotateSpeed = 1.6f;
      cooldownTime = 90;
      recoil = 1.6f;

      energyCapacity = 1024;

      shootY = 22;

      shake = 4;
      shootSound = Sounds.laserblast;

      newAmmo(new BulletType(){
        final RandomGenerator branch = new RandomGenerator();

        {
          speed = 0;
          lifetime = 60;
          collides = false;
          hittable = false;
          absorbable = false;
          splashDamage = 760;
          splashDamageRadius = 46;
          damage = 0;

          hitColor = Pal.reactorPurple;
          shootEffect = new MultiEffect(SglFx.impactBubble, SglFx.shootRecoilWave, new WaveEffect(){{
            colorFrom = colorTo = Pal.reactorPurple;
            lifetime = 12f;
            sizeTo = 40f;
            strokeFrom = 6f;
            strokeTo = 0.3f;
          }});

          hitEffect = Fx.none;
          despawnEffect = Fx.none;
          smokeEffect = Fx.none;

          fragBullet = lightning(60, 34, 6.5f, Pal.reactorPurple, b -> new RandomGenerator(){{
            maxLength = 100;
            maxDeflect = 55;

            originAngle = b.rotation();

            branchChance = 0.2f;
            minBranchStrength = 0.8f;
            maxBranchStrength = 1;
            branchMaker = (vert, strength) -> {
              branch.maxLength = 60*strength;
              branch.originAngle = vert.angle + Mathf.random(-90, 90);

              return branch;
            };
          }});
          fragSpread = 0;
          fragOnHit = false;
        }

        final VectorLightningGenerator generator = new VectorLightningGenerator(){{
          maxSpread = 14;
          minInterval = 12;
          maxInterval = 20;

          branchChance = 0.1f;
          minBranchStrength = 0.5f;
          maxBranchStrength = 0.8f;
          branchMaker = (vert, strength) -> {
            branch.maxLength = 60*strength;
            branch.originAngle = vert.angle + Mathf.random(-90, 90);

            return branch;
          };
        }};

        @Override
        public void init(Bullet b){
          super.init(b);

          LightningContainer container = new LightningContainer();
          container.lifeTime = lifetime;
          container.minWidth = 5;
          container.maxWidth = 8;
          container.time = 6;
          b.data = container;

          Tmp.v1.set(b.aimX - b.originX, b.aimY - b.originY);
          float scl = Mathf.clamp(Tmp.v1.len()/shootRan);
          Tmp.v1.setLength(shootRan).scl(scl);

          float shX = b.x + Tmp.v1.x;
          float shY = b.y + Tmp.v1.y;

          container.generator = generator;
          generator.vector.set(
              shX - b.originX,
              shY - b.originY
          );

          int amount = Mathf.random(5, 7);
          for(int i = 0; i < amount; i++){
            container.create();
          }

          Time.run(6, () -> {
            SglFx.lightningBoltWave.at(shX, shY, Pal.reactorPurple);
            createFrags(b, shX, shY);
            Effect.shake(6, 6, shX, shY);
            Sounds.laserbig.at(shX, shY, hitSoundPitch, hitSoundVolume);
            Damage.damage(shX, shY, splashDamageRadius, splashDamage);
          });
        }

        @Override
        public void update(Bullet b){
          super.update(b);
          ((LightningContainer) b.data).update();
        }

        @Override
        public void draw(Bullet b){
          LightningContainer container = (LightningContainer) b.data;
          Draw.z(Layer.bullet);
          Draw.color(Pal.reactorPurple);
          container.draw(b.x, b.y);
        }

        @Override
        public void createSplashDamage(Bullet b, float x, float y){}

        @Override
        public void despawned(Bullet b){}
      });
      consume.item(SglItems.crystal_FEX_power, 2);
      consume.energy(2.2f);
      consume.time(180);

      initialed = e -> {
        e.setVar("lightningContainer", new LightningContainer(){{
          generator = new CircleGenerator(){{
            radius = 8;
            maxSpread = 2.5f;
            minInterval = 2;
            maxInterval = 2.5f;
          }};
          lifeTime = 45f;
          maxWidth = 2f;
          lerp = f -> 1 - f;
          time = 0;
        }});
      };

      int timeId = timers++;
      updating = e -> {
        e.<LightningContainer>getVar("lightningContainer").update();
        SglTurretBuild turret = (SglTurretBuild) e;
        if(turret.warmup > 0 && e.timer(timeId, 25/turret.warmup)){
          e.<LightningContainer>getVar("lightningContainer").create();
        }

        if(Mathf.chanceDelta(0.03f*turret.warmup)){
          Tmp.v1.set(0, -16).rotate(turret.drawrot());
          SglFx.randomLightning.at(e.x + Tmp.v1.x, e.y + Tmp.v1.y, Pal.reactorPurple);
        }
      };

      draw = new DrawMulti(
          new DrawSglTurret("reinforced-",
              new RegionPart("_center"){{
                moveY = 8;
                progress = PartProgress.warmup;
                heatColor = Pal.reactorPurple;
                heatProgress = PartProgress.warmup.delay(0.25f);

                moves.add(new PartMove(PartProgress.recoil, 0f, -4f, 0f));
              }},
              new RegionPart("_body"){{
                progress = PartProgress.warmup;
                heatColor = Pal.reactorPurple;
                heatProgress = PartProgress.warmup.delay(0.25f);
              }},
              new RegionPart("_side"){{
                mirror = true;
                turretShading = true;
                moveX = 5;
                moveY = -5;
                progress = PartProgress.warmup;
                heatColor = Pal.reactorPurple;
                heatProgress = PartProgress.warmup.delay(0.25f);
              }},
              new ShapePart(){{
                color = Pal.reactorPurple;
                circle = true;
                hollow = true;
                stroke = 0;
                strokeTo = 2f;
                y = -16;
                radius = 0;
                radiusTo = 10f;
                progress = PartProgress.warmup;
                layer = Layer.effect;
              }},
              new ShapePart(){{
                circle = true;
                y = -16;
                radius = 0;
                radiusTo = 3.5f;
                color = Pal.reactorPurple;
                layer = Layer.effect;
                progress = PartProgress.warmup;
              }},
              new HaloPart(){{
                progress = PartProgress.warmup;
                color = Pal.reactorPurple;
                layer = Layer.effect;
                y = -16;
                haloRotation = 90f;
                shapes = 2;
                triLength = 0f;
                triLengthTo = 30f;
                haloRadius = 10f;
                tri = true;
                radius = 4f;
              }},
              new HaloPart(){{
                progress = PartProgress.warmup;
                color = Pal.reactorPurple;
                layer = Layer.effect;
                y = -16;
                haloRotation = 90f;
                shapes = 2;
                triLength = 0f;
                triLengthTo = 6f;
                haloRadius = 10f;
                tri = true;
                radius = 4f;
                shapeRotation = 180f;
              }},
              new ShapePart(){{
                circle = true;
                y = 22;
                radius = 0;
                radiusTo = 5;
                color = Pal.reactorPurple;
                layer = Layer.effect;
                progress = PartProgress.warmup;
              }},
              new ShapePart(){{
                color = Pal.reactorPurple;
                circle = true;
                hollow = true;
                stroke = 0;
                strokeTo = 1.5f;
                y = 22;
                radius = 0;
                radiusTo = 8;
                progress = PartProgress.warmup;
                layer = Layer.effect;
              }}
          ),
          new DrawBlock(){
            @Override
            public void draw(Building build){
              SglTurretBuild turret = (SglTurretBuild) build;
              Draw.z(Layer.effect);
              Draw.color(Pal.reactorPurple);
              Tmp.v1.set(1, 0).setAngle(turret.rotation);
              float sclX = Tmp.v1.x, sclY = Tmp.v1.y;
              turret.<LightningContainer>getVar("lightningContainer").draw(turret.x + sclX*22, turret.y + sclY*22);

              float step = 45/16f;
              if(turret.warmup < 0.001f) return;
              for(int i = 0; i < 16; i++){
                float x = turret.x + (step*i)*sclX*turret.warmup + 14*sclX;
                float y = turret.y + (step*i)*sclY*turret.warmup + 14*sclY;
                SglDraw.drawRectAsCylindrical(x, y,
                    Mathf.randomSeed(turret.id + i, 2, 18)*turret.warmup,
                    Mathf.randomSeed(turret.id + i + 1, 1.5f, 10),
                    (10 + i*0.75f + Mathf.randomSeed(turret.id + i + 2, 8))*turret.warmup,
                    (Time.time*Mathf.randomSeed(turret.id + i + 3, 0.8f, 2) + Mathf.randomSeed(turret.id + i + 4, 360))
                        *(Mathf.randomSeed(turret.id + i + 5, 1f) < 0.5? -1: 1),
                    turret.drawrot(),
                    Pal.reactorPurple, Pal.reactorPurple2, Layer.bullet - 0.5f, Layer.effect
                );
              }
            }
          }
      );
    }};
  }

  public BulletType lightning(float lifeTime, float damage, float size, Color color, Func<Bullet, LightningGenerator> generator){
    return new BulletType(0, damage){
      {
        lifetime = lifeTime;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;

        hitEffect = Fx.hitLancer;
        hitColor = color;
        shootEffect = Fx.none;
        despawnEffect = Fx.none;
        smokeEffect = Fx.none;

        status = StatusEffects.shocked;
        statusDuration = 18;

        drawSize = 120;
      }

      @Override
      public void init(Bullet b){
        super.init(b);

        LightningContainer container;
        b.data = container = new LightningContainer();
        container.time = lifeTime/2;
        container.lifeTime = lifeTime;
        container.generator = generator.get(b);
        container.maxWidth = size;
        container.minWidth = size*0.85f;
        container.trigger = (last, vert) -> {
          Tmp.v1.set(vert.x - last.x, vert.y - last.y);
          float resultLength = findPierceLength(b, pierceCap, Tmp.v1.len());

          collideLine(b, b.team, b.type.hitEffect, b.x + last.x, b.y + last.y, Tmp.v1.angle(), resultLength, false, false, pierceCap);

          b.fdata = resultLength;
        };

        Boolf2<LightningVertex, LightningVertex> old = container.generator.blockNow;
        container.generator.blockNow = (last, vertex) -> {
          if(Damage.findAbsorber(b.team, b.x + last.x, b.y + last.y, b.x + vertex.x, b.y + vertex.y) != null) return true;
          return old != null && old.get(last, vertex);
        };
        container.create();
      }

      @Override
      public void update(Bullet b){
        super.update(b);
        ((LightningContainer) b.data).update();
      }

      @Override
      public void draw(Bullet b){
        LightningContainer container = (LightningContainer) b.data;
        Draw.z(Layer.bullet);
        Draw.color(color);
        container.draw(b.x, b.y);
      }
    };
  }
}
