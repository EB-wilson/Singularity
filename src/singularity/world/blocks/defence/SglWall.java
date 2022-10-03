package singularity.world.blocks.defence;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.*;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.Wall;
import universecore.UncCore;
import universecore.annotations.Annotations;
import universecore.math.gravity.GravityField;
import universecore.math.gravity.GravitySystem;
import universecore.util.aspect.EntityAspect;
import universecore.util.aspect.triggers.TriggerEntry;
import universecore.util.path.BFSPathFinder;
import universecore.util.path.IPath;
import universecore.util.path.PathFindFunc;

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

  public class SglWallBuild extends WallBuild{
    private static final ObjectSet<GravityGroup> ITERATED = new ObjectSet<>();

    protected GravityGroup gravGroup;
    private final OrderedSet<SglWallBuild> proximityGrav = new OrderedSet<>();

    static{
      Events.run(EventType.Trigger.update, ITERATED::clear);
    }

    @Override
    public SglWall block(){
      return SglWall.this;
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      new GravityGroup().add(this);

      return this;
    }

    @Override
    public void onProximityAdded(){
      super.onProximityAdded();

      proximityGrav.clear();
      for(Building building: proximity){
        if(building instanceof SglWallBuild w && w.block().mass > 256){
          proximityGrav.add(w);
        }
      }

      for(SglWallBuild build: proximityGrav){
        gravGroup.add(build.gravGroup);
      }
      gravGroup.clip(16);
    }

    @Override
    public void onProximityRemoved(){
      super.onProximityRemoved();

      for(ClipGravitySystem system: gravGroup.childGroup){
        Pools.free(system);
      }
      gravGroup.childGroup.clear();
      gravGroup.remove(this);
    }

    @Override
    public void updateTile(){
      super.updateTile();

      if(Core.graphics.getFrameId() - bulletsMark <= 1){
        if(!ITERATED.add(gravGroup)) return;

        for(ClipGravitySystem system: gravGroup.childGroup){
          system.field().setAssociatedFields(
              bulletMap.values(),
              this::gravitable,
              GravitySystem::field
          );
        }
      }
    }

    private boolean gravitable(BulletGravitySystem e){
      return e.bullet.team != team && (e.bullet.type.collides || e.bullet.type.absorbable || e.bullet.type.hittable);
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

    public float mass(){
      return mass;
    }
  }

  @Annotations.ImplEntries
  protected static class GravityGroup implements BFSPathFinder<SglWallBuild>{
    private static final ObjectSet<SglWallBuild> added = new ObjectSet<>();
    private static final Queue<SglWallBuild> queue = new Queue<>();

    private static final IntMap<OrderedSet<SglWallBuild>> clips = new IntMap<>();

    OrderedSet<SglWallBuild> child = new OrderedSet<>();
    Seq<ClipGravitySystem> childGroup = new Seq<>();

    public void clip(int clipStep){
      clips.clear();

      for(SglWallBuild build: child){
        int pos = Point2.pack(build.tileX()/clipStep, build.tileY()/clipStep);
        OrderedSet<SglWallBuild> clip = clips.get(pos, OrderedSet::new);
        clip.add(build);
      }

      for(ClipGravitySystem clip: childGroup){
        Pools.free(clip);
      }
      childGroup.clear();

      added.clear();

      ClipGravitySystem clip;
      for(OrderedSet<SglWallBuild> value: clips.values()){
        for(SglWallBuild build: value){
          if(added.contains(build)) break;
          clip = ClipGravitySystem.make();

          queue.clear();

          queue.addFirst(build);
          added.add(build);
          clip.add(build);

          while(!queue.isEmpty()){
            for(SglWallBuild vertex: queue.removeFirst().proximityGrav){
              if(!value.contains(vertex)) continue;

              if(added.add(vertex)){
                queue.addFirst(vertex);
                clip.add(vertex);
              }
            }
          }

          childGroup.add(clip);
        }
      }
    }

    public void add(GravityGroup other){
      if(other == this) return;

      for(SglWallBuild build: other.child){
        add(build);
      }
    }

    public void add(SglWallBuild build){
      child.add(build);
      build.gravGroup = this;
    }

    public void remove(SglWallBuild build){
      for(SglWallBuild other: build.proximityGrav){
        if(other.gravGroup != this) continue;
        other.proximityGrav.remove(build);

        new GravityGroup(){{
          eachVertices(other, this::add);
        }};
        other.gravGroup.clip(16);
      }
    }

    @Override
    public void reset(){
      added.clear();
      queue.clear();
    }

    @Override
    public boolean relateToPointer(SglWallBuild sglWallBuild, PathPointer<SglWallBuild> pathPointer){
      return added.add(sglWallBuild);
    }

    @Override
    public PathPointer<SglWallBuild> getPointer(SglWallBuild sglWallBuild){
      return null;
    }

    @Override
    public SglWallBuild queueNext(){
      return queue.isEmpty()? null: queue.removeFirst();
    }

    @Override
    public void queueAdd(SglWallBuild sglWallBuild){
      queue.addFirst(sglWallBuild);
    }

    @Override
    public IPath<SglWallBuild> createPath(){
      return null;
    }

    @Override
    public Iterable<SglWallBuild> getLinkVertices(SglWallBuild sglWallBuild){
      return sglWallBuild.proximityGrav;
    }

    @Override
    public boolean isDestination(SglWallBuild sglWallBuild, SglWallBuild vert1){
      return false;
    }

    @Override
    public void findPath(SglWallBuild origin, PathFindFunc.PathAcceptor<SglWallBuild> pathConsumer){
      throw new UnsupportedOperationException();
    }
  }

  @Annotations.ImplEntries
  protected static class ClipGravitySystem implements GravitySystem, Pool.Poolable{
    float mass;
    float argX, argY;
    Vec2 position = new Vec2();

    private final GravityField gravityField = new GravityField(this);
    private long curr;

    public static ClipGravitySystem make(){
      return Pools.obtain(ClipGravitySystem.class, ClipGravitySystem::new);
    }

    public void add(SglWallBuild build){
      mass += build.mass();
      argX += build.mass()*build.x;
      argY += build.mass()*build.y;

      position.set(argX/mass, argY/mass);
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

    @Override
    public void reset(){
      mass = 0;
      position.setZero();
      argX = argY = 0;

      field().remove();
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
