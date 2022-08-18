package singularity.world.lightnings;

import arc.func.Cons;
import arc.func.Cons2;
import arc.math.geom.Position;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.entities.EntityGroup;
import mindustry.gen.Building;
import mindustry.gen.Drawc;
import mindustry.gen.Groups;
import mindustry.gen.Unitc;
import mindustry.io.TypeIO;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import singularity.world.lightnings.generator.LightningGenerator;

/**闪电容器的实体实现，通常大范围的闪电绘制需要使用这个实体，只在局部的draw中使用前容器实现
 * 注意这个容器应当被保存并持续使用，不应该随时回收，尽管实现了{@link arc.util.pooling.Pool.Poolable}接口，但这个对象通常情况应当被重复使用而不是不断的释放又创建*/
@SuppressWarnings("unchecked")
public class LightningsGroup extends LightningContainer implements Drawc, Pool.Poolable{//TODO 未完成
  public Cons<LightningsGroup> perDraw;
  public Cons2<LightningVertex, LightningVertex> lightningPathUpdate;

  boolean added;
  float x, y;
  Tile tile;
  int id = EntityGroup.nextId();

  private LightningsGroup(){}

  public static LightningsGroup create(float x, float y){
    LightningsGroup result = Pools.obtain(LightningsGroup.class, LightningsGroup::new);
    result.x = x;
    result.y = y;

    result.add();
    return result;
  }

  public static LightningsGroup create(float x, float y, LightningGenerator generator){
    LightningsGroup result = create(x, y);
    result.generator = generator;
    return result;
  }

  @Override
  public void set(float x, float y){
    this.x = x;
    this.y = y;
  }

  @Override
  public void set(Position position){
    this.x = position.getX();
    this.y = position.getY();
  }

  @Override
  public void trns(float x, float y){
    this.x += x;
    this.y += y;
  }

  @Override
  public void trns(Position position){
    this.x += position.getX();
    this.y += position.getY();
  }

  @Override
  public int tileX(){
    return tile.x;
  }

  @Override
  public int tileY(){
    return tile.y;
  }

  @Override
  public Floor floorOn(){
    return tile.floor();
  }

  @Override
  public Building buildOn(){
    return tile.build;
  }

  @Override
  public Block blockOn(){
    return tile.block();
  }

  @Override
  public boolean onSolid(){
    return tile.solid();
  }

  @Override
  public Tile tileOn(){
    return tile;
  }

  @Override
  public float getX(){
    return x;
  }

  @Override
  public float getY(){
    return y;
  }

  @Override
  public float x(){
    return x;
  }

  @Override
  public void x(float x){
    this.x = x;
  }

  @Override
  public float y(){
    return y;
  }

  @Override
  public void y(float y){
    this.y = y;
  }

  @Override
  public boolean isAdded(){
    return added;
  }

  @Override
  public void draw(){
    if(perDraw != null) perDraw.get(this);
    super.draw();
  }

  @Override
  public void update(){
    if(generator != null){
      generator.originX = x;
      generator.originY = y;
    }
    if(lightningPathUpdate == null) return;
    LightningVertex last = null;
    for(Lightning lightning: lightnings){
      for(LightningVertex vertex: lightning.vertices){
        if(!vertex.valid) continue;
        if(last != null) lightningPathUpdate.get(vertex, last);
        last = vertex;
      }
    }
  }

  @Override
  public void remove(){
    if(added){
      added = false;
      Groups.queueFree(this);
      Groups.all.remove(this);
      Groups.draw.remove(this);
    }
  }

  @Override
  public void add(){
    if(!added){
      added = true;
      Groups.all.add(this);
      Groups.draw.add(this);
    }
  }

  public boolean isLocal() {
    if(this instanceof Unitc u) {
      return u.controller() != Vars.player;
    }
    else{
      return true;
    }
  }

  public boolean isRemote() {
    if(!(this instanceof Unitc u)) {
      return false;
    }
    else{
      return u.isPlayer() && !this.isLocal();
    }
  }

  @Override
  public boolean isNull(){
    return false;
  }

  @Override
  public LightningsGroup self(){
    return this;
  }

  @Override
  public LightningsGroup as(){
    return this;
  }

  @Override
  public int classId(){
    return 105;
  }

  @Override
  public boolean serialize(){
    return true;
  }

  public void read(Reads read) {
    short REV = read.s();
    if (REV == 0) {
      this.x = read.f();
      read.i();
    } else {
      if (REV != 1) {
        throw new IllegalArgumentException("Unknown revision '" + REV + "' for entity type 'PuddleComp'");
      }

      this.x = read.f();
    }

    this.y = read.f();
    this.tile = TypeIO.readTile(read);
    this.afterRead();
  }

  public void afterRead() {
  }

  public void write(Writes writes) {
    writes.s(1);
    writes.f(this.x);
    writes.f(this.y);
    TypeIO.writeTile(writes, this.tile);
  }

  public int id() {
    return this.id;
  }

  public void id(int id) {
    this.id = id;
  }

  @Override
  public void reset(){
    x = 0;
    y = 0;
    added = false;
    tile = null;
    id = EntityGroup.nextId();

    time = 0;
    generator = null;
    lifeTime = 0;
    maxWidth = 0;
    minWidth = 0;
    clipSize = 9;
    lerp = null;
    branchCreated = null;

    for(Lightning lightning: lightnings){
      Pools.free(lightning);
    }
    lightnings.clear();
  }
}
