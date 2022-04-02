package singularity.world.blocks.defence;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Wall;
import universecore.UncCore;
import universecore.annotations.Annotations;
import universecore.math.gravity.GravityField;
import universecore.math.gravity.GravitySystem;
import universecore.util.aspect.EntityAspect;
import universecore.util.aspect.triggers.TriggerEntry;

@Annotations.ImplEntries
public class SglWall extends Wall{
  protected static final ObjectMap<Bullet, BulletGravitySystem> bulletMap = new ObjectMap<>(128);
  protected static long bulletsMark;

  static {
    UncCore.aspects.addAspect(new EntityAspect<Bullet>(EntityAspect.Group.bullet, e -> true))
        .setTrigger(new TriggerEntry<>(EventType.Trigger.update, e -> {
          BulletGravitySystem sys = bulletMap.get(e);
          if(sys != null) sys.field().update();
        }))
        .setEntryTrigger(e -> {
          if(!bulletMap.containsKey(e)){
            BulletGravitySystem sys;
            bulletMap.put(e, sys = Pools.obtain(BulletGravitySystem.class, BulletGravitySystem::new));
            sys.setOwner(e);
            bulletsMark = Core.graphics.getFrameId();
          }})
        .setExitTrigger(e -> {
          BulletGravitySystem sys;
          if((sys = bulletMap.remove(e)) != null){
            Pools.free(sys);
            bulletsMark = Core.graphics.getFrameId();
          }
        });

    Events.on(EventType.ResetEvent.class, e -> {
      bulletMap.clear();
      bulletsMark = 0;
    });
  }

  /**一格方块的质量，mass不指定的情况下用size计算质量*/
  public float density = 1024;
  public float mass = -1;
  
  public float damageFilter = -1;
  public float healMultiplier = 1f/3;
  public Color healColor = Color.valueOf("84f491");

  public SglWall(String name){
    super(name);
    update = true;
  }

  @Override
  public void init(){
    super.init();
    if(mass == -1){
      mass = size*size*density;
    }
  }

  @Annotations.ImplEntries
  public class SglWallBuild extends WallBuild implements GravitySystem{
    private final Vec2 position = new Vec2();
    private final GravityField gravityField = new GravityField(this);

    @Override
    public SglWall block(){
      return SglWall.this;
    }

    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      return this;
    }

    @Override
    public void remove(){
      super.remove();
      field().remove();
    }

    @Override
    public void onProximityAdded(){
      super.onProximityAdded();
      position.set(x, y);
    }

    @Override
    public void updateTile(){
      super.updateTile();

      if(Core.graphics.getFrameId() - bulletsMark <= 1){
        field().setAssociatedFields(bulletMap.values(),
            e -> e.bullet.team != team && (e.bullet.type.collides || e.bullet.type.absorbable || e.bullet.type.hittable),
            GravitySystem::field);
      }
    }
  
    @Override
    public boolean collision(Bullet bullet){
      if(bullet.type.absorbable && bullet.type.reflectable && bullet.damage < damageFilter){
  
        heal(bullet.damage*bullet.type.buildingDamageMultiplier*healMultiplier);
        Fx.healBlockFull.at(x, y, size, healColor);
        
        return true;
      }
      return super.collision(bullet);
    }

    @Override
    public float mass(){
      return mass;
    }

    @Override
    public Vec2 position(){
      return position;
    }

    @Override
    public void gravityUpdate(Vec2 vec2){
      //no actions
    }
  }

  @Annotations.ImplEntries
  protected static class BulletGravitySystem implements GravitySystem, Pool.Poolable{
    public Bullet bullet;
    public Vec2 position = new Vec2();

    public GravityField gravityField = new GravityField(this);

    public void setOwner(Bullet owner){
      bullet = owner;
    }

    @Override
    public float mass(){
      return 1;
    }

    @Override
    public Vec2 position(){
      return position.set(bullet.x, bullet.y);
    }

    @Override
    public void gravityUpdate(Vec2 vec2){
      bullet.vel.add(vec2);
    }

    @Override
    public void reset(){
      bullet = null;
      position.setZero();
    }
  }
}
