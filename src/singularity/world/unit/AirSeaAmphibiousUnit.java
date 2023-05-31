package singularity.world.unit;

import arc.math.Mathf;
import mindustry.ai.ControlPathfinder;
import mindustry.ai.Pathfinder;
import mindustry.ai.types.FlyingAI;
import mindustry.ai.types.GroundAI;
import mindustry.entities.EntityCollisions;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.gen.UnitWaterMove;
import mindustry.world.Tile;
import mindustry.world.meta.Env;

import static mindustry.Vars.state;
import static mindustry.Vars.tilesize;

public class AirSeaAmphibiousUnit extends SglUnitType {
  public float airReloadMulti = 0.75f;
  public float airShootingSpeedMulti = 0.8f;

  public AirSeaAmphibiousUnit(String name) {
    super(name);

    envEnabled |= Env.space;
    pathCost = ControlPathfinder.costHover;
    canBoost = true;

    aiController = () -> new GroundAI() {
      {
        fallback = new FlyingAI(){
          @Override
          public void updateMovement() {
            super.updateMovement();
            if(unit.type.canBoost && unit.elevation > 0.001f && !unit.onSolid()){
              unit.elevation = Mathf.approachDelta(unit.elevation, 0f, unit.type.riseSpeed);
            }
          }
        };
      }

      @Override
      public boolean useFallback() {
        return unit.isFlying();
      }

      @Override
      public void updateMovement() {
        Building core = unit.closestEnemyCore();

        if(core != null && unit.within(core, unit.range() / 1.3f + core.block.size * tilesize / 2f)){
          target = core;
          for(var mount : unit.mounts){
            if(mount.weapon.controllable && mount.weapon.bullet.collidesGround){
              mount.target = core;
            }
          }
        }

        if((core == null || !unit.within(core, unit.type.range * 0.5f))){
          boolean move = true;

          if (core != null){
            if (unit.type.canBoost && Mathf.len(core.tileX() - unit.tileX(), core.tileY() - unit.tileY()) > 50){
              unit.elevation = Mathf.approachDelta(unit.elevation, 1, unit.type.riseSpeed);
              return;
            }
          }

          if(state.rules.waves && unit.team == state.rules.defaultTeam){
            Tile spawner = getClosestSpawner();
            if (unit.type.canBoost && Mathf.len(spawner.x - unit.tileX(), spawner.y - unit.tileY()) > 50){
              unit.elevation = Mathf.approachDelta(unit.elevation, 1, unit.type.riseSpeed);
              return;
            }
            if(spawner != null && unit.within(spawner, state.rules.dropZoneRadius + 120f)) move = false;
            if(spawner == null && core == null) move = false;
          }

          //no reason to move if there's nothing there
          if(core == null && (!state.rules.waves || getClosestSpawner() == null)){
            move = false;
          }

          if(move){
            pathfind(Pathfinder.fieldCore);
          }
        }

        if(unit.type.canBoost && unit.elevation > 0.001f && !unit.onSolid()){
          unit.elevation = Mathf.approachDelta(unit.elevation, 0f, unit.type.riseSpeed);
        }

        faceTarget();
      }
    };
  }

  @Override
  public void update(Unit unit) {
    super.update(unit);
    if (unit.isFlying()){
      unit.reloadMultiplier *= airReloadMulti;
      if (unit.isShooting){
        unit.speedMultiplier *= airShootingSpeedMulti;
      }
    }
  }

  public static class AirSeaUnit extends UnitWaterMove {
    @Override
    public EntityCollisions.SolidPred solidity() {
      return null;
    }

    public boolean canShoot() {
      return !this.disarmed && (!this.type.canBoost || elevation < 0.09f || elevation > 0.9f);
    }

    @Override
    public int classId() {
      return 50;
    }
  }
}
