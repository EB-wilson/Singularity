package singularity.contents;

import arc.Core;
import arc.func.Func;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.pooling.Pools;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.WaveEffect;
import mindustry.entities.part.HaloPart;
import mindustry.entities.part.RegionPart;
import mindustry.entities.part.ShapePart;
import mindustry.entities.pattern.ShootPattern;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawMulti;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.world.SglFx;
import singularity.world.blocks.turrets.LaserTurret;
import singularity.world.blocks.turrets.ProjectileTurret;
import singularity.world.blocks.turrets.SglTurret;
import singularity.world.blocks.turrets.WarpedBulletType;
import singularity.world.draw.DrawSglTurret;
import singularity.world.draw.part.CustomPart;
import singularity.world.meta.SglStat;
import universecore.util.UncLiquidStack;
import universecore.world.lightnings.LightningContainer;
import universecore.world.lightnings.generator.CircleGenerator;
import universecore.world.lightnings.generator.LightningGenerator;
import universecore.world.lightnings.generator.RandomGenerator;
import universecore.world.lightnings.generator.VectorLightningGenerator;

import static arc.math.Angles.randLenVectors;
import static mindustry.Vars.control;
import static mindustry.Vars.tilesize;
import static mindustry.entities.Damage.collideLine;
import static mindustry.entities.Damage.findPierceLength;

public class Turrets implements ContentList{
  /**石墨粉云团*/
  public static BulletType graphiteCloud,
  /**闪电（SGL）*/
  lightning,
  /**碎冰*/
  crushedIce,
  /**极寒领域*/
  freezingField;

  /**遮幕*/
  public static Block curtain,
  /**惊蛰*/
  thunder,
  /**白露*/
  dew,
  /**春分*/
  spring,
  /**霜降*/
  frost,
  /**冬至*/
  winter,
  /**夏至*/
  summer;

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

    crushedIce = new BulletType(){
      {
        lifetime = 45;
        hitColor = SglDrawConst.frost;
        hitEffect = SglFx.continuousLaserRecoil;
        damage = 18;
        speed = 3;
        collidesGround = true;
        collidesAir = false;
        pierceCap = 1;
        hitSize = 2;
      }

      @Override
      public void hitEntity(Bullet b, Hitboxc entity, float health){
        if(entity instanceof Healthc h){
          h.damage(b.damage);
        }

        if(entity instanceof Unit unit){
          unit.apply(OtherContents.frost, unit.getDuration(OtherContents.frost) + 8);
        }
      }

      @Override
      public void draw(Bullet b){
        Draw.color(SglDrawConst.frost);
        SglDraw.drawDiamond(b.x, b.y, 6*b.fout(), 3*b.fout(), b.rotation());
      }
    };

    freezingField = new BulletType(){
      {
        lifetime = 600;
        hittable = false;
        pierce = true;
        absorbable = false;
        collides = false;
        despawnEffect = Fx.none;
        hitEffect = Fx.none;
        drawSize = 200;
      }

      @Override
      public void update(Bullet b){
        super.update(b);
        float radius = 200*b.fout();
        Damage.damage(b.team, b.x, b.y, radius, 12*Time.delta);

        control.sound.loop(Sounds.windhowl, b, 2);

        if(Mathf.chanceDelta(0.075f*b.fout())){
          SglFx.iceParticleSpread.at(b.x, b.y, SglDrawConst.winter);
        }
        if(Mathf.chanceDelta(0.25f*b.fout(Interp.pow2Out))){
          Angles.randLenVectors((long) Time.time, 1, radius, (dx, dy) -> {
            if(Mathf.chanceDelta(0.7f)){
              SglFx.iceParticle.at(b.x + dx, b.y + dy, -45+Mathf.random(-15, 15), SglDrawConst.frost);
            }
            else{
              SglFx.iceCrystal.at(b.x + dx, b.y + dy, SglDrawConst.frost);
            }
          });
        }

        Units.nearbyEnemies(b.team, b.x, b.y, radius, unit -> {
          unit.apply(OtherContents.frost, unit.getDuration(OtherContents.frost) + 2.5f*Time.delta);
        });
      }

      @Override
      public void draw(Bullet b){
        super.draw(b);
        Draw.z(Layer.flyingUnit + 0.01f);
        Draw.color(SglDrawConst.winter);
        Draw.alpha(0);
        float lerp = b.fin() <= 0.1f? 1 - Mathf.pow(1 - Mathf.clamp(b.fin()/0.1f), 2): Mathf.clamp(b.fout()/0.9f);
        SglDraw.gradientCircle(b.x, b.y, 215*lerp, 0.8f);
      }
    };

    curtain = new SglTurret("curtain"){{
      requirements(Category.turret, ItemStack.with(
          Items.titanium, 20,
          Items.graphite, 20,
          Items.lead, 12
      ));
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
        bt.add(Core.bundle.format("bullet.splashdamage", (int)ammo.splashDamage, Strings.fixed(ammo.splashDamageRadius/tilesize, 1)));
        bt.row();
        bt.add(Core.bundle.get("infos.curtainAmmo"));
        bt.row();
        bt.add(OtherContents.electric_disturb.emoji() + "[stat]" + OtherContents.electric_disturb.localizedName + "[lightgray] ~ [stat]2[lightgray] " + Core.bundle.get("unit.seconds"));
      });
      consume.item(Items.graphite, 5);
      consume.time(90);
    }};

    thunder = new SglTurret("thunder"){{
      requirements(Category.turret, ItemStack.with(
          SglItems.strengthening_alloy, 180,
          SglItems.aerogel, 150,
          Items.surgeAlloy, 120,
          SglItems.matrix_alloy, 100,
          SglItems.crystal_FEX, 100,
          SglItems.crystal_FEX_power, 80,
          SglItems.iridium, 80
      ));
      float shootRan;
      size = 5;
      scaledHealth = 320;
      shootRan = range = 400;
      warmupSpeed = 0.01f;
      linearWarmup = false;
      fireWarmupThreshold = 0.9f;
      rotateSpeed = 1.6f;
      cooldownTime = 90;
      recoil = 3.4f;

      energyCapacity = 1024;

      squareSprite = false;

      shootY = 22;

      shake = 4;
      shootSound = Sounds.largeCannon;

      newAmmo(new BulletType(){
        final RandomGenerator branch = new RandomGenerator();

        {
          speed = 0;
          lifetime = 60;
          collides = false;
          hittable = false;
          absorbable = false;
          splashDamage = 1460;
          splashDamageRadius = 46;
          damage = 0;
          drawSize = shootRan;

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

          fragBullet = lightning(60, 42, 6.5f, Pal.reactorPurple, b -> new RandomGenerator(){{
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

          LightningContainer container = Pools.obtain(LightningContainer.PoolLightningContainer.class, LightningContainer.PoolLightningContainer::new);;
          container.lifeTime = lifetime;
          container.minWidth = 5;
          container.maxWidth = 8;
          container.time = 6;
          b.data = container;

          Tmp.v1.set(b.aimX - b.originX, b.aimY - b.originY);
          float scl = Mathf.clamp(Tmp.v1.len()/shootRan);
          Tmp.v1.setLength(shootRan).scl(scl);

          float shX, shY;

          Building absorber = Damage.findAbsorber(b.team, b.originX, b.originY, b.originX + Tmp.v1.x, b.originY + Tmp.v1.y);
          if(absorber != null){
            shX = absorber.x;
            shY = absorber.y;
          }
          else{
            shX = b.x + Tmp.v1.x;
            shY = b.y + Tmp.v1.y;
          }

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
            Sounds.largeExplosion.at(shX, shY, hitSoundPitch, hitSoundVolume);
            Damage.damage(b.team, shX, shY, splashDamageRadius, splashDamage);
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

        @Override
        public void removed(Bullet b){
          super.removed(b);
          if(b.data instanceof LightningContainer.PoolLightningContainer c){
            Pools.free(c);
          }
        }
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

      newCoolant(1.45f, 20);
      consume.liquid(SglLiquids.phase_FEX_liquid, 0.25f);

      draw = new DrawMulti(
          new DrawSglTurret(
              new RegionPart("_center"){{
                moveY = 8;
                progress = PartProgress.warmup;
                heatColor = Pal.reactorPurple;
                heatProgress = PartProgress.warmup.delay(0.25f);

                moves.add(new PartMove(PartProgress.recoil, 0f, -4f, 0f));
              }},
              new RegionPart("_body"){{
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

    dew = new ProjectileTurret("dew"){{
      requirements(Category.turret, ItemStack.with(
          SglItems.strengthening_alloy, 150,
          SglItems.aluminium, 110,
          SglItems.aerogel, 120,
          SglItems.matrix_alloy, 160,
          Items.thorium, 100,
          Items.silicon, 85,
          SglItems.uranium_238, 85
      ));
      size = 5;
      scaledHealth = 360;
      rotateSpeed = 2.5f;
      range = 350;
      shootY = 17.4f;
      warmupSpeed = 0.03f;
      linearWarmup = false;
      recoil = 0f;
      fireWarmupThreshold = 0.85f;
      shootCone = 15;
      shake = 2.2f;

      squareSprite = false;

      shootSound = Sounds.shootAlt;

      shoot = new ShootPattern(){
        @Override
        public void shoot(int totalShots, BulletHandler handler){
          float off = totalShots%2 - 0.5f;

          for(int i = 0; i < 3; i++){
            handler.shoot(off*16, 0, 0f, firstShotDelay + 3 * i);
          }
        }
      };

      newAmmo(new BulletType(){
        {
          damage = 60;
          speed = 8;
          lifetime = 45;
          hitSize = 4.3f;
          hitColor = SglDrawConst.matrixNet;
          hitEffect = Fx.colorSpark;
          despawnEffect = Fx.circleColorSpark;
          trailEffect = SglFx.polyParticle;
          trailRotation = true;
          trailChance = 0.04f;
          trailColor = SglDrawConst.matrixNet;
          shootEffect = new MultiEffect(Fx.shootBig, Fx.colorSparkBig);
          hittable = true;
          pierceBuilding = true;
          pierceCap = 4;
        }

        @Override
        public void draw(Bullet b){
          SglDraw.drawDiamond(b.x, b.y, 18, 6, b.rotation(), SglDrawConst.matrixNet);
          Draw.color(SglDrawConst.matrixNet);
          for(int i : Mathf.signs){
            Drawf.tri(b.x, b.y, 6f*b.fin(), 20f*b.fin(), b.rotation() + 156f*i);
          }
        }
      });
      consume.item(Items.thorium, 3);
      consume.time(10);

      newAmmoCoating(Core.bundle.get("coating.depletedUranium"), b -> new WarpedBulletType(b){
        {
          damage = b.damage*1.15f;
          pierceArmor = true;
          pierceCap = 5;
        }

        public void hitEntity(Bullet b, Hitboxc entity, float health){
          if(entity instanceof Unit unit){
            if(unit.shield > 0){
              float damageShield = Math.min(Math.max(unit.shield, 0), b.type.damage*0.85f);
              unit.shield -= damageShield;
              Fx.colorSparkBig.at(b.x, b.y, b.rotation(), Pal.bulletYellowBack);
            }
          }
          super.hitEntity(b, entity, health);
        }

        @Override
        public void draw(Bullet b){
          SglDraw.drawDiamond(b.x, b.y, 24, 6, b.rotation(), Pal.accent);
          Draw.color(SglDrawConst.matrixNet);
          for(int i : Mathf.signs){
            Drawf.tri(b.x, b.y, 6f*b.fin(), 30f*b.fin(), b.rotation() + 162f*i);
          }
        }
      }, t -> {
        t.add(SglStat.exDamageMultiplier.localized() + 115 + "%");
        t.row();
        t.add(SglStat.exShieldDamage.localized() + 85 + "%");
        t.row();
        t.add(SglStat.exPierce.localized() + ": 1");
        t.row();
        t.add("@bullet.armorpierce");
      });
      consume.time(10);
      consume.item(SglItems.uranium_238, 1);

      draw = new DrawSglTurret(
          new RegionPart("_blade"){{
            mirror = true;
            turretShading = true;
            moveX = 4;
            progress = PartProgress.warmup;
            heatColor = SglDrawConst.dew;
            heatProgress = PartProgress.heat;

            moves.add(new PartMove(PartProgress.recoil, 0, -2.6f, 0));
          }},
          new RegionPart("_side"){{
            mirror = true;
            turretShading = true;
            moveX = 8;
            moveRot = -25;
            progress = PartProgress.warmup;
            heatColor = SglDrawConst.dew;
            heatProgress = PartProgress.warmup.delay(0.25f);

            moves.add(new PartMove(PartProgress.recoil, 1f, -1f, -5));
          }},
          new RegionPart("_body"){{
            heatColor = SglDrawConst.dew;
            heatProgress = PartProgress.warmup.delay(0.25f);
          }},
          new ShapePart(){{
            layer = Layer.effect;
            color = SglDrawConst.matrixNet;
            x = 0;
            y = -16;
            circle = true;
            hollow = true;
            stroke = 0;
            strokeTo = 1.8f;
            radius = 0;
            radiusTo = 8;
            progress = PartProgress.warmup;
          }},
          new HaloPart(){{
            progress = PartProgress.warmup;
            color = SglDrawConst.matrixNet;
            layer = Layer.effect;
            y = -16;
            shapes = 1;
            triLength = 16f;
            triLengthTo = 46f;
            haloRadius = 0;
            tri = true;
            radius = 0;
            radiusTo = 4;
          }},
          new HaloPart(){{
            progress = PartProgress.warmup;
            color = SglDrawConst.matrixNet;
            layer = Layer.effect;
            y = -16;
            shapes = 1;
            triLength = 8f;
            triLengthTo = 20f;
            haloRotation = 180;
            haloRadius = 0;
            tri = true;
            radius = 0;
            radiusTo = 4;
          }},
          new HaloPart(){{
            progress = PartProgress.warmup;
            color = SglDrawConst.matrixNet;
            layer = Layer.effect;
            y = -16;
            shapes = 2;
            haloRotation = 90;
            triLength = 6f;
            triLengthTo = 24f;
            haloRadius = 0;
            tri = true;
            radius = 0;
            radiusTo = 2.5f;
          }},
          new HaloPart(){{
            progress = PartProgress.recoil.delay(0.3f);
            color = SglDrawConst.matrixNet;
            layer = Layer.effect;
            mirror = true;
            x = 2;
            y = -6;
            haloRotation = -135f;
            shapes = 1;
            triLength = 14f;
            triLengthTo = 21f;
            haloRadius = 10f;
            tri = true;
            radius = 0;
            radiusTo = 6;
          }},
          new HaloPart(){{
            progress = PartProgress.recoil.delay(0.3f);
            color = SglDrawConst.matrixNet;
            layer = Layer.effect;
            mirror = true;
            x = 2;
            y = -6;
            haloRotation = -135;
            shapes = 1;
            triLength = 0f;
            triLengthTo = 6f;
            haloRadius = 10f;
            tri = true;
            radius = 0;
            radiusTo = 6f;
            shapeRotation = 180f;
          }},
          new HaloPart(){{
            progress = PartProgress.recoil.delay(0.3f);
            color = SglDrawConst.matrixNet;
            layer = Layer.effect;
            mirror = true;
            x = 22;
            y = -6;
            haloRotation = -135f;
            shapes = 1;
            triLength = 8f;
            triLengthTo = 16f;
            haloRadius = 0f;
            tri = true;
            radius = 0;
            radiusTo = 4.5f;
          }},
          new HaloPart(){{
            progress = PartProgress.recoil.delay(0.3f);
            color = SglDrawConst.matrixNet;
            layer = Layer.effect;
            mirror = true;
            x = 22;
            y = -6;
            haloRotation = -135;
            shapes = 1;
            triLength = 0f;
            triLengthTo = 4f;
            haloRadius = 0f;
            tri = true;
            radius = 0;
            radiusTo = 4.5f;
            shapeRotation = 180f;
          }},
          new HaloPart(){{
            progress = PartProgress.recoil.delay(0.3f);
            color = SglDrawConst.matrixNet;
            layer = Layer.effect;
            mirror = true;
            x = 12;
            y = -4;
            haloRotation = -160f;
            shapes = 1;
            triLength = 12f;
            triLengthTo = 20f;
            haloRadius = 0f;
            tri = true;
            radius = 0;
            radiusTo = 5f;
          }},
          new HaloPart(){{
            progress = PartProgress.recoil.delay(0.3f);
            color = SglDrawConst.matrixNet;
            layer = Layer.effect;
            mirror = true;
            x = 12;
            y = -4;
            haloRotation = -160;
            shapes = 1;
            triLength = 0f;
            triLengthTo = 5f;
            haloRadius = 0f;
            tri = true;
            radius = 0;
            radiusTo = 5;
            shapeRotation = 180f;
          }}
      );

      newCoolant(1f, 0.25f, l -> l.heatCapacity > 0.7f && l.temperature < 0.35f, 0.4f, 20);
    }};

    spring = new SglTurret("spring"){{
      requirements(Category.turret, ItemStack.with(
          SglItems.strengthening_alloy, 120,
          SglItems.aluminium, 140,
          Items.phaseFabric, 80,
          SglItems.matrix_alloy, 100,
          SglItems.chlorella, 120,
          SglItems.crystal_FEX_power, 85,
          SglItems.iridium, 60
      ));
      size = 5;
      scaledHealth = 450;
      recoil = 1.8f;
      rotateSpeed = 3;
      warmupSpeed = 0.018f;
      linearWarmup = false;
      fireWarmupThreshold = 0.9f;
      range = 400;
      targetGround = true;
      targetHealUnit = true;
      targetAir = true;
      targetHealing = true;
      shootY = 12;
      shootEffect = Fx.none;

      squareSprite = false;

      shootSound = Sounds.malignShoot;

      shoot = new ShootPattern(){
        @Override
        public void shoot(int totalShots, BulletHandler handler){
          for(int i: new int[]{-1, 1}){
            for(int a = 1; a <= 2; a++){
              int am = a;
              handler.shoot(0, 0, 4.57f*i, 0, b -> {
                float len = b.time*5f;
                b.moveRelative(0, i*(4*am - 0.01f*am*len)*Mathf.sin(0.04f*len + 4));
              });
            }
          }
        }
      };
      shoot.shots = 2;

      newAmmo(new BulletType(){
        {
          damage = 42;
          lifetime = 80;
          speed = 5f;
          drawSize = 24;
          pierce = true;
          pierceBuilding = true;
          collidesTeam = true;
          smokeEffect = Fx.none;
          hitColor = Pal.heal;
          hitEffect = Fx.circleColorSpark;
          healEffect = SglFx.impactBubble;
          shootEffect = Fx.none;
          healAmount = 24;
          healPercent = 1f;
          hitSize = 8;
          trailColor = Pal.heal;
          trailRotation = true;
          trailEffect = Fx.disperseTrail;
          trailInterval = 3f;
          trailWidth = hitSize;
          trailLength = 24;

          hitSound = Sounds.plasmadrop;
        }

        @Override
        public void draw(Bullet b){
          super.draw(b);
          Draw.color(Pal.heal);
          Fill.circle(b.x, b.y, b.hitSize);
        }

        @Override
        public void update(Bullet b){
          super.update(b);
          Units.nearby(b.team, b.x, b.y, b.hitSize, u -> {
            if(u.damaged() && !b.hasCollided(u.id)){
              b.collided.add(u.id);

              u.heal(u.maxHealth*(b.type.healPercent/100) + b.type.healAmount);
              u.apply(OtherContents.spring_coming, 30);
              b.type.healEffect.at(b.x, b.y, b.rotation(), b.type.healColor);
            }
          });
          Damage.status(b.team, b.x, b.y, b.hitSize, OtherContents.wild_growth, 12, true, true);
        }
      }, (s, b) -> {
        s.add(Core.bundle.get("misc.toTeam") + " " + OtherContents.spring_coming.emoji()
            + "[stat]" + OtherContents.spring_coming.localizedName + "[lightgray] ~ [stat]0.5[lightgray] " + Core.bundle.get("unit.seconds") + "[]"
            + "\n" + Core.bundle.get("misc.toEnemy") + " " + OtherContents.wild_growth.emoji()
            + "[stat]" + OtherContents.wild_growth.localizedName + "[lightgray] ~ [stat]0.2[lightgray] " + Core.bundle.get("unit.seconds") + "[]"
        );
      });
      consume.energy(2.6f);
      consume.time(20);

      draw = new DrawSglTurret(
          new RegionPart("_side"){{
            mirror = true;
            turretShading = true;
            moveX = 2;
            heatColor = Pal.heal;
            progress = PartProgress.warmup;
            heatProgress = PartProgress.warmup.delay(0.25f);

            moves.add(new PartMove(PartProgress.recoil, 0, -2, -6));
          }},
          new RegionPart("_blade"){{
            mirror = true;
            turretShading = true;
            moveY = 4;
            moveRot = -30;
            heatColor = Pal.heal;
            progress = PartProgress.warmup;
            heatProgress = PartProgress.warmup.delay(0.25f);

            moves.add(new PartMove(PartProgress.recoil, -2, -2, 0));
          }},
          new RegionPart("_body"){{
            heatColor = Pal.heal;
            heatProgress = PartProgress.warmup.delay(0.25f);
          }},
          new CustomPart(){{
            mirror = true;
            x = 10;
            y = 16;
            drawRadius = 0;
            drawRadiusTo = 4;
            rotation = -30;

            moveY = -10f;
            moveX = 2;
            moveRot = -35;
            progress = PartProgress.warmup;
            layer = Layer.effect;
            draw = (x, y, r, p) -> {
              Draw.color(Pal.heal);
              Drawf.tri(x, y, 4*p, 6 + 10*p, r);
              Drawf.tri(x, y, 4*p, 4*p, r + 180);
            };

            moves.add(new PartMove(PartProgress.recoil, 0, -1, -3));
          }},
          new CustomPart(){{
            mirror = true;
            x = 10;
            y = 12;
            drawRadius = 0;
            drawRadiusTo = 4f;
            rotation = -30;

            moveY = -10;
            moveX = 2;
            moveRot = -65;
            progress = PartProgress.warmup;
            layer = Layer.effect;
            draw = (x, y, r, p) -> {
              Draw.color(Pal.heal);
              Drawf.tri(x, y, 6*p, 8 + 12*p, r);
              Drawf.tri(x, y, 6*p, 6*p, r + 180);
            };

            moves.add(new PartMove(PartProgress.recoil, 0, -1, -5));
          }},
          new CustomPart(){{
            mirror = true;
            x = 8;
            y = 16;
            drawRadius = 0;
            drawRadiusTo = 5f;
            rotation = -30;

            moveY = -20;
            moveX = 4;
            moveRot = -90;
            progress = PartProgress.warmup;
            layer = Layer.effect;
            draw = (x, y, r, p) -> {
              Draw.color(Pal.heal);
              Drawf.tri(x, y, 8*p, 8 + 16*p, r);
              Drawf.tri(x, y, 8*p, 8*p, r + 180);
            };

            moves.add(new PartMove(PartProgress.recoil, 0, -2, -6));
          }}
      );
    }};

    frost = new LaserTurret("frost"){{
      requirements(Category.turret, ItemStack.with(
          SglItems.strengthening_alloy, 160,
          SglItems.aluminium, 110,
          Items.phaseFabric, 100,
          SglItems.matrix_alloy, 120,
          SglItems.crystal_FEX_power, 100,
          SglItems.iridium, 100
      ));
      size = 5;
      scaledHealth = 420;
      recoil = 2.8f;
      rotateSpeed = 2;
      warmupSpeed = 0.02f;
      fireWarmupThreshold = 0.9f;
      linearWarmup = false;
      range = 360;
      targetGround = true;
      targetAir = true;
      shootEffect = SglFx.continuousLaserRecoil;

      shootSound = Sounds.laserbig;
      loopSound = Sounds.beam;
      loopSoundVolume = 2f;

      squareSprite = false;

      updating = e -> {
        SglTurretBuild t = (SglTurretBuild) e;
        if(Mathf.chanceDelta(0.08f*e.warmup())) SglFx.iceParticle.at(
            t.x + Angles.trnsx(t.rotation, -12),
            t.y + Angles.trnsy(t.rotation, -12),
            t.rotation + 90*(Mathf.randomBoolean()? 1: -1),
            SglDrawConst.frost
        );
        if(Mathf.chanceDelta(0.05f*e.warmup())) SglFx.iceParticle.at(
            t.x + Angles.trnsx(t.rotation, 22),
            t.y + Angles.trnsy(t.rotation, 22),
            t.rotation + 15*(Mathf.randomBoolean()? 1: -1),
            SglDrawConst.frost
        );
      };

      newAmmo(new ContinuousLaserBulletType(){
        {
          damage = 145;
          lifetime = 300;
          damageInterval = 6;
          fadeTime = 30;
          length = 360;
          width = 8;
          hitColor = SglDrawConst.frost;
          fragBullet = crushedIce;
          fragBullets = 2;
          fragSpread = 10;
          fragOnHit = true;
          fragRandomSpread = 60;
          incendAmount = 0;
          incendChance = 0;
          drawSize = 500;
          pointyScaling = 0.7f;
          oscMag = 0.8f;
          oscScl = 1.2f;
          frontLength = 70;
          lightColor = SglDrawConst.frost;
          colors = new Color[]{
              Color.valueOf("6CA5FF").a(0.6f),
              Color.valueOf("6CA5FF").a(0.85f),
              Color.valueOf("ACE7FF"),
              Color.valueOf("DBFAFF")
          };
        }

        @Override
        public void hitEntity(Bullet b, Hitboxc entity, float health){
          if(entity instanceof Healthc h){
            h.damage(b.damage);
          }

          if(entity instanceof Unit unit){
            unit.apply(OtherContents.frost, unit.getDuration(OtherContents.frost) + 18);
          }
        }
      });
      consume.liquid(Liquids.cryofluid, 0.4f);
      consume.energy(2.4f);
      consume.time(180);

      draw = new DrawSglTurret(
          new RegionPart("_side"){{
            turretShading = true;
            mirror = true;
            moveX = 8;
            moveRot = -22;
            heatColor = SglDrawConst.frost;
            progress = PartProgress.warmup;
            heatProgress = PartProgress.warmup.delay(0.5f);

            moves.add(new PartMove(PartProgress.recoil, 0, -2, -8));
          }},
          new RegionPart("_blade"){{
            turretShading = true;
            mirror = true;
            moveY = 2;
            moveX = 4;
            moveRot = -24;
            heatColor = SglDrawConst.frost;
            progress = PartProgress.warmup;
            heatProgress = PartProgress.warmup.delay(0.5f);

            moves.add(new PartMove(PartProgress.recoil, 0, -1, -6));
          }},
          new CustomPart(){{
            y = 4;
            progress = PartProgress.warmup;
            draw = (x, y, r, p) -> {
              SglDraw.gradientTri(x, y, 40 + 260*p, 60*p, r, SglDrawConst.frost, 0);
              SglDraw.gradientTri(x, y, 40*p, 60*p, r + 180, SglDrawConst.frost, 0);
            };
          }},
          new RegionPart("_body"){{
            heatColor = SglDrawConst.frost;
            heatProgress = PartProgress.warmup.delay(0.5f);
          }},
          new CustomPart(){{
            mirror = true;
            x = 16;
            y = 16;
            rotation = -12;

            layer = Layer.effect;
            draw = (x, y, r, p) -> {
              SglDraw.gradientTri(x, y, 8 + 32*p, 6*p, r, SglDrawConst.frost, 0);
              SglDraw.drawDiamond(x, y, 8 + 16*p, 6*p, r, SglDrawConst.frost);
            };
            progress = PartProgress.warmup.delay(0.5f);

            moves.add(new PartMove(PartProgress.recoil, 0, -1, -8));
          }},
          new CustomPart(){{
            mirror = true;
            x = 30;
            y = 4;
            rotation = -45;

            layer = Layer.effect;
            draw = (x, y, r, p) -> {
              SglDraw.gradientTri(x, y, 12 + 36*p, 6*p, r, SglDrawConst.frost, 0);
              SglDraw.drawDiamond(x, y, 12 + 18*p, 6*p, r, SglDrawConst.frost);
            };
            progress = PartProgress.warmup.delay(0.5f);

            moves.add(new PartMove(PartProgress.recoil, 2, -1.5f, -9));
          }},
          new HaloPart(){{
            color = SglDrawConst.frost;
            tri = true;
            y = -12;
            radius = 0;
            radiusTo = 8;
            triLength = 8;
            triLengthTo = 18;
            haloRadius = 0;
            shapes = 2;
            layer = Layer.effect;
            progress = PartProgress.warmup;
          }},
          new ShapePart(){{
            circle = true;
            color = Color.white;
            y = 24;
            radius = 0;
            radiusTo = 6;
            layer = Layer.effect;
            progress = PartProgress.warmup;
          }},
          new ShapePart(){{
            circle = true;
            color = SglDrawConst.frost;
            y = 24;
            radius = 0;
            radiusTo = 6;
            hollow = true;
            stroke = 0;
            strokeTo = 2.5f;
            layer = Layer.effect;
            progress = PartProgress.warmup;
          }},
          new CustomPart(){{
            y = -12;
            layer = Layer.effect;
            progress = PartProgress.warmup;
            rotation = 90;

            draw = (x, y, r, p) -> {
              SglDraw.drawDiamond(x, y, 20 + 76*p, 32*p, r, SglDrawConst.frost, 0);
            };
          }}
      );
    }};

    winter = new SglTurret("winter"){{
      requirements(Category.turret, ItemStack.with(
          SglItems.strengthening_alloy, 210,
          SglItems.degenerate_neutron_polymer, 80,
          Items.phaseFabric, 180,
          SglItems.iridium, 100,
          SglItems.aerogel, 200,
          SglItems.aluminium, 220,
          SglItems.matrix_alloy, 160,
          SglItems.crystal_FEX_power, 180
      ));
      size = 6;
      scaledHealth = 410;
      recoil = 3.6f;
      rotateSpeed = 1.75f;
      warmupSpeed = 0.015f;
      shake = 6;
      fireWarmupThreshold = 0.925f;
      linearWarmup = false;
      range = 560;
      targetGround = true;
      targetAir = true;
      shootEffect = new MultiEffect(
          SglFx.winterShooting,
          SglFx.shootRecoilWave
      );
      moveWhileCharging = true;
      shootY = 4;

      squareSprite = false;

      shoot.firstShotDelay = 120;
      chargeSound = Sounds.lasercharge;
      chargeSoundPitch = 0.9f;

      shootSound = Sounds.plasmaboom;
      shootSoundPitch = 0.6f;
      shootSoundVolume = 2;

      soundPitchRange = 0.05f;

      newAmmo(new BulletType(){
        {
          lifetime = 20;
          speed = 28;
          collides = false;
          absorbable = false;
          scaleLife = true;
          drawSize = 80;
          fragBullet = new BulletType(){
            {
              lifetime = 120;
              speed = 0.6f;
              collides = false;
              hittable = true;
              absorbable = false;
              despawnHit = true;
              splashDamage = 2180;
              splashDamageRadius = 84;
              hitShake = 12;

              trailEffect = Fx.disperseTrail;
              trailLength = 18;
              trailWidth = 7;
              trailColor = SglDrawConst.winter;

              hitEffect = SglFx.iceExplode;
              hitColor = SglDrawConst.winter;

              hitSound = Sounds.release;
              hitSoundPitch = 0.6f;
              hitSoundVolume = 2.5f;

              fragBullet = freezingField;
              fragOnHit = false;
              fragBullets = 1;
              fragVelocityMin = 0;
              fragVelocityMax = 0;
            }

            @Override
            public void draw(Bullet b){
              super.draw(b);
              Draw.color(SglDrawConst.winter);

              SglDraw.startBloom(Layer.flyingUnit + 1);
              float rot = b.fin(Interp.pow2Out)*3600;
              SglDraw.drawCrystal(b.x, b.y, 30, 14, 8, 0, 0, 0.8f,
                  Layer.effect, Layer.bullet, rot, b.rotation(), SglDrawConst.frost, SglDrawConst.winter);

              Draw.alpha(1);
              Fill.circle(b.x, b.y, 18*b.fin(Interp.pow3In));
              SglDraw.endBloom();
            }

            @Override
            public void update(Bullet b) {
              super.update(b);
              control.sound.loop(Sounds.spellLoop, b, 2);
            }
          };
          fragBullets = 1;
          fragSpread = 0;
          fragRandomSpread = 0;
          fragAngle = 0;
          fragOnHit = false;
          hitColor = SglDrawConst.winter;

          hitEffect = Fx.none;
          despawnEffect = Fx.none;
          smokeEffect = Fx.none;

          trailEffect = Fx.disperseTrail;
          trailLength = 75;
          trailWidth = 7;
          trailColor = SglDrawConst.winter;

          chargeEffect = SglFx.shrinkIceParticleSmall;
        }

        @Override
        public void draw(Bullet b){
          super.draw(b);
          Draw.z(Layer.bullet);
          Draw.color(SglDrawConst.winter);
          float rot = b.fin()*3600;

          SglDraw.drawCrystal(b.x, b.y, 30, 14, 8, 0, 0, 0.8f,
              Layer.effect, Layer.bullet, rot, b.rotation(), SglDrawConst.frost, SglDrawConst.winter);
        }
      }, true, (bt, ammo) -> {
        bt.add(Core.bundle.format("bullet.splashdamage", (int)ammo.fragBullet.splashDamage , Strings.fixed(ammo.fragBullet.splashDamageRadius/tilesize, 1)));
        bt.row();
        bt.add(Core.bundle.get("infos.winterAmmo"));
      });
      consume.time(720);
      consume.energy(1.1f);
      consume.liquids(UncLiquidStack.with(
          SglLiquids.phase_FEX_liquid, 0.2f,
          Liquids.cryofluid, 0.2f
      ));

      updating = e -> {
        SglTurretBuild t = (SglTurretBuild) e;
        if(Mathf.chanceDelta(0.06f*t.warmup)){
          Tmp.v1.set(36, 0).setAngle(t.rotation + (Mathf.randomBoolean()? 90: -90)).rotate(Mathf.random(-30, 30));
          SglFx.iceParticle.at(e.x + Tmp.v1.x, e.y + Tmp.v1.y, Tmp.v1.angle(), SglDrawConst.frost);
        }
      };

      draw = new DrawSglTurret(
          new CustomPart(){{
            progress = PartProgress.warmup;
            draw = (x, y, r, p) -> {
              Draw.color(SglDrawConst.winter);
              SglDraw.gradientTri(x, y, 70 + 120*p, 92*p, r, 0);
              SglDraw.gradientTri(x, y, 40 + 68*p, 92*p, r + 180, 0);
              Draw.color();
            };
          }},
          new RegionPart("_blade"){{
            mirror = true;
            turretShading = true;
            heatColor = SglDrawConst.winter;
            heatProgress = PartProgress.warmup.delay(0.3f);
            moveX = 4;
            moveY = 4;
            progress = PartProgress.warmup;

            moves.add(new PartMove(PartProgress.recoil, 0, -2, 0));
          }},
          new RegionPart("_side"){{
            mirror = true;
            turretShading = true;
            heatColor = SglDrawConst.winter;
            heatProgress = PartProgress.warmup.delay(0.3f);
            moveX = 8;
            moveRot = -20;
            progress = PartProgress.warmup;

            moves.add(new PartMove(PartProgress.recoil, 0, -2, -5));
          }},
          new RegionPart("_bot"){{
            mirror = true;
            turretShading = true;
            heatColor = SglDrawConst.winter;
            heatProgress = PartProgress.warmup.delay(0.3f);
            moveX = 4;
            progress = PartProgress.warmup;

            moves.add(new PartMove(PartProgress.recoil, 0, -2, 0));
          }},
          new RegionPart("_body"){{
            heatColor = SglDrawConst.winter;
            heatProgress = PartProgress.warmup.delay(0.3f);
          }},
          new CustomPart(){{
            mirror = true;
            x = 20;
            drawRadius = 0;
            drawRadiusTo = 20;
            rotation = -30;
            layer = Layer.effect;
            progress = PartProgress.warmup;
            draw = (x, y, r, p) -> {
              SglDraw.drawCrystal(x, y, 8 + 8*p, 6*p, 4*p, 0, 0, 0.4f*p,
                  Layer.effect, Layer.bullet - 1, Time.time*1.24f, r, Tmp.c1.set(SglDrawConst.frost).a(0.65f), SglDrawConst.winter);
            };
          }},
          new CustomPart(){{
            mirror = true;
            x = 20;
            drawRadius = 0;
            drawRadiusTo = 28;
            rotation = -65;
            layer = Layer.effect;
            progress = PartProgress.warmup.delay(0.15f);
            draw = (x, y, r, p) -> {
              SglDraw.drawCrystal(x, y, 16 + 21*p, 12*p, 8*p, 0, 0, 0.7f*p,
                  Layer.effect, Layer.bullet - 1, Time.time*1.24f + 45, r, Tmp.c1.set(SglDrawConst.frost).a(0.65f), SglDrawConst.winter);
            };
          }},
          new CustomPart(){{
            mirror = true;
            x = 20;
            drawRadius = 0;
            drawRadiusTo = 24;
            rotation = -105;
            layer = Layer.effect;
            progress = PartProgress.warmup.delay(0.3f);
            draw = (x, y, r, p) -> {
              SglDraw.drawCrystal(x, y, 12 + 14*p, 10*p, 6*p, 0, 0, 0.6f*p,
                  Layer.effect, Layer.bullet - 1, Time.time*1.24f + 90, r, Tmp.c1.set(SglDrawConst.frost).a(0.65f), SglDrawConst.winter);
            };
          }},
          new CustomPart(){{
            mirror = true;
            x = 20;
            drawRadius = 0;
            drawRadiusTo = 20;
            rotation = -135;
            layer = Layer.effect;
            progress = PartProgress.warmup.delay(0.45f);
            draw = (x, y, r, p) -> {
              SglDraw.drawCrystal(x, y, 9 + 12*p, 8*p, 5*p, 0, 0, 0.65f*p,
                  Layer.effect, Layer.bullet - 1, Time.time*1.24f + 135, r, Tmp.c1.set(SglDrawConst.frost).a(0.65f), SglDrawConst.winter);
            };
          }},
          new CustomPart(){{
            progress = PartProgress.charge;
            y = 4;
            layer = Layer.effect;
            draw = (x, y, r, p) -> {
              Draw.color(SglDrawConst.winter);
              Drawf.tri(x, y, 10*p, 12*p, r);
              Drawf.tri(x, y, 10*p, 8*p, r + 180);
              Draw.color(SglDrawConst.frost);
              SglDraw.gradientCircle(x, y, 4 + 12*p, -7*p, 0);
            };
          }}
      );
    }};

    summer = new SglTurret("summer"){{
      requirements(Category.turret, ItemStack.with(
          SglItems.strengthening_alloy, 210,
          SglItems.degenerate_neutron_polymer, 80,
          Items.phaseFabric, 180,
          SglItems.iridium, 120,
          SglItems.aerogel, 240,
          SglItems.matrix_alloy, 140,
          SglItems.crystal_FEX_power, 150,
          SglItems.crystal_FEX, 100
      ));
      size = 6;
      accurateDelay = false;
      scaledHealth = 410;
      recoil = 2f;
      recoilTime = 120;
      rotateSpeed = 2f;
      shootCone = 45;
      warmupSpeed = 0.025f;
      fireWarmupThreshold = 0.85f;
      linearWarmup = false;
      range = 500;
      targetGround = true;
      targetAir = true;
      shootY = 12;
      shake = 2;

      squareSprite = false;

      shootSound = Sounds.release;
      shootSoundPitch = 2;

      shoot = new ShootPattern(){
        @Override
        public void shoot(int totalShots, BulletHandler handler) {
          for (int i = 0; i < shots; i+=2) {
            for (int sign : Mathf.signs) {
              Tmp.v1.set(sign, 1).setLength(Mathf.random(2.5f)).scl(Mathf.randomBoolean()? 1: -1);
              handler.shoot(12*sign + Tmp.v1.x, Tmp.v1.y, -45*sign + Mathf.random(-20, 20), i/2f*shotDelay, b -> {
                if(b.owner instanceof SglTurretBuild turr && turr.wasShooting()){
                  b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(turr.targetPos), b.type.homingPower * Time.delta * 50f));
                }
              });
            }
          }
        }
      };
      shoot.shots = 12;
      shoot.shotDelay = 5f;

      newAmmo(new BulletType(){
        {
          speed = 4.5f;
          lifetime = 180;
          damage = 65;
          hitSize = 2;
          homingPower = 0.06f;
          trailEffect = Fx.disperseTrail;
          trailColor = Pal.lightishOrange.cpy().a(0.7f);
          hitColor = Pal.lightishOrange;
          shootEffect = Fx.shootSmallColor;
          smokeEffect = Fx.none;
          despawnEffect = Fx.none;
          despawnHit = false;
          trailWidth = 2;
          trailLength = 26;

          hitSound = Sounds.spark;
          hitSoundPitch = 2;
          hitSoundVolume = 1.6f;

          status = OtherContents.meltdown;
          statusDuration = 18;
        }

        @Override
        public void draw(Bullet b) {
          super.draw(b);
          Draw.z(Layer.bullet);
          Draw.color(Pal.lighterOrange);
          SglDraw.drawLightEdge(b.x, b.y, 35 + Mathf.absin(0.5f, 3.5f), 2, 14 + Mathf.absin(0.4f, 2.5f), 2, 30, Pal.lightishOrange);
          SglDraw.drawDiamond(b.x, b.y, 16 + Mathf.absin(0.6f, 2f), 2, 90, Pal.lightishOrange);
          Fill.circle(b.x, b.y, 2.2f);
        }

        @Override
        public void hitEntity(Bullet b, Hitboxc entity, float health) {
          if(entity instanceof Healthc h){
            if(pierceArmor){
              h.damagePierce(b.damage);
            }else{
              h.damage(b.damage);
            }
          }

          if(entity instanceof Unit unit){
            unit.damagePierce(Math.min(unit.getDuration(status)/2, 90));
            unit.apply(status, statusDuration + unit.getDuration(status));
          }
        }
      }, true, (t, b) -> {
        t.add(Core.bundle.format("bullet.damage", (int)b.damage));
        t.row();
        t.add(Core.bundle.get("infos.summerAmmo"));
      });
      consume.energy(5);
      consume.time(60);

      draw = new DrawSglTurret(
          new RegionPart("_side"){{
            mirror = true;
            turretShading = true;
            moveX = 4;
            progress = PartProgress.warmup;
            heatColor = Pal.lightishOrange;
            heatProgress = PartProgress.warmup.delay(0.25f);
          }},
          new RegionPart("_bot"){{
            mirror = true;
            turretShading = true;
            moveY = -4;
            moveX = 2;
            progress = PartProgress.warmup;
            heatColor = Pal.lightishOrange;
            heatProgress = PartProgress.warmup.delay(0.25f);
          }},
          new RegionPart("_body"){{
            heatColor = Pal.lightishOrange;
            heatProgress = PartProgress.warmup.delay(0.25f);
          }},
          new RegionPart("_blade"){{
            mirror = true;
            turretShading = true;
            moveX = 2;
            moveY = 8;
            moveRot = -45;
            progress = PartProgress.warmup;
            heatColor = Pal.lightishOrange;
            heatProgress = PartProgress.warmup.delay(0.25f);
          }},
          new ShapePart(){{
            color = Pal.lighterOrange;
            circle = true;
            hollow = true;
            stroke = 0;
            strokeTo = 2f;
            y = -18;
            radius = 0;
            radiusTo = 12f;
            progress = PartProgress.warmup;
            layer = Layer.effect;
          }},
          new ShapePart(){{
            circle = true;
            y = -18;
            radius = 0;
            radiusTo = 3.5f;
            color = Pal.lighterOrange;
            layer = Layer.effect;
            progress = PartProgress.warmup;
          }},
          new HaloPart(){{
            progress = PartProgress.warmup;
            color = Pal.lighterOrange;
            layer = Layer.effect;
            y = -18;
            haloRotation = 90f;
            shapes = 2;
            triLength = 0f;
            triLengthTo = 32f;
            haloRadius = 0;
            haloRadiusTo = 12f;
            tri = true;
            radius = 2;
            radiusTo = 5;
          }},
          new HaloPart(){{
            progress = PartProgress.warmup;
            color = Pal.lighterOrange;
            layer = Layer.effect;
            y = -18;
            haloRotation = 90f;
            shapes = 2;
            triLength = 0f;
            triLengthTo = 8f;
            haloRadius = 0;
            haloRadiusTo = 12f;
            tri = true;
            radius = 2;
            radiusTo = 5;
            shapeRotation = 180f;
          }},
          new HaloPart(){{
            progress = PartProgress.warmup;
            color = Pal.lighterOrange;
            layer = Layer.effect;
            y = -18;
            haloRotation = 0f;
            haloRotateSpeed = 1;
            shapes = 2;
            triLength = 0f;
            triLengthTo = 10f;
            haloRadius = 16f;
            tri = true;
            radius = 6f;
          }},
          new HaloPart(){{
            progress = PartProgress.warmup;
            color = Pal.lighterOrange;
            layer = Layer.effect;
            y = -18;
            haloRotation = 0f;
            haloRotateSpeed = 1;
            shapes = 2;
            triLength = 0f;
            triLengthTo = 6f;
            haloRadius = 16f;
            tri = true;
            radius = 6f;
            shapeRotation = 180f;
          }},
          new HaloPart(){{
            progress = PartProgress.warmup;
            color = Pal.lighterOrange;
            layer = Layer.effect;
            y = -18;
            haloRotation = 0f;
            haloRotateSpeed = -1;
            shapes = 4;
            triLength = 0f;
            triLengthTo = 4f;
            haloRadius = 12f;
            tri = true;
            radius = 5f;
          }},
          new HaloPart(){{
            progress = PartProgress.warmup;
            color = Pal.lighterOrange;
            layer = Layer.effect;
            y = -18;
            haloRotation = 0f;
            haloRotateSpeed = -1;
            shapes = 4;
            triLength = 0f;
            triLengthTo = 6f;
            haloRadius = 12f;
            tri = true;
            radius = 5f;
            shapeRotation = 180f;
          }}
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

        hitColor = color;
        hitEffect = Fx.hitLancer;
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
        b.data = container = Pools.obtain(LightningContainer.PoolLightningContainer.class, LightningContainer.PoolLightningContainer::new);
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

        container.generator.blockNow = (last, vertex) -> {
          Building abs = Damage.findAbsorber(b.team, b.x + last.x, b.y + last.y, b.x + vertex.x, b.y + vertex.y);
          if(abs == null) return -1;
          float ox = b.x + last.x, oy = b.y + last.y;
          return Mathf.len(abs.x - ox, abs.y - oy);
        };
        container.create();
      }

      @Override
      public void removed(Bullet b){
        super.removed(b);
        if(b.data instanceof LightningContainer.PoolLightningContainer c){
          Pools.free(c);
        }
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
