package singularity.contents;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.part.HaloPart;
import mindustry.entities.pattern.ShootPattern;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.world.Block;
import mindustry.world.meta.BlockFlag;
import singularity.Sgl;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.world.SglFx;
import singularity.world.blocks.product.PayloadCrafter;
import singularity.world.draw.part.CustomPart;
import singularity.world.unit.AirSeaAmphibiousUnit;
import singularity.world.unit.RelatedWeapon;
import singularity.world.unit.UnitEntityType;
import singularity.world.unit.UnitTypeRegister;

public class SglUnits implements ContentList{
  /**棱镜*/
  public static UnitType prism,
  /**光弧*/
  lightarc,
  /**黎明*/
  dawn,
  /**晨星*/
  mornstar,
  /**辉夜*/
  kaguya;

  /**极光*/
  @UnitEntityType(AirSeaAmphibiousUnit.AirSeaUnit.class)
  public static UnitType aurora;

  /**机械构造坞*/
  public static Block machine_construct_dock;

  @Override
  public void load() {
    UnitTypeRegister.registerAll();

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

        engines.addAll(
            new UnitEngine(){{
              x = 38f;
              y = -12;
              radius = 8;
              rotation = -45;
            }},
            new UnitEngine(){{
              x = -38f;
              y = -12;
              radius = 8;
              rotation = -135;
            }},
            new UnitEngine(){{
              x = 40f;
              y = -54;
              radius = 10;
              rotation = -45;
            }},
            new UnitEngine(){{
              x = -40f;
              y = -54;
              radius = 10;
              rotation = -135;
            }}
        );

        weapons.addAll(
            new Weapon(Sgl.modName + "-aurora_lightcone"){{
                shake = 5f;
                shootSound = Sounds.laser;
                x = 29;
                y = -30;
                shootY = 8;
                rotate = true;
                rotateSpeed = 3;
                recoil = 6;
                recoilTime = 60;
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
                    damage = 520;
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
              recoilTime = 45;
              cooldownTime = 60;
              reload = 30;
              shadow = 25;

              bullet = new LaserBulletType(){{
                damage = 285f;
                lifetime = 24;
                sideAngle = 20f;
                sideWidth = 1.75f;
                sideLength = 80f;
                width = 24f;
                length = 480f;
                shootEffect = Fx.shockwave;
                colors = new Color[]{SglDrawConst.matrixNetDark, SglDrawConst.matrixNet, Color.white};
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
                ejectEffect = SglFx.continuousLaserRecoil;
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
                alternativeShoot.shots = 14;
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
                    damage = 360;
                    splashDamageRadius = 60;
                    splashDamage = 180;
                    speed = 10;
                    lifetime = 60;
                    homingRange = 450;
                    homingPower = 0.25f;
                    hitColor = SglDrawConst.matrixNet;
                    hitEffect = Fx.flakExplosion;

                    trailLength = 40;
                    trailWidth = 3;
                    trailColor = SglDrawConst.matrixNet;
                    trailEffect = SglFx.trailParticle;
                    trailChance = 0.4f;

                    fragBullet = new BulletType(){
                      {
                        pierceCap = 3;
                        damage = 120;
                        speed = 18;
                        lifetime = 10;
                        hitEffect = Fx.colorSpark;
                        hitColor = SglDrawConst.matrixNet;
                        despawnEffect = Fx.none;
                      }

                      @Override
                      public void draw(Bullet b) {
                        super.draw(b);
                        Lines.stroke(2*b.fout(Interp.pow2Out), SglDrawConst.matrixNet);
                        Lines.line(b.x, b.y,
                            b.x + Angles.trnsx(b.rotation(), 8 + 12*b.fin(Interp.pow2Out), 0),
                            b.y + Angles.trnsy(b.rotation(), 8 + 12*b.fin(Interp.pow2Out), 0)
                        );
                      }
                    };
                    fragBullets = 4;
                    fragRandomSpread = 90;
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
    };

    machine_construct_dock = new PayloadCrafter("machine_construct_dock"){{
      requirements(Category.units, ItemStack.with());
      size = 5;
    }};
  }
}
