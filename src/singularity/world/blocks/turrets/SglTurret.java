package singularity.world.blocks.turrets;

import arc.Core;
import arc.audio.Sound;
import arc.func.Boolf;
import arc.func.Cons2;
import arc.func.Floatf;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.EnumSet;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.content.UnitTypes;
import mindustry.core.World;
import mindustry.entities.*;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.logic.Ranged;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.world.blocks.ControlBlock;
import mindustry.world.meta.*;
import singularity.graphic.SglDrawConst;
import singularity.ui.UIUtils;
import singularity.world.blocks.SglBlock;
import singularity.world.consumers.SglConsumers;
import singularity.world.draw.DrawSglTurret;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.*;

import static mindustry.Vars.tilesize;
import static mindustry.world.blocks.defense.turrets.Turret.logicControlCooldown;

@Annotations.ImplEntries
public class SglTurret extends SglBlock{
  private final int timerTarget = timers++;

  /**炮塔的索敌范围*/
  public float range = 80f;
  /**是否根据敌人的移动提前修正弹道*/
  public boolean accurateDelay = true;
  /**是否根据敌人的移动提前修正弹道*/
  public boolean accurateSpeed = true;

  /**是否攻击空中目标*/
  public boolean targetAir = true;
  /**是否攻击地面目标*/
  public boolean targetGround = true;
  /**是否瞄准生命值未满的友方*/
  public boolean targetHealing;
  /**瞄准右方时是否瞄准单位*/
  public boolean targetHealUnit = true;
  /**单位目标选择过滤器*/
  public Boolf<Unit> unitFilter = u -> true;
  /**建筑目标选择过滤器*/
  public Boolf<Building> buildingFilter = b -> !b.block.underBullets;
  /**单位索敌排序准则，默认为最近目标*/
  public Units.Sortf unitSort = UnitSorts.closest;

  /**能否由玩家控制*/
  public boolean playerControllable = true;

  /**索敌时间间隔，以刻为单位*/
  public float targetInterval = 20;
  /**预热速度*/
  public float warmupSpeed = 0.1f;
  /**是否为线性预热过程*/
  public boolean linearWarmup = true;
  /**开火预热阈值，需要达到阈值才能开火*/
  public float fireWarmupThreshold = 0;

  /**开火音效*/
  public Sound shootSound = Sounds.shoot;
  /**开火音效音调*/
  public float shootSoundPitch = 1, shootSoundVolume = 1;
  public Sound chargeSound = Sounds.none;
  /**充能音效音调*/
  public float chargeSoundPitch = 1, chargeSoundVolume = 1;
  /**音效音量范围*/
  public float soundPitchRange = 0.05f;

  /**开火特效*/
  public Effect shootEffect;
  /**烟雾特效*/
  public Effect smokeEffect;
  /**弹药使用特效（例如抛壳）*/
  public Effect ammoUseEffect = Fx.none;
  /**在炮塔冷却过程中显示的特效*/
  public Effect coolEffect = Fx.fuelburn;
  /**炮管红热时的光效遮罩层颜色*/
  public Color heatColor = Pal.turretHeat;

  /**弹药出膛的偏移位置*/
  public float shootX = 0f, shootY = Float.NEGATIVE_INFINITY;
  /**子弹消耗特效产生的偏移位置*/
  public float ammoEjectBack = 1f;
  /**开火抖动*/
  public float shake = 0f;
  /**子弹最小开火范围，用于跨射武器*/
  public float minRange = 0f;
  /**弹药出膛位置的横向平移范围*/
  public float xRand = 0f;
  /**子弹弹道的散布角度范围*/
  public float inaccuracy = 0f;
  /**子弹速度的随机偏差量*/
  public float velocityRnd = 0f;
  /**炮塔的高光角度*/
  public float elevation = -1f;

  /**射击模式*/
  public ShootPattern shoot = new ShootPattern();
  /**炮管冷却时间，这仅用于绘制热量*/
  public float cooldownTime = 20f;
  /**后座力复位时间，默认使用当前弹药的装载时长*/
  public float recoilTime = -1;
  /**后座偏移插值的幂，参考{@link arc.math.Interp}*/
  public float recoilPow = 1.8f;
  /**炮塔尝试对目标开火的最小直线偏差角度*/
  public float shootCone = 8f;
  /**每次射击后座力最大平移距离*/
  public float recoil = 1f;
  /**转向速度*/
  public float rotateSpeed = 5;
  /**炮台在充能时能否转向*/
  public boolean moveWhileCharging = true;
  /**炮塔充能时是否保持预热状态*/
  public boolean chargingWarm = true;

  public ObjectMap<BaseConsumers, AmmoDataEntry> ammoTypes = new ObjectMap<>();

  public SglTurret(String name){
    super(name);
    canOverdrive = false;
    update = true;
    solid = true;
    autoSelect = true;
    canSelect = false;
    outlinedIcon = 1;
    quickRotate = false;
    outlineIcon = true;
    attacks = true;
    priority = TargetPriority.turret;
    group = BlockGroup.turrets;
    flags = EnumSet.of(BlockFlag.turret);

    draw = new DrawSglTurret();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void init(){
    oneOfOptionCons = false;

    if(shootY == Float.NEGATIVE_INFINITY) shootY = size * tilesize / 2f;
    if(elevation < 0) elevation = size / 2f;

    for (BaseConsumers consumer : consumers) {
      for (BaseConsume baseConsume : consumer.all()) {
        if (baseConsume instanceof ConsumePower cp) cp.showIcon = true;

        if (!(baseConsume instanceof ConsumeItemBase<?>) && !(baseConsume instanceof ConsumePayload<?>)){
          Floatf old = baseConsume.consMultiplier;
          baseConsume.setMultiple(old == null? e -> (((SglTurretBuild)e).coolantScl): (e -> old.get(e)*((SglTurretBuild)e).coolantScl));
        }
      }
    }

    super.init();
  }

  public AmmoDataEntry newAmmo(BulletType ammoType){
    return newAmmo(ammoType, false, (t, p) -> {});
  }

  public AmmoDataEntry newAmmo(BulletType ammoType, Cons2<Table, BulletType> value){
    return newAmmo(ammoType, false, value);
  }

  public AmmoDataEntry newAmmo(BulletType ammoType, boolean override, Cons2<Table, BulletType> value){
    consume = new SglConsumers(false){
      {
        showTime = false;
      }

      @Override
      public BaseConsumers time(float time){
        showTime = false;
        craftTime = time;
        return this;
      }

      @Override
      public ConsumeItems<? extends ConsumerBuildComp> items(ItemStack[] items){
        ConsumeItems<? extends ConsumerBuildComp> res = new ConsumeItems<>(items);
        res.showPerSecond = false;
        return add(res);
      }
    };
    consumers().add(consume);

    AmmoDataEntry res;
    ammoTypes.put(consume, res = new AmmoDataEntry(ammoType, override));
    res.display(value);

    return res;
  }

  public void newCoolant(float scl, float duration){
    newOptionalConsume((SglTurretBuild e, BaseConsumers c) -> {
      e.applyCoolant(c, scl, duration);
    }, (s, c) -> {
      s.add(Stat.booster, t -> {
        t.table(req -> {
          req.left().defaults().left().padLeft(3);
          for (BaseConsume<? extends ConsumerBuildComp> co : c.all()) {
            co.buildIcons(req);
          }
        }).left().padRight(40);
        t.add(Core.bundle.format("bullet.reload", Strings.autoFixed(scl*100, 1))).growX().right();
      });
    });
    BaseConsumers c = consume;
    consume.customDisplayOnly = true;
    consume.optionalAlwaysValid = false;
    consume.consValidCondition((SglTurretBuild t) -> t.consumer.current != null && t.reloadCounter < t.consumer.current.craftTime
        && (t.currCoolant == null || t.currCoolant == c));
  }

  /**使用默认的冷却模式，与原版的冷却稍有不同，液体的温度和热容共同确定冷却力，热容同时影响液体消耗倍率*/
  public void newCoolant(float baseCoolantScl, float attributeMultiplier, Boolf<Liquid> filter, float usageBase, float duration){
    newCoolant(
        liquid -> baseCoolantScl + (liquid.heatCapacity*1.2f - (liquid.temperature - 0.35f)*0.6f)*attributeMultiplier,
        liquid -> !liquid.gas && liquid.coolant && filter.get(liquid),
        usageBase,
        liquid -> usageBase/(liquid.heatCapacity*0.7f),
        duration
    );
    BaseConsumers c = consume;
    consume.optionalAlwaysValid = false;
    consume.consValidCondition((SglTurretBuild t) -> (t.currCoolant == null || t.currCoolant == c));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void newCoolant(Floatf<Liquid> coolEff, Boolf<Liquid> filters, float usageBase, Floatf<Liquid> usageMult, float duration){
    newOptionalConsume((SglTurretBuild e, BaseConsumers c) -> {
      ConsumeLiquidCond<SglTurretBuild> cl;
      if((cl = (ConsumeLiquidCond<SglTurretBuild>) c.get(ConsumeType.liquid)) != null){
        Liquid curr = cl.getCurrCons(e);
        if(curr != null) e.applyCoolant(c, coolEff.get(curr), duration);
      }
    }, (s, c) -> {
      s.add(Stat.booster, t -> {
        t.defaults().left().padTop(4);
        t.row();
        if (c.get(ConsumeType.liquid) instanceof ConsumeLiquidCond cons){
          for (LiquidStack stack : cons.getCons()) {
            Liquid liquid = stack.liquid;

            t.add(StatValues.displayLiquid(liquid, usageBase*usageMult.get(liquid)*60, true)).padRight(40).left().top().height(50);
            t.table(Tex.underline, tb -> {
              tb.right().add(Core.bundle.format("bullet.reload", Strings.autoFixed(coolEff.get(liquid)*100, 1))).growX().right();
            }).height(50).growX().right();
            t.row();
          }
        }
      });
    });
    consume.optionalAlwaysValid = false;
    consume.add(new ConsumeLiquidCond<SglTurretBuild>(){
      {
        filter = filters;
        usage = usageBase;
        usageMultiplier = usageMult;

        maxFlammability = 0.1f;
      }

      @Override
      public void display(Stats stats){}
    });
    BaseConsumers c = consume;
    consume.consValidCondition((SglTurretBuild t) -> t.consumer.current != null && t.reloadCounter < t.consumer.current.craftTime
        && (t.currCoolant == null || t.currCoolant == c));
  }

  @Override
  public void setStats(){
    super.setStats();

    stats.add(Stat.shootRange, range / tilesize, StatUnit.blocks);
    stats.add(Stat.inaccuracy, (int)inaccuracy, StatUnit.degrees);
    stats.add(Stat.targetsAir, targetAir);
    stats.add(Stat.targetsGround, targetGround);

    stats.add(Stat.ammo, table -> {
      table.defaults().padLeft(15);
      for(ObjectMap.Entry<BaseConsumers, AmmoDataEntry> entry: ammoTypes){
        table.row();
        table.table(SglDrawConst.grayUIAlpha, t -> {
          t.left().defaults().left().growX();
          t.table(st -> {
            st.left().defaults().left();
            st.table(c -> {
              c.left().defaults().left();
              for (BaseConsume<? extends ConsumerBuildComp> consume : entry.key.all()) {
                c.table(cons -> {
                  cons.left().defaults().left().padLeft(3).fill();

                  consume.buildIcons(cons);
                }).fill();
              }
            }).fill();

            st.row();
            st.add(Stat.reload.localized() + ":" + Strings.autoFixed(60f/entry.key.craftTime*shoot.shots, 1) + StatUnit.perSecond.localized());

            if (entry.value.reloadAmount > 1) {
              st.row();
              st.add(Core.bundle.format("bullet.multiplier", entry.value.reloadAmount));
            }
          });
          t.row();

          AmmoDataEntry ammoEntry = entry.value;
          BulletType type = ammoEntry.bulletType;

          t.left().defaults().padRight(3).left();

          if(type.spawnUnit != null && type.spawnUnit.weapons.size > 0){
            UIUtils.buildAmmo(t, type.spawnUnit.weapons.first().bullet);
            return;
          }

          t.table(bt -> {
            bt.defaults().left();
            if(!ammoEntry.override){
              UIUtils.buildAmmo(bt, type);
            }

            for(Cons2<Table, BulletType> value: ammoEntry.statValues){
              value.get(bt, type);
            }
          }).left();
        }).fillY().growX().margin(10).pad(5);
      }
    });
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    super.drawPlace(x, y, rotation, valid);
    Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);
  }

  @Annotations.ImplEntries
  public class SglTurretBuild extends SglBuilding implements ControlBlock, Ranged{
    public Vec2 recoilOffset = new Vec2();
    public float charge;
    public float reloadCounter;
    public float coolantScl;
    public float coolantSclTimer;
    public float warmup;
    public Posc target;
    public float rotation = 90;
    public Vec2 targetPos = new Vec2();
    public BlockUnitc unit = (BlockUnitc) UnitTypes.block.create(team);
    public float curRecoil;
    public float heat;

    public boolean wasShooting;
    public boolean logicShooting;

    AmmoDataEntry currentAmmo;
    int shotStack;
    int totalShots;
    int queuedBullets;
    float logicControlTime;

    BaseConsumers currCoolant;

    void applyCoolant(BaseConsumers consing, float scl, float duration){
      coolantSclTimer = Math.max(coolantSclTimer, duration);
      currCoolant = consing;
      coolantScl = scl;
    }

    @Override
    public SglTurret block(){
      return SglTurret.this;
    }

    public boolean logicControlled(){
      return logicControlTime > 0;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void updateTile(){
      wasShooting = false;
      if(consumer.current == null) return;

      if(!ammoTypes.containsKey(consumer.current))
        throw new RuntimeException("unknown ammo recipe");

      currentAmmo = ammoTypes.get(consumer.current);

      curRecoil = Mathf.approachDelta(curRecoil, 0, 1/(recoilTime > 0? recoilTime: consumer.current.craftTime));
      heat = Mathf.approachDelta(heat, 0, 1/cooldownTime*coolantScl);
      charge = charging() ? Mathf.approachDelta(charge, 1, 1/shoot.firstShotDelay) : 0;

      unit.tile(this);
      unit.rotation(rotation);
      unit.team(team);
      recoilOffset.trns(rotation, -Mathf.pow(curRecoil, recoilPow)*recoil);

      updateTarget();

      if(!isControlled()){
        unit.aimX(targetPos.x);
        unit.aimY(targetPos.y);
      }

      if(logicControlTime > 0){
        logicControlTime -= Time.delta;
      }

      boolean tarValid = validateTarget();
      float targetRot = angleTo(targetPos);

      if(tarValid && (shootValid() || isControlled()) && (moveWhileCharging || !charging())){
        turnToTarget(targetRot);
      }

      if(wasShooting() && shootValid()){
        if(canShoot() && tarValid){
          warmup = linearWarmup? Mathf.approachDelta(warmup, 1, warmupSpeed*consEfficiency()):
              Mathf.lerpDelta(warmup, 1, warmupSpeed*consEfficiency());
          wasShooting = true;

          if(!charging() && warmup >= fireWarmupThreshold){
            if(reloadCounter >= consumer.current.craftTime){
              if(Angles.angleDist(rotation, targetRot) < shootCone){
                doShoot(currentAmmo.bulletType);
              }
            }
          }
        }
      }
      else if (!chargingWarm || !charging()){
        warmup = linearWarmup? Mathf.approachDelta(warmup, 0, warmupSpeed): Mathf.lerpDelta(warmup, 0, warmupSpeed);
      }

      if(canShoot() && shootValid() && !charging() && reloadCounter < consumer.current.craftTime){
        reloadCounter += consEfficiency()*delta()*coolantScl;
        if(coolantSclTimer > 0){
          ConsumeLiquidBase<SglTurretBuild> c = (ConsumeLiquidBase<SglTurretBuild>) consumer.optionalCurr.get(ConsumeType.liquid);
          float usage = 0;
          if(c instanceof ConsumeLiquidCond con){
            Liquid l = con.getCurrCons(this);
            if(l != null) usage = con.usageMultiplier.get(l);
          }
          else if(c instanceof ConsumeLiquids con){
            for(LiquidStack liquid: con.consLiquids){
              usage += liquid.amount;
            }
          }
          if(Mathf.chance(0.06*usage)){
            coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
          }
        }
      }

      if(coolantSclTimer > 0) coolantSclTimer -= Time.delta;
      else{
        currCoolant = null;
        coolantScl = 1;
      }
    }

    public boolean shootValid(){
      return consumeValid() || shotStack > 0;
    }

    public boolean canShoot(){
      return true;
    }

    public void turnToTarget(float targetRot){
      rotation = Angles.moveToward(rotation, targetRot, rotateSpeed*delta());
    }

    protected boolean validateTarget(){
      return !Units.invalidateTarget(target, canHeal() ? Team.derelict : team, x, y) || isControlled() || logicControlled();
    }

    @Override
    public boolean shouldConsume(){
      return super.shouldConsume() && !charging() && consumer.current != null && reloadCounter < consumer.current.craftTime;
    }

    public void doShoot(BulletType type){
      float bulletX = x + Angles.trnsx(rotation - 90, shootX, shootY),
          bulletY = y + Angles.trnsy(rotation - 90, shootX, shootY);

      if(shoot.firstShotDelay > 0){
        chargeSound.at(bulletX, bulletY, Math.max(chargeSoundPitch + Mathf.random(-soundPitchRange, soundPitchRange), 0.01f), chargeSoundVolume);
        type.chargeEffect.at(bulletX, bulletY, rotation, type.lightColor);
      }

      shoot.shoot(totalShots, (xOffset, yOffset, angle, delay, mover) -> {
        queuedBullets++;
        if(delay > 0f){
          Time.run(delay, () -> bullet(type, xOffset, yOffset, angle, mover));
        }else{
          bullet(type, xOffset, yOffset, angle, mover);
        }
        totalShots++;
      });

      reloadCounter %= consumer.current.craftTime;

      if(shotStack <= 0){
        consumer.trigger();
        shotStack = currentAmmo.reloadAmount;
      }
      if(shotStack > 0){
        shotStack--;
      }
    }

    protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
      queuedBullets--;

      if(dead) return;

      float xSpread = Mathf.range(xRand),
          bulletX = x + Angles.trnsx(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
          bulletY = y + Angles.trnsy(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
          shootAngle = rotation + angleOffset + Mathf.range(inaccuracy);

      float lifeScl = type.scaleLife ? Mathf.clamp(Mathf.dst(bulletX, bulletY, targetPos.x, targetPos.y) / type.range, minRange / type.range, range() / type.range) : 1f;

      handleBullet(type.create(this, team, bulletX, bulletY, shootAngle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, targetPos.x, targetPos.y), xOffset, yOffset, shootAngle - rotation);

      (shootEffect == null ? type.shootEffect : shootEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
      (smokeEffect == null ? type.smokeEffect : smokeEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
      shootSound.at(bulletX, bulletY, Math.max(shootSoundPitch + Mathf.random(-soundPitchRange, soundPitchRange), 0.01f), shootSoundVolume);

      ammoUseEffect.at(
          x - Angles.trnsx(rotation, ammoEjectBack),
          y - Angles.trnsy(rotation, ammoEjectBack),
          rotation * Mathf.sign(xOffset)
      );

      if(shake > 0){
        Effect.shake(shake, shake, this);
      }

      curRecoil = 1f;
      heat = 1f;
    }

    @Override
    public boolean canControl(){
      return playerControllable;
    }

    @Override
    public void control(LAccess type, double p1, double p2, double p3, double p4){
      if(type == LAccess.shoot && !unit.isPlayer()){
        targetPos.set(World.unconv((float)p1), World.unconv((float)p2));
        logicControlTime = logicControlCooldown;
        logicShooting = !Mathf.zero(p3);
      }

      super.control(type, p1, p2, p3, p4);
    }

    @Override
    public void control(LAccess type, Object p1, double p2, double p3, double p4){
      if(type == LAccess.shootp && (unit == null || !unit.isPlayer())){
        logicControlTime = logicControlCooldown;
        logicShooting = !Mathf.zero(p2);

        if(p1 instanceof Posc pos){
          targetPosition(pos);
        }
      }

      super.control(type, p1, p2, p3, p4);
    }

    @Override
    public double sense(LAccess sensor){
      return switch(sensor){
        case ammo -> items.total();
        case ammoCapacity -> itemCapacity;
        case rotation -> rotation;
        case shootX -> World.conv(targetPos.x);
        case shootY -> World.conv(targetPos.y);
        case shooting -> wasShooting() ? 1 : 0;
        case progress -> progress();
        default -> super.sense(sensor);
      };
    }

    @Override
    public float progress(){
      return consumer.current == null? 0: Mathf.clamp(reloadCounter/consumer.current.craftTime);
    }

    protected void handleBullet(Bullet bullet, float offsetX, float offsetY, float angleOffset){}

    public boolean charging(){
      return queuedBullets > 0 && shoot.firstShotDelay > 0;
    }

    public boolean wasShooting(){
      return isControlled()? unit.isShooting(): logicControlled()? logicShooting: target != null;
    }

    public void updateTarget(){
      if(timer(timerTarget, targetInterval)){
        findTarget();
      }

      if(isControlled()){
        targetPos.set(unit.aimX(), unit.aimY());
      }
      else{
        targetPosition(target);

        if(Float.isNaN(rotation)) rotation = 0;
      }
    }

    public void targetPosition(Posc pos){
      if(!shootValid() || pos == null || currentAmmo == null) return;

      Vec2 offset = Tmp.v1.setZero();

      if (accurateDelay && pos instanceof Hitboxc h){
        offset.set(h.deltaX(), h.deltaY()).scl(shoot.firstShotDelay / Time.delta);
      }

      if (accurateSpeed){
        targetPos.set(Predict.intercept(this, pos, offset.x, offset.y, currentAmmo.bulletType.speed <= 0.01f ? 99999999f : currentAmmo.bulletType.speed));
      }
      else targetPos.set(pos);

      if(targetPos.isZero()){
        targetPos.set(pos);
      }
    }

    @Override
    public float range(){
      if(currentAmmo != null){
        return range + currentAmmo.bulletType.rangeChange;
      }
      return range;
    }

    public void findTarget(){
      float range = range();

      if(targetAir && !targetGround){
        target = Units.bestEnemy(team, x, y, range, e -> !e.dead() && !e.isGrounded() && unitFilter.get(e), unitSort);
      }else{
        boolean heal = canHeal();

        target = Units.bestTarget(null, x, y, range,
            e -> (e.team != team || (heal && targetHealUnit && e.damaged())) && !e.dead() && unitFilter.get(e) && (e.isGrounded() || targetAir) && (!e.isGrounded() || targetGround),
            b -> (b.team != team || (heal && b.damaged())) && targetGround && buildingFilter.get(b), unitSort);
      }
    }

    protected boolean canHeal(){
      return targetHealing && shootValid() && currentAmmo.bulletType.collidesTeam && currentAmmo.bulletType.heals();
    }

    @Override
    public void drawSelect(){
      super.drawSelect();

      Drawf.dashCircle(x, y, range, Pal.placing);
    }

    @Override
    public float warmup(){
      return warmup;
    }

    @Override
    public float drawrot(){
      return rotation - 90;
    }

    @Override
    public boolean shouldConsumeOptions(){
      return consumer.current != null && reloadCounter < consumer.current.craftTime;
    }

    @Override
    public boolean shouldActiveSound() {
      return wasShooting() && warmup > 0.01f;
    }

    @Override
    public float activeSoundVolume() {
      return 1;
    }

    @Override
    public Unit unit(){
      unit.tile(this);
      unit.team(team);
      return (Unit)unit;
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.f(reloadCounter);
      write.f(warmup);
      write.f(rotation);
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      reloadCounter = read.f();
      warmup = read.f();
      rotation = read.f();
    }
  }

  public static class AmmoDataEntry{
    public int reloadAmount = 1;
    final BulletType bulletType;
    final boolean override;

    final Seq<Cons2<Table, BulletType>> statValues = new Seq<>();

    public AmmoDataEntry(BulletType bulletType, boolean override){
      this.override = override;
      this.bulletType = bulletType;
    }

    public AmmoDataEntry display(Cons2<Table, BulletType> statValue){
      statValues.add(statValue);
      return this;
    }

    public AmmoDataEntry setReloadAmount(int amount){
      reloadAmount = amount;
      return this;
    }
  }
}
