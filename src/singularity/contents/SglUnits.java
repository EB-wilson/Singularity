package singularity.contents;

import arc.func.Func2;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Units;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.part.DrawPart;
import mindustry.entities.part.HaloPart;
import mindustry.entities.part.RegionPart;
import mindustry.entities.pattern.ShootBarrel;
import mindustry.entities.pattern.ShootPattern;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.graphics.Trail;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.weapons.PointDefenseWeapon;
import mindustry.world.Block;
import mindustry.world.meta.BlockFlag;
import singularity.Sgl;
import singularity.graphic.MathRenderer;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.util.MathTransform;
import singularity.world.SglFx;
import singularity.world.blocks.product.PayloadCrafter;
import singularity.world.blocks.turrets.EmpMultiTrailBulletType;
import singularity.world.blocks.turrets.MultiTrailBulletType;
import singularity.world.draw.part.CustomPart;
import singularity.world.unit.*;
import universecore.util.handler.ObjectHandler;

import java.util.Iterator;

import static mindustry.Vars.headless;

public class SglUnits implements ContentList{
  public static final BulletType LIGHTEDGE_FRAG_BULLET = new BulletType() {
    {
      damage = 80;
      splashDamage = 80;
      splashDamageRadius = 24;
      speed = 4;
      hitSize = 3;
      lifetime = 120;
      despawnHit = true;
      hitEffect = SglFx.diamondSpark;
      hitColor = SglDrawConst.matrixNet;

      collidesTiles = false;

      homingRange = 160;
      homingPower = 0.075f;

      trailColor = SglDrawConst.matrixNet;
      trailLength = 25;
      trailWidth = 3f;
    }

    @Override
    public void draw(Bullet b) {
      super.draw(b);
      SglDraw.drawDiamond(b.x, b.y, 10, 4, b.rotation());
    }

    @Override
    public void update(Bullet b) {
      super.update(b);

      b.vel.lerpDelta(Vec2.ZERO, 0.04f);
    }
  };
  public static final String EPHEMERAS = "ephemeras";
  public static final String TIMER = "timer";
  /**棱镜*/
  public static UnitType prism,
  /**光弧*/
  lightarc,
  /**黎明*/
  dawn;

  /**辉夜*/
  @UnitEntityType(UnitEntity.class)
  public static UnitType kaguya;

  /**极光*/
  @UnitEntityType(AirSeaAmphibiousUnit.AirSeaUnit.class)
  public static UnitType aurora,
  /**晨星*/
  mornstar;

  /**机械构造坞*/
  public static Block machine_construct_dock;

  @Override
  public void load() {
    UnitTypeRegister.registerAll();

    mornstar = new AirSeaAmphibiousUnit("mornstar"){{
      speed = 1f;
      accel = 0.065f;
      drag = 0.03f;
      rotateSpeed = 1.8f;
      riseSpeed = 0.02f;
      boostMultiplier = 1.25f;
      faceTarget = true;
      health = 42500;
      lowAltitude = true;
      hitSize = 64;
      targetFlags = BlockFlag.allLogic;

      engineOffset = 0;
      engineSize = 0;

      setEnginesMirror(
          new UnitEngine(){{
            x = 16f;
            y = -44f;
            radius = 10;
            rotation = 45;
          }},
          new UnitEngine(){{
            x = 24f;
            y = -52f;
            radius = 6;
            rotation = 45;
          }},
          new UnitEngine(){{
            x = 34f;
            y = -52f;
            radius = 8;
            rotation = -45;
          }}
      );

      weapons.addAll(
          new Weapon(Sgl.modName + "-mornstar_cannon"){{
            recoil = 0;
            recoilTime = 120;
            cooldownTime = 120;

            reload = 90;
            rotate = true;
            mirror = false;

            rotateSpeed = 2.5f;

            layerOffset = 1;

            x = 0;
            y = 4;
            shootY = 25;

            shoot = new ShootBarrel(){{
              barrels = new float[]{
                  5.75f, 0, 0,
                  -5.75f, 0, 0
              };
              shots = 2;
              shotDelay = 0;
            }};

            bullet = new EmpMultiTrailBulletType(){
              {
                hitColor = trailColor = SglDrawConst.matrixNet;
                trailLength = 22;
                trailWidth = 2f;
                trailEffect = new MultiEffect(
                    SglFx.trailLineLong,
                    SglFx.railShootRecoil,
                    SglFx.movingCrystalFrag
                );
                trailRotation = true;
                trailChance = 1;

                shootEffect = new MultiEffect(
                    SglFx.shootRecoilWave,
                    SglFx.shootRail
                );
                hitEffect = SglFx.lightConeHit;
                despawnEffect = new MultiEffect(
                    SglFx.impactWaveSmall,
                    SglFx.spreadSparkLarge,
                    SglFx.diamondSparkLarge
                );
                smokeEffect = Fx.shootSmokeSmite;

                shootSound = Sounds.malignShoot;

                damage = 500;
                empDamage = 100;
                lifetime = 45;
                speed = 8;
                pierceCap = 4;
                hittable = false;

                fragBullet = LIGHTEDGE_FRAG_BULLET;
                fragBullets = 3;
                fragRandomSpread = 115;
              }

              @Override
              public void draw(Bullet b) {
                super.draw(b);
                Draw.color(SglDrawConst.matrixNet);
                SglDraw.gapTri(b.x, b.y, 18, 28, 16, b.rotation());
                SglDraw.drawTransform(b.x, b.y, -6, 0, b.rotation(), (x, y, r) -> {
                  SglDraw.drawDiamond(x, y, 16, 8, r);
                });
              }

              @Override
              public void hit(Bullet b) {
                super.hit(b);
                b.damage -= 125;
              }

              @Override
              public void init(Bullet b) {
                super.init(b);
                b.data = new TrailMoveLightning();
              }

              @Override
              public void updateTrail(Bullet b) {
                if(!headless && trailLength > 0){
                  if(b.trail == null){
                    b.trail = new Trail(trailLength);
                  }
                  b.trail.length = trailLength;

                  if (!(b.data instanceof TrailMoveLightning m)) return;
                  m.update();
                  SglDraw.drawTransform(b.x, b.y, 0, m.off, b.rotation(), (x, y, r) -> b.trail.update(x, y));
                }
              }

              class TrailMoveLightning{
                float off;
                float offDelta;

                {
                  flushDelta(0);
                }

                private void flushDelta(int i) {
                  offDelta = Mathf.random(i <= 0? -4: 0, i >= 0? 4: 0);
                }

                public void update() {
                  if (Mathf.chanceDelta(0.3f) || off >= 4 || off <= -4) flushDelta(off >= 4? -1: off <= 4? 1: 0);
                  off += offDelta*Time.delta;
                }
              }
            };

            parts.addAll(
                new RegionPart("_blade"){{
                  under = true;
                  progress = PartProgress.recoil;
                  moveY = -3;
                  heatColor = Pal.turretHeat;
                  heatProgress = PartProgress.heat;
                }},
                new RegionPart("_body"){{
                  under = true;
                }}
            );
          }},
          new Weapon(Sgl.modName + "-mornstar_turret"){{
            x = 26;
            y = -28;
            shootY = 0;
            recoil = 5;
            recoilTime = 60;
            reload = 8;

            rotate = true;
            rotateSpeed = 8;

            bullet = new BulletType(){
              {
                speed = 10;
                lifetime = 30;
                damage = 120;

                despawnHit = true;

                hitColor = trailColor = SglDrawConst.matrixNet;
                hitEffect = new MultiEffect(
                    SglFx.spreadDiamondSmall,
                    SglFx.movingCrystalFrag
                );
                smokeEffect = Fx.colorSpark;
                shootEffect = new MultiEffect(
                    SglFx.railShootRecoil,
                    SglFx.crossLightMini
                );
                trailWidth = 3;
                trailLength = 8;

                trailEffect = SglFx.glowParticle;
                trailChance = 0.1f;
              }

              @Override
              public void draw(Bullet b) {
                super.draw(b);
                Draw.color(SglDrawConst.matrixNet);
                Tmp.v1.set(1, 0).setAngle(b.rotation());
                SglDraw.gapTri(b.x + Tmp.v1.x*3*b.fout(), b.y + Tmp.v1.y*3*b.fout(), 15, 22, 14, b.rotation());
                SglDraw.gapTri(b.x - Tmp.v1.x*3*b.fout(), b.y - Tmp.v1.y*3*b.fout(), 12, 18, 10, b.rotation());
                SglDraw.gapTri(b.x - Tmp.v1.x*5*b.fout(), b.y - Tmp.v1.y*5*b.fout(), 9, 12, 8, b.rotation());
                SglDraw.drawDiamond(b.x, b.y, 18, 6, b.rotation());
              }

              @Override
              public void update(Bullet b) {
                super.update(b);
                b.damage = (b.type.damage + b.type.damage*b.fout())*0.5f;
              }
            };
          }},
          new RelatedWeapon(){
            {
              useAlternative = Flyingc::isFlying;
              mirror = false;
              x = 0;
              y = -25;
              shootCone = 180;

              recoil = 0;
              recoilTime = 1;

              linearWarmup = false;
              shootWarmupSpeed = 0.025f;
              minWarmup = 0.9f;

              reload = 60;

              bullet = new ContinuousBulletType(){{
                speed = 0;
                lifetime = 180;
                length = 360;
              }};

              alternativeBullet = new BulletType(){{
                speed = 8;
                lifetime = 240;
                rangeOverride = 360;
              }};
            }

            @Override
            public void init(DataWeaponMount mount) {
              super.init(mount);
              mount.setVar(EPHEMERAS, new Seq<>(Ephemera.class));
              mount.setVar(TIMER, new Interval(3));
            }

            @Override
            protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation) {
              DataWeaponMount m = (DataWeaponMount) mount;
              Seq<Ephemera> seq = m.getVar(EPHEMERAS);
              Log.info("shoot");
              for (Ephemera ephemera : seq) {
                if (ephemera.alpha > 0.9f) {
                  ephemera.shoot(unit, useAlternative.get(unit) ? bullet : alternativeBullet);
                  ephemera.removed = true;
                  break;
                }
              }
            }

            @Override
            public void draw(Unit unit, DataWeaponMount mount) {
              super.draw(unit, mount);
              Draw.z(Layer.effect);
              SglDraw.drawTransform(unit.x, unit.y, mount.weapon.x, mount.weapon.y, unit.rotation - 90, (x, y, r) -> {
                Draw.color(SglDrawConst.matrixNet);
                Fill.circle(x, y, 6);
                Lines.stroke(0.8f);
                SglDraw.dashCircle(x, y, 8, 4, 180, Time.time);
                Draw.alpha(0.65f);
                SglDraw.gradientCircle(x, y, 16, 10, 0);

                Draw.alpha(1);
                SglDraw.drawDiamond(x, y, 20 + 14*mount.warmup, 2 + 3*mount.warmup, Time.time*1.2f);
                SglDraw.drawDiamond(x, y, 26 + 14*mount.warmup, 3 + 4*mount.warmup, -Time.time*1.2f);

                for (Ephemera ephemera : mount.<Seq<Ephemera>>getVar(EPHEMERAS)) {
                  Draw.color(SglDrawConst.matrixNet);

                  if (!ephemera.removed) {
                    Fill.circle(ephemera.x, ephemera.y, 4);
                    Lines.stroke(0.8f);
                    Lines.circle(ephemera.x, ephemera.y, 6);
                  }

                  for (int i = 0; i < 3; i++) {
                    Tmp.v1.set(16, 0).setAngle(Time.time + i*120);
                    float sin = Mathf.absin((Time.time*4 + i*120)*Mathf.degRad, 0.5f, 1);
                    sin = Math.max(sin, mount.warmup)*ephemera.alpha;
                    float w = sin*sin*sin*4;
                    SglDraw.drawDiamond(ephemera.x + Tmp.v1.x, ephemera.y + Tmp.v1.y, 20, w, Time.time + i*120);
                    SglDraw.drawDiamond(ephemera.x - Tmp.v1.x, ephemera.y - Tmp.v1.y, 20, w, Time.time + i*120);
                  }

                  ephemera.trail.draw(SglDrawConst.matrixNet, 4.5f);
                }
              });
            }

            @Override
            public void update(Unit unit, DataWeaponMount mount) {
              Tmp.v1.set(mount.weapon.x, mount.weapon.y).rotate(unit.rotation - 90);
              float mx = unit.x + Tmp.v1.x;
              float my = unit.y + Tmp.v1.y;
              Seq<Ephemera> seq = mount.getVar(EPHEMERAS);
              if (mount.warmup <= 0.1f){
                if (seq.size < 4){
                  if (mount.<Interval>getVar(TIMER).get(0, 240)) {
                    mount.totalShots++;

                    Ephemera ephemera = Pools.obtain(Ephemera.class, Ephemera::new);
                    ephemera.x = mx;
                    ephemera.y = my;
                    ephemera.move = Mathf.random(0.02f, 0.04f);
                    ephemera.angelOff = Mathf.random(15, 45) * (mount.totalShots%2 == 0 ? 1 : -1);
                    ephemera.bestDst = Mathf.random(18, 36);
                    ephemera.trail.clear();
                    ephemera.vel.rnd(Mathf.random(0.6f, 2));

                    seq.add(ephemera);
                  }
                }
              }

              if (seq.isEmpty()){
                mount.reload = mount.weapon.reload;
              }

              for (Iterator<Ephemera> iterator = seq.iterator(); iterator.hasNext(); ) {
                Ephemera ephemera = iterator.next();
                ephemera.alpha = Mathf.lerpDelta(ephemera.alpha, ephemera.removed? 0: 1, 0.02f);
                if (ephemera.removed && ephemera.alpha <= 0.1f){
                  iterator.remove();
                  Pools.free(ephemera);
                }

                ephemera.x += ephemera.vel.x * Time.delta;
                ephemera.y += ephemera.vel.y * Time.delta;
                ephemera.trail.update(ephemera.x, ephemera.y);

                float dst = (ephemera.bestDst + ephemera.bestDst * mount.warmup) - Mathf.dst(ephemera.x - mx, ephemera.y - my);
                float speed = dst / 30;

                ephemera.vel.lerpDelta(Tmp.v1.set(ephemera.x - unit.x, ephemera.y - unit.y).setLength2(1).scl(speed).rotate(ephemera.angelOff), 0.12f);
                Tmp.v1.set(ephemera.move + ephemera.move * mount.warmup, 0).rotate(Time.time);
                ephemera.vel.add(Tmp.v1);
              }
            }

            class Ephemera implements Pool.Poolable {
              float x, y, angelOff, move;
              float bestDst, alpha;
              boolean removed;
              final Vec2 vel = new Vec2();
              final Trail trail = new Trail(42);

              @Override
              public void reset() {
                x = y = 0;
                alpha = 0;
                removed = false;
                vel.setZero();
                bestDst = 0;
                move = 0;
                angelOff = 0;
              }

              public void shoot(Unit u, BulletType bullet) {
                Bullet b = bullet.create(u, x, y, vel.angle());
                if (bullet.speed > 0){
                  b.vel.set(vel);
                }

                Fx.trailFade.at(x, y, vel.angle(), SglDrawConst.matrixNet, trail);
              }
            }
          }
      );
      weapons.get(weapons.size - 1).flip();
    }};

    kaguya = new UnitType("kaguya"){
      {
        speed = 1.1f;
        accel = 0.06f;
        drag = 0.04f;
        rotateSpeed = 1.5f;
        faceTarget = true;
        flying = true;
        health = 45000;
        lowAltitude = true;
        //canBoost = true;
        //boostMultiplier = 2.5f;
        hitSize = 70;
        targetFlags = BlockFlag.all;

        engineSize = 0;

        Func2<Float, Float, Weapon> laser = (dx, dy) -> new Weapon(Sgl.modName + "-kaguya_laser"){{
          this.x = dx;
          this.y = dy;
          mirror = true;
          reload = 30;
          recoil = 4;
          recoilTime = 30;
          shadow = 4;
          rotate = true;
          layerOffset = 0.1f;
          shootSound = Sounds.laser;

          shake = 3;

          bullet = new LaserBulletType(){{
            damage = 165f;
            lifetime = 20;
            sideAngle = 90f;
            sideWidth = 1.25f;
            sideLength = 15f;
            width = 16f;
            length = 450f;
            shootEffect = Fx.colorSparkBig;
            colors = new Color[]{SglDrawConst.matrixNetDark, SglDrawConst.matrixNet, Color.white};
            hitColor = colors[0];
          }};
        }};

        weapons.addAll(
            laser.get(19.25f, 16f),
            laser.get(13.5f, 33.5f),
            new Weapon(Sgl.modName + "-kaguya_cannon"){
              {
                x = 30.5f;
                y = -3.5f;
                mirror = true;

                cooldownTime = 120;
                recoil = 0;
                recoilTime = 120;
                reload = 90;
                shootX = 2;
                shootY = 22;
                rotate = true;
                rotationLimit = 30;
                rotateSpeed = 10;

                shake = 5;

                layerOffset = 0.1f;

                shootSound = Sounds.shockBlast;

                shoot.shots = 3;
                shoot.shotDelay = 10;

                parts.addAll(
                    new RegionPart("_shooter"){{
                      heatColor = SglDrawConst.matrixNet;
                      heatProgress = PartProgress.heat;
                      moveY = -6;
                      progress = PartProgress.recoil;
                    }},
                    new RegionPart("_body")
                );

                bullet = new MultiTrailBulletType(){
                  {
                    speed = 6;
                    lifetime = 75;
                    damage = 180;
                    splashDamage = 240;
                    splashDamageRadius = 36;

                    hitEffect = new MultiEffect(
                        Fx.shockwave,
                        Fx.bigShockwave,
                        SglFx.impactWaveSmall,
                        SglFx.spreadSparkLarge,
                        SglFx.diamondSparkLarge
                    );
                    despawnHit = true;

                    shootEffect = SglFx.railShootRecoil;
                    hitColor = SglDrawConst.matrixNet;
                    trailColor = SglDrawConst.matrixNet;
                    hitSize = 8;
                    trailLength = 36;
                    trailWidth = 4;

                    hitShake = 4;
                    hitSound = Sounds.dullExplosion;
                    hitSoundVolume = 3.5f;

                    trailEffect = SglFx.trailParticle;
                    trailChance = 0.5f;

                    fragBullet = LIGHTEDGE_FRAG_BULLET;
                    fragBullets = 4;
                    fragOnHit = true;
                    fragOnAbsorb = true;
                  }

                  @Override
                  public void draw(Bullet b){
                    super.draw(b);
                    Drawf.tri(b.x, b.y, 12, 30, b.rotation());
                    Drawf.tri(b.x, b.y, 12, 12, b.rotation() + 180);
                  }
                };
              }

              @Override
              public void flip(){
                super.flip();

                parts = new Seq<>(parts);
                for(int i = 0; i < parts.size; i++){
                  DrawPart part = parts.get(i);
                  if(part instanceof RegionPart p){
                    RegionPart n = new RegionPart(p.suffix);
                    ObjectHandler.copyField(p, n);
                    n.xScl *= -1;
                    parts.set(i, n);
                  }
                }
              }
            },
            new PointDefenseWeapon(Sgl.modName + "-kaguya_point_laser"){{
              x = 30.5f;
              y = -3.5f;
              mirror = true;

              recoil = 0;
              reload = 12;
              targetInterval = 0;
              targetSwitchInterval = 0;

              layerOffset = 0.2f;

              bullet = new BulletType(){{
                damage = 62;
                rangeOverride = 420;
              }};
            }},
            new DataWeapon(){
              {
                x = 0;
                y = -14;
                minWarmup = 0.98f;
                shootWarmupSpeed = 0.02f;
                linearWarmup = false;
                rotate = false;
                shootCone = 10;
                rotateSpeed = 10;
                shootY = 80;
                reload = 30;
                recoilTime = 60;
                recoil = 2;
                recoilPow = 0;
                targetSwitchInterval = 300;
                targetInterval = 0;

                mirror = false;
                continuous = true;
                alwaysContinuous = true;

                Weapon s = this;

                bullet = new PointLaserBulletType(){
                  {
                    damage = 240;
                    damageInterval = 5;
                    rangeOverride = 450;
                    shootEffect = SglFx.railShootRecoil;
                    hitColor = SglDrawConst.matrixNet;
                    hitEffect = SglFx.diamondSparkLarge;
                    shake = 5;
                  }

                  @Override
                  public float continuousDamage(){
                    return damage*(60/damageInterval);
                  }

                  @Override
                  public void update(Bullet b){
                    super.update(b);

                    if(b.owner instanceof Unit u){
                      for(WeaponMount mount: u.mounts){
                        if(mount.weapon == s){
                          float bulletX = u.x + Angles.trnsx(u.rotation - 90, x + shootX, y + shootY),
                              bulletY = u.y + Angles.trnsy(u.rotation - 90, x + shootX, y + shootY);

                          b.set(bulletX, bulletY);
                          Tmp.v2.set(mount.aimX - bulletX, mount.aimY - bulletY);
                          float angle = Mathf.clamp(Tmp.v2.angle() - u.rotation, -shootCone, shootCone);
                          Tmp.v2.setAngle(u.rotation).rotate(angle);

                          Tmp.v1.set(b.aimX - bulletX, b.aimY - bulletY).lerpDelta(Tmp.v2, 0.1f).clampLength(80, range);

                          b.aimX = bulletX + Tmp.v1.x;
                          b.aimY = bulletY + Tmp.v1.y;

                          shootEffect.at(bulletX, bulletY, Tmp.v1.angle(), hitColor);
                        }
                      }
                    }
                  }

                  @Override
                  public void draw(Bullet b){
                    super.draw(b);
                    Draw.draw(Draw.z(), () -> {
                      Draw.color(hitColor);
                      MathRenderer.setDispersion(0.1f);
                      MathRenderer.setThreshold(0.4f, 0.6f);

                      for(int i = 0; i < 3; i++){
                        MathRenderer.drawSin(b.x, b.y, b.aimX, b.aimY,
                            Mathf.randomSeed(b.id + i, 4f, 6f)*b.fslope(),
                            Mathf.randomSeed(b.id + i + 1, 360f, 720f),
                            Mathf.randomSeed(b.id + i + 2, 360f) - Time.time*Mathf.randomSeed(b.id + i + 3, 4f, 7f)
                        );
                      }
                    });
                  }
                };

                parts.addAll(
                    new CustomPart(){{
                      layer = Layer.effect;
                      progress = PartProgress.warmup;
                      draw = (x, y, r, p) -> {
                        Draw.color(SglDrawConst.matrixNet);
                        Fill.circle(x, y, 8);
                        Lines.stroke(1.4f);
                        SglDraw.dashCircle(x, y, 12, Time.time);
                        Lines.stroke(1f);
                        Lines.circle(x, y, 14);
                        Draw.alpha(0.65f);
                        SglDraw.gradientCircle(x, y, 20, 12, 0);

                        Draw.alpha(1);
                        SglDraw.drawDiamond(x, y, 24 + 18*p, 3 + 3*p, Time.time*1.2f);
                        SglDraw.drawDiamond(x, y, 30 + 18*p, 4 + 4*p, -Time.time*1.2f);
                      };
                    }}
                );
              }

              @Override
              public void init(DataWeaponMount mount){
                Shooter[] shooters = new Shooter[3];
                for(int i = 0; i < shooters.length; i++){
                  shooters[i] = new Shooter();
                }
                mount.setVar("shooters", shooters);
              }

              @Override
              public void update(Unit unit, DataWeaponMount mount){
                Shooter[] shooters = mount.getVar("shooters");
                for(Shooter shooter: shooters){
                  Vec2 v = MathTransform.fourierTransform(Time.time, shooter.param).scl(mount.warmup);
                  Tmp.v1.set(mount.weapon.x, mount.weapon.y).rotate(unit.rotation - 90);
                  shooter.x = Tmp.v1.x + v.x;
                  shooter.y = Tmp.v1.y + v.y;
                  shooter.trail.update(unit.x + shooter.x, unit.y + shooter.y);
                }
              }

              @Override
              protected void shoot(Unit unit, DataWeaponMount mount, float shootX, float shootY, float rotation){
                float mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
                    mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y);

                SglFx.shootRecoilWave.at(shootX, shootY, rotation, SglDrawConst.matrixNet);
                SglFx.impactWave.at(shootX, shootY, SglDrawConst.matrixNet);

                SglFx.impactWave.at(mountX, mountY, SglDrawConst.matrixNet);
                SglFx.crossLight.at(mountX, mountY, SglDrawConst.matrixNet);
                Shooter[] shooters = mount.getVar("shooters");
                for(Shooter shooter: shooters){
                  SglFx.impactWaveSmall.at(mountX + shooter.x, mountY + shooter.y);
                }
              }

              @Override
              public void draw(Unit unit, DataWeaponMount mount){
                Shooter[] shooters = mount.getVar("shooters");
                Draw.z(Layer.effect);

                float mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
                    mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y);

                float bulletX = mountX + Angles.trnsx(unit.rotation - 90, shootX, shootY),
                    bulletY = mountY + Angles.trnsy(unit.rotation - 90, shootX, shootY);

                Draw.color(SglDrawConst.matrixNet);
                Fill.circle(bulletX, bulletY, 6*mount.recoil);
                Draw.color(Color.white);
                Fill.circle(bulletX, bulletY, 3*mount.recoil);
                for(Shooter shooter: shooters){
                  Draw.color(SglDrawConst.matrixNet);
                  shooter.trail.draw(SglDrawConst.matrixNet, 3*mount.warmup);

                  float drawx = unit.x + shooter.x, drawy = unit.y + shooter.y;
                  Fill.circle(drawx, drawy, 4*mount.warmup);
                  Lines.stroke(0.65f*mount.warmup);
                  SglDraw.dashCircle(drawx, drawy, 6f*mount.warmup, 4, 180, Time.time);
                  SglDraw.drawDiamond(drawx, drawy, 4 + 8*mount.warmup, 3*mount.warmup, Time.time*1.45f);
                  SglDraw.drawDiamond(drawx, drawy, 8 + 10*mount.warmup, 3.6f*mount.warmup, -Time.time*1.45f);

                  Lines.stroke(3*mount.recoil, SglDrawConst.matrixNet);
                  Lines.line(drawx, drawy, bulletX, bulletY);
                  Lines.stroke(1.75f*mount.recoil, Color.white);
                  Lines.line(drawx, drawy, bulletX, bulletY);

                  Draw.alpha(0.5f);
                  Lines.line(mountX, mountY, drawx, drawy);
                }
              }

              class Shooter{
                final Trail trail = new Trail(45);
                final float[] param;

                float x, y;

                {
                  param = new float[9];
                  for(int d = 0; d < 3; d++){
                    param[d*3] = Mathf.random(0.5f, 3f)/(d + 1)*(Mathf.randomBoolean()? 1: -1);
                    param[d*3 + 1] = Mathf.random(0f, 360f);
                    param[d*3 + 2] = Mathf.random(18f, 48f)/((d + 1)*(d + 1));
                  }
                }
              }
            }
        );
      }
    };

    aurora = new AirSeaAmphibiousUnit("aurora"){
      {
        speed = 0.65f;
        accel = 0.06f;
        drag = 0.04f;
        rotateSpeed = 1.25f;
        riseSpeed = 0.02f;
        boostMultiplier = 1.2f;
        faceTarget = true;
        health = 52500;
        lowAltitude = true;
        hitSize = 75;
        targetFlags = BlockFlag.allLogic;

        engineOffset = 50;
        engineSize = 16;

        setEnginesMirror(
            new UnitEngine(){{
              x = 38f;
              y = -12;
              radius = 8;
              rotation = -45;
            }},
            new UnitEngine(){{
              x = 40f;
              y = -54;
              radius = 10;
              rotation = -45;
            }}
        );

        weapons.addAll(
            new Weapon(Sgl.modName + "-aurora_lightcone"){{
                shake = 5f;
                shootSound = Sounds.pulseBlast;
                x = 29;
                y = -30;
                shootY = 8;
                rotate = true;
                rotateSpeed = 3;
                recoil = 6;
                recoilTime = 60;
                cooldownTime = 60;
                reload = 60;
                shadow = 45;

                layerOffset = 1;

                bullet = new BulletType(){
                  {
                    trailLength = 36;
                    trailWidth = 3.25f;
                    trailColor = SglDrawConst.matrixNet;
                    trailRotation = true;
                    trailChance = 1;
                    hitSize = 8;
                    speed = 12;
                    lifetime = 40;
                    damage = 620;
                    range = 480;

                    homingRange = 30;
                    homingPower = 0.15f;

                    pierce = true;
                    hittable = false;
                    reflectable = false;
                    pierceArmor = true;
                    pierceBuilding = true;
                    absorbable = false;

                    trailEffect = new MultiEffect(
                        SglFx.lightConeTrail,
                        SglFx.lightCone
                    );
                    hitEffect = SglFx.lightConeHit;
                    hitColor = SglDrawConst.matrixNet;
                  }

                  @Override
                  public void draw(Bullet b) {
                    super.draw(b);
                    Draw.color(SglDrawConst.matrixNet);
                    Drawf.tri(b.x, b.y, 8, 18, b.rotation());
                    for(int i : Mathf.signs){
                      Drawf.tri(b.x, b.y, 8f, 26f, b.rotation() + 156f*i);
                    }
                  }

                  @Override
                  public void update(Bullet b) {
                    super.update(b);
                    Damage.damage(b.team, b.x, b.y, hitSize, damage*Time.delta);
                  }
                };
            }},
            new Weapon(Sgl.modName + "-aurora_turret"){{
              shake = 4f;
              shootSound = Sounds.laser;
              x = 22;
              y = 20;
              shootY = 6;
              rotate = true;
              rotateSpeed = 6;
              recoil = 4;
              recoilTime = 20;
              cooldownTime = 60;
              reload = 20;
              shadow = 25;

              bullet = new LaserBulletType(){{
                damage = 425f;
                lifetime = 24;
                sideAngle = 90f;
                sideWidth = 1.45f;
                sideLength = 20f;
                width = 24f;
                length = 480f;
                shootEffect = SglFx.shootRecoilWave;
                colors = new Color[]{SglDrawConst.matrixNetDark, SglDrawConst.matrixNet, Color.white};
                hitColor = colors[0];
              }};
            }},
            new RelatedWeapon(){
              {
                x = 0;
                y = -22;
                shootY = 0;
                reload = 600;
                mirror = false;
                rotateSpeed = 0;
                shootCone = 0.5f;
                rotate = true;
                shootSound = Sounds.laserblast;
                ejectEffect = SglFx.railShootRecoil;
                recoilTime = 30;
                shake = 4;

                minWarmup = 0.9f;
                shootWarmupSpeed = 0.03f;

                shoot.firstShotDelay = 80;

                alternativeShoot = new ShootPattern(){
                  @Override
                  public void shoot(int totalShots, BulletHandler handler) {
                    for (int i = 0; i < shots; i++) {
                      handler.shoot(0, 0, Mathf.random(0, 360f), firstShotDelay + i*shotDelay);
                    }
                  }
                };
                alternativeShoot.shots = 12;
                alternativeShoot.shotDelay = 3;
                alternativeShoot.firstShotDelay = 0;
                useAlternative = Flyingc::isFlying;
                parentizeEffects = true;

                parts.addAll(
                    new HaloPart(){{
                      progress = PartProgress.warmup;
                      color = SglDrawConst.matrixNet;
                      layer = Layer.effect;
                      haloRotateSpeed = -1;
                      shapes = 2;
                      triLength = 0f;
                      triLengthTo = 26f;
                      haloRadius = 0;
                      haloRadiusTo = 14f;
                      tri = true;
                      radius = 6;
                    }},
                    new HaloPart(){{
                      progress = PartProgress.warmup;
                      color = SglDrawConst.matrixNet;
                      layer = Layer.effect;
                      haloRotateSpeed = -1;
                      shapes = 2;
                      triLength = 0f;
                      triLengthTo = 8f;
                      haloRadius = 0;
                      haloRadiusTo = 14f;
                      tri = true;
                      radius = 6;
                      shapeRotation = 180f;
                    }},
                    new HaloPart(){{
                      progress = PartProgress.warmup;
                      color = SglDrawConst.matrixNet;
                      layer = Layer.effect;
                      haloRotateSpeed = 1;
                      shapes = 2;
                      triLength = 0f;
                      triLengthTo = 12f;
                      haloRadius = 8;
                      tri = true;
                      radius = 8;
                    }},
                    new HaloPart(){{
                      progress = PartProgress.warmup;
                      color = SglDrawConst.matrixNet;
                      layer = Layer.effect;
                      haloRotateSpeed = 1;
                      shapes = 2;
                      triLength = 0f;
                      triLengthTo = 8f;
                      haloRadius = 8;
                      tri = true;
                      radius = 8;
                      shapeRotation = 180f;
                    }},
                    new CustomPart(){{
                      layer = Layer.effect;
                      progress = PartProgress.warmup;

                      draw = (x, y, r, p) -> {
                        Draw.color(SglDrawConst.matrixNet);
                        SglDraw.gapTri(x + Angles.trnsx(r + Time.time, 16, 0), y + Angles.trnsy(r + Time.time, 16, 0), 12*p, 42, 12, r + Time.time);
                        SglDraw.gapTri(x + Angles.trnsx(r + Time.time + 180, 16, 0), y + Angles.trnsy(r + Time.time + 180, 16, 0), 12*p, 42, 12, r + Time.time + 180);
                      };
                    }}
                );

                Weapon s = this;
                bullet = new ContinuousLaserBulletType(){
                  {
                    damage = 260;
                    lifetime = 180;
                    fadeTime = 30;
                    length = 720;
                    width = 6;
                    hitColor = SglDrawConst.matrixNet;
                    shootEffect = SglFx.explodeImpWave;
                    chargeEffect = SglFx.auroraCoreCharging;
                    chargeSound = Sounds.lasercharge;
                    fragBullets = 2;
                    fragSpread = 10;
                    fragOnHit = true;
                    fragRandomSpread = 60;
                    shake = 5;
                    incendAmount = 0;
                    incendChance = 0;

                    drawSize = 620;
                    pointyScaling = 0.7f;
                    oscMag = 0.85f;
                    oscScl = 1.1f;
                    frontLength = 70;
                    lightColor = SglDrawConst.matrixNet;
                    colors = new Color[]{
                        Color.valueOf("8FFFF0").a(0.6f),
                        Color.valueOf("8FFFF0").a(0.85f),
                        Color.valueOf("B6FFF7"),
                        Color.valueOf("D3FDFF")
                    };
                  }

                  @Override
                  public void update(Bullet b) {
                    super.update(b);
                    if (b.owner instanceof Unit u){
                      u.vel.lerp(0, 0, 0.1f);

                      float bulletX = u.x + Angles.trnsx(u.rotation - 90, x + shootX, y + shootY),
                          bulletY = u.y + Angles.trnsy(u.rotation - 90, x + shootX, y + shootY),
                          angle = u.rotation;

                      b.rotation(angle);
                      b.set(bulletX, bulletY);

                      for (WeaponMount mount : u.mounts) {
                        mount.reload = mount.weapon.reload;
                        if (mount.weapon == s){
                          mount.recoil = 1;
                        }
                      }

                      if(ejectEffect != null) ejectEffect.at(bulletX, bulletY, angle, b.type.hitColor);
                    }
                  }

                  @Override
                  public void draw(Bullet b) {
                    float realLength = Damage.findLaserLength(b, length);
                    float fout = Mathf.clamp(b.time > b.lifetime - fadeTime ? 1f - (b.time - (lifetime - fadeTime)) / fadeTime : 1f);
                    float baseLen = realLength * fout;
                    float rot = b.rotation();

                    for(int i = 0; i < colors.length; i++){
                      Draw.color(Tmp.c1.set(colors[i]).mul(1f + Mathf.absin(Time.time, 1f, 0.1f)));

                      float colorFin = i / (float)(colors.length - 1);
                      float baseStroke = Mathf.lerp(strokeFrom, strokeTo, colorFin);
                      float stroke = (width + Mathf.absin(Time.time, oscScl, oscMag)) * fout * baseStroke;
                      float ellipseLenScl = Mathf.lerp(1 - i / (float)(colors.length), 1f, pointyScaling);

                      Lines.stroke(stroke);
                      Lines.lineAngle(b.x, b.y, rot, baseLen - frontLength, false);

                      //back ellipse
                      Drawf.flameFront(b.x, b.y, divisions, rot + 180f, backLength, stroke / 2f);

                      //front ellipse
                      Tmp.v1.trnsExact(rot, baseLen - frontLength);
                      Drawf.flameFront(b.x + Tmp.v1.x, b.y + Tmp.v1.y, divisions, rot, frontLength * ellipseLenScl, stroke / 2f);
                    }

                    Tmp.v1.trns(b.rotation(), baseLen * 1.1f);

                    Drawf.light(b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, lightStroke, lightColor, 0.7f);

                    Draw.color(SglDrawConst.matrixNet);

                    float step = 1/45f;
                    Tmp.v1.set(length, 0).setAngle(b.rotation());
                    float dx = Tmp.v1.x;
                    float dy = Tmp.v1.y;
                    for (int i = 0; i < 45; i++) {
                      if(i*step*length > realLength) break;

                      float lerp = Mathf.clamp(b.time/(fadeTime*step*i))*Mathf.sin(Time.time/2 - i*step*Mathf.pi*6);
                      Draw.alpha(0.4f + 0.6f*lerp);
                      SglDraw.drawDiamond(b.x + dx*step*i, b.y + dy*step*i, 8*fout, 16 + 20*lerp + 80*(1 - fout), b.rotation());
                    }
                    Draw.reset();
                  }
                };

                alternativeBullet = new BulletType(){
                  {
                    pierceArmor = true;
                    damage = 320;
                    splashDamageRadius = 60;
                    splashDamage = 180;
                    speed = 10;
                    lifetime = 120;
                    homingRange = 450;
                    homingPower = 0.25f;
                    hitColor = SglDrawConst.matrixNet;
                    hitEffect = Fx.flakExplosion;

                    trailLength = 40;
                    trailWidth = 3;
                    trailColor = SglDrawConst.matrixNet;
                    trailEffect = SglFx.trailParticle;
                    trailChance = 0.4f;

                    homingDelay = 30;
                  }

                  @Override
                  public void updateHoming(Bullet b) {
                    if (b.time < homingDelay) {
                      b.vel.lerpDelta(0, 0, 0.06f);
                    }

                    if(homingPower > 0.0001f && b.time >= homingDelay){
                      float realAimX = b.aimX < 0 ? b.x : b.aimX;
                      float realAimY = b.aimY < 0 ? b.y : b.aimY;

                      Teamc target;
                      if(b.aimTile != null && b.aimTile.build != null && b.aimTile.build.team != b.team && collidesGround && !b.hasCollided(b.aimTile.build.id)){
                        target = b.aimTile.build;
                      }else{
                        target = Units.closestTarget(b.team, realAimX, realAimY, homingRange,
                            e -> e != null && e.checkTarget(collidesAir, collidesGround) && !b.hasCollided(e.id),
                            t -> t != null && collidesGround && !b.hasCollided(t.id));
                      }

                      if(target != null){
                        float v = Mathf.lerpDelta(b.vel.len(), speed, 0.08f);
                        b.vel.setLength(v);
                        b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(target), homingPower*(v/speed)*Time.delta*50f));
                      }
                      else{
                        b.vel.lerpDelta(0, 0, 0.06f);
                      }
                    }
                  }

                  @Override
                  public void draw(Bullet b) {
                    super.draw(b);
                    Draw.color(SglDrawConst.matrixNet);
                    Drawf.tri(b.x, b.y, 8, 24, b.rotation());
                    Drawf.tri(b.x, b.y, 8, 10, b.rotation() + 180);
                  }

                  public void hitEntity(Bullet b, Hitboxc entity, float health){
                    if(entity instanceof Unit unit){
                      if(unit.shield > 0){
                        float damageShield = Math.min(Math.max(unit.shield, 0), b.type.damage*1.25f);
                        unit.shield -= damageShield;
                        Fx.colorSparkBig.at(b.x, b.y, b.rotation(), SglDrawConst.matrixNet);
                      }
                    }
                    super.hitEntity(b, entity, health);
                  }
                };
              }

              @Override
              public void draw(Unit unit, WeaponMount mount) {
                super.draw(unit, mount);
                Tmp.v1.set(0, y).rotate(unit.rotation - 90);
                float dx = unit.x + Tmp.v1.x;
                float dy = unit.y + Tmp.v1.y;

                Lines.stroke(1.6f*(mount.charging? 1: mount.warmup*(1 - mount.recoil)), SglDrawConst.matrixNet);
                Draw.alpha(0.7f*mount.warmup*(1 - unit.elevation));
                float disX = Angles.trnsx(unit.rotation - 90, 3*mount.warmup, 0);
                float disY = Angles.trnsy(unit.rotation - 90, 3*mount.warmup, 0);

                Tmp.v1.set(0, 720).rotate(unit.rotation - 90);
                float angle = Tmp.v1.angle();
                float distX = Tmp.v1.x;
                float distY = Tmp.v1.y;

                Lines.line(dx + disX, dy + disY, dx + distX + disX, dy + distY + disY);
                Lines.line(dx - disX, dy - disY, dx + distX - disX, dy + distY - disY);
                float step = 1/30f;
                float rel = (1 - mount.reload / reload)*mount.warmup*(1 - unit.elevation);
                for (float i = 0.001f; i <= 1; i += step){
                  Draw.alpha(rel > i? 1: Mathf.maxZero(rel - (i - step))/step);
                  Drawf.tri(dx + distX*i, dy + distY*i, 3, 2.598f, angle);
                }

                Draw.reset();

                Draw.color(SglDrawConst.matrixNet);
                float relLerp = mount.charging? 1: 1 - mount.reload / reload;
                float edge = Math.max(relLerp, mount.recoil*1.25f);
                Lines.stroke(0.8f*edge);
                Draw.z(Layer.bullet);
                SglDraw.dashCircle(dx, dy, 10, 4, 240, Time.time*0.8f);
                Lines.stroke(edge);
                Lines.circle(dx, dy, 8);
                Fill.circle(dx, dy, 5* relLerp);

                SglDraw.drawDiamond(dx, dy, 6 + 12*relLerp, 3*relLerp, Time.time);
                SglDraw.drawDiamond(dx, dy, 5 + 10*relLerp, 2.5f*relLerp, -Time.time*0.87f);
              }

              @Override
              public void update(Unit unit, WeaponMount mount) {
                float axisX = unit.x + Angles.trnsx(unit.rotation - 90,  x, y),
                    axisY = unit.y + Angles.trnsy(unit.rotation - 90,  x, y);

                if (mount.charging) mount.reload = mount.weapon.reload;

                if (unit.isFlying()){
                  mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - unit.rotation;
                  mount.rotation = mount.targetRotation;
                }
                else{
                  mount.rotation = 0;
                }

                if (mount.warmup < 0.01f){
                  mount.reload = Math.max(mount.reload - 0.2f*Time.delta, 0);
                }

                super.update(unit, mount);
              }
            }
        );
      }

      @Override
      public void init() {
        super.init();

        omniMovement = true;
      }
    };

    machine_construct_dock = new PayloadCrafter("machine_construct_dock"){{
      requirements(Category.units, ItemStack.with());
      size = 5;
    }};
  }
}
