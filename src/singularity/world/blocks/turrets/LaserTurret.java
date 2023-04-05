package singularity.world.blocks.turrets;

import arc.math.Angles;
import arc.struct.Seq;
import mindustry.gen.Bullet;
import mindustry.world.blocks.defense.turrets.Turret;

public class LaserTurret extends SglTurret{
  private final int timeId = timers++;

  public float shootingRotateSpeedScl = 0.35f;
  public float shootEffInterval = 5;
  public boolean needCooldown = true;
  public boolean shootingConsume = false;

  public LaserTurret(String name){
    super(name);
  }

  public class LaserTurretBuild extends SglTurretBuild{
    public Seq<Turret.BulletEntry> allLaser = new Seq<>();

    @Override
    public void turnToTarget(float targetRot){
      rotation = Angles.moveToward(rotation, targetRot, rotateSpeed*delta()*(allLaser.isEmpty()? 1: shootingRotateSpeedScl));
    }

    @Override
    public void updateTile(){
      super.updateTile();

      allLaser.removeAll(e -> e.bullet.type == null || !e.bullet.isAdded() || e.bullet.owner != this);

      if(allLaser.any()){
        wasShooting = true;

        for(Turret.BulletEntry entry: allLaser){
          float bulletX = x + Angles.trnsx(rotation - 90, shootX + entry.x, shootY + entry.y),
              bulletY = y + Angles.trnsy(rotation - 90, shootX + entry.x, shootY + entry.y),
              angle = rotation + entry.rotation;

          entry.bullet.rotation(angle);
          entry.bullet.set(bulletX, bulletY);

          if(shootEffect != null && timer(timeId, shootEffInterval)) shootEffect.at(bulletX, bulletY, angle, entry.bullet.type.hitColor);
        }
      }
      else if(heat > 0 && needCooldown){
        wasShooting = true;
      }
    }

    @Override
    protected void handleBullet(Bullet bullet, float offsetX, float offsetY, float angleOffset){
      allLaser.add(new Turret.BulletEntry(bullet, offsetX, offsetY, angleOffset, bullet.lifetime));
    }

    @Override
    public boolean shouldConsume(){
      return super.shouldConsume() && (shootingConsume || allLaser.isEmpty()) && !(heat > 0 && needCooldown);
    }

    @Override
    public boolean canShoot(){
      return allLaser.isEmpty() && !(heat > 0 && needCooldown);
    }

    @Override
    public float activeSoundVolume() {
      return warmup;
    }

    @Override
    public boolean shouldActiveSound(){
      return allLaser.any();
    }
  }
}
