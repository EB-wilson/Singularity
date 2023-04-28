package singularity.world.unit;

import arc.func.Boolf;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.entities.Effect;
import mindustry.entities.Mover;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;

public class RelatedWeapon extends DataWeapon {
  public ShootPattern alternativeShoot;
  public BulletType alternativeBullet;
  public Boolf<Unit> useAlternative = e -> false;

  @Override
  protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
    unit.apply(shootStatus, shootStatusDuration);

    ShootPattern shoot = alternativeShoot != null && useAlternative.get(unit)? alternativeShoot: this.shoot;

    if(shoot.firstShotDelay > 0){
      mount.charging = true;
      chargeSound.at(shootX, shootY, Mathf.random(soundPitchMin, soundPitchMax));
      bullet.chargeEffect.at(shootX, shootY, rotation, bullet.keepVelocity || parentizeEffects ? unit : null);
    }

    shoot.shoot(mount.totalShots, (xOffset, yOffset, angle, delay, mover) -> {
      mount.totalShots++;
      if(delay > 0f){
        Time.run(delay, () -> bullet(unit, mount, xOffset, yOffset, angle, mover));
      }else{
        bullet(unit, mount, xOffset, yOffset, angle, mover);
      }
    });

    if(mount instanceof DataWeaponMount m){
      shoot(unit, m, shootX, shootY, rotation);
    }
  }

  @Override
  protected void bullet(Unit unit, WeaponMount mount, float xOffset, float yOffset, float angleOffset, Mover mover){
    if(!unit.isAdded()) return;

    mount.charging = false;
    float
        xSpread = Mathf.range(xRand),
        weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation),
        mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
        mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
        bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset),
        bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset),
        shootAngle = bulletRotation(unit, mount, bulletX, bulletY) + angleOffset,
        lifeScl = bullet.scaleLife ? Mathf.clamp(Mathf.dst(bulletX, bulletY, mount.aimX, mount.aimY) / bullet.range) : 1f,
        angle = angleOffset + shootAngle + Mathf.range(inaccuracy + bullet.inaccuracy);

    mount.bullet = (alternativeBullet != null && useAlternative.get(unit)? alternativeBullet: bullet)
        .create(unit, unit.team, bulletX, bulletY, angle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, mount.aimX, mount.aimY);
    handleBullet(unit, mount, mount.bullet);

    if(!continuous){
      shootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));
    }

    ejectEffect.at(mountX, mountY, angle * Mathf.sign(this.x));
    mount.bullet.type.shootEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);
    mount.bullet.type.smokeEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);

    unit.vel.add(Tmp.v1.trns(shootAngle + 180f, bullet.recoil));
    Effect.shake(shake, shake, bulletX, bulletY);
    mount.recoil = 1f;
    mount.heat = 1f;
  }
}
