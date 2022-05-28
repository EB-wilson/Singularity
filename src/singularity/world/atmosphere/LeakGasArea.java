package singularity.world.atmosphere;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Position;
import arc.util.Strings;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pool;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.World;
import mindustry.entities.EntityGroup;
import mindustry.gen.Drawc;
import mindustry.gen.Entityc;
import mindustry.gen.Groups;
import mindustry.gen.Unitc;
import mindustry.io.TypeIO;
import mindustry.ui.Fonts;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.world.SglFx;
import singularity.world.components.GasBlockComp;
import singularity.world.components.GasBuildComp;
import singularity.world.modules.GasesModule;

@SuppressWarnings("unchecked")
public class LeakGasArea implements Pool.Poolable, Drawc, GasBuildComp{
  public static final float maxGasCapacity = 100;
  
  public Tile tile;
  
  public float radius;
  public GasesModule gases = new GasesModule(this);
  
  public transient int id = EntityGroup.nextId();
  public boolean added = false;
  public float x, y;
  
  public int timing;
  
  @SuppressWarnings("all")
  public static GasBlockComp blockMark = new GasBlockComp(){
    @Override
    public boolean hasGases(){
      return true;
    }
  
    @Override
    public boolean outputGases(){
      return false;
    }
  
    @Override
    public float maxGasPressure(){
      return Sgl.atmospheres.current.getCurrPressure()*2;
    }
  
    @Override
    public float gasCapacity(){
      return maxGasCapacity;
    }
  
    @Override
    public boolean compressProtect(){
      return false;
    }
  };
  
  public static LeakGasArea create(){
    return new LeakGasArea();
  }
  
  @Override
  public GasBlockComp getGasBlock(){
    return blockMark;
  }
  
  @Override
  public GasesModule gases(){
    return gases;
  }
  
  public void set(Gas gas, float flow, Tile tile){
    this.tile = tile;
    gases.add(gas, flow);
    set(tile.drawx(), tile.drawy());
  }
  
  @Override
  public boolean isAdded(){
    return added;
  }
  
  @Override
  public void update(){
    float leakRate = Sgl.atmospheres.current.getCurrPressure()*gases().total()/24;
    float total = gases.total();
    gases.each((gas, amount) ->{
      float present = amount/total;
      gases.remove(gas, leakRate*present);
      if(Vars.state.isCampaign()) Sgl.atmospheres.current.add(gas, leakRate*present);
    });
    
    float amount = gases.total()/maxGasCapacity;
    radius = 5*amount;
    float rate = Math.min(0.7f, gases.total()/10);
    
    if(Mathf.chanceDelta(rate)){
      SglFx.gasLeak.at(x, y, 0, gases.color(), amount);
    }
  
    Geometry.circle(tile.x, tile.y, 5, (x, y) -> {
      Tile otherT = Vars.world.tile(x, y);
      if(otherT == tile || otherT == null) return;
      LeakGasArea other = Sgl.gasAreas.get(otherT);
      
      if(other == null) return;
      if(Tmp.cr1.set(tile.x, tile.y, radius/Vars.tilesize).overlaps(Tmp.cr2.set(other.tile.x, other.tile.y, other.radius/Vars.tilesize))){
        Tile t = Vars.world.tile((tile.x + other.tile.x)/2, (tile.y + other.tile.y)/2);
        
        gases.each((gas, a) ->{
          float moveAmount = Math.min(a, 0.4f);
          Sgl.reactionPoints.transfer(t, gas, moveAmount);
          gases.remove(gas, moveAmount);
        });
      }
    });
  
    gases.update(false, false);
    
    if(timing>0){
      timing--;
    }
    else{
      if(gases.total() <= 1) remove();
    }
  }
  
  public void flow(Gas gas, float flow){
    gases.add(gas, flow);
    timing = 3;
  }
  
  @Override
  public void draw(){
    if(Sgl.config.debugMode){
      Fonts.outline.draw(Strings.autoFixed(gases.total(), 3), x, y-8, Color.white, 0.1f, true, 1);
    }
  }
  
  @Override
  public void read(Reads read){
    short REV = read.s();
    if (REV == 0) {
      x = read.f();
      read.i();
    } else {
      if (REV != 1) throw new IllegalArgumentException("Unknown revision '" + REV + "' for entity type 'PuddleComp'");
      x = read.f();
    }
    y = read.f();
    gases.read(read);
    tile = TypeIO.readTile(read);
    
    this.afterRead();
  }
  
  @Override
  public void afterRead(){
    Sgl.gasAreas.add(this);
  }
  
  @Override
  public void write(Writes writes){
    writes.s(1);
    writes.f(x);
    writes.f(y);
    gases.write(writes);
    TypeIO.writeTile(writes, tile);
  }
  
  @Override
  public void reset(){
    gases = null;
    id = EntityGroup.nextId();
    x = 0;
    y = 0;
    added = false;
  }
  
  @Override
  public void remove() {
    if (this.added) {
      Groups.all.remove(this);
      Groups.draw.remove(this);
      this.added = false;
      gases = null;
      Sgl.gasAreas.remove(this);
      Groups.queueFree(this);
    }
  }
  
  @Override
  public void add(){
    if (!added) {
      Groups.all.add(this);
      Groups.draw.add(this);
      gases = new GasesModule(this);
      added = true;
    }
  }
  
  @Override
  public boolean isLocal(){
    if(this instanceof Unitc u){
      return u.controller() != Vars.player;
    }
    
    return true;
  }
  
  @Override
  public boolean isRemote(){
    if (this instanceof Unitc u) {
      return u.isPlayer() && !this.isLocal();
    }
    return false;
  }
  
  @Override
  public boolean isNull(){
    return false;
  }
  
  @Override
  public <T extends Entityc> T self(){
    return (T)this;
  }
  
  @Override
  public <T> T as(){
    return (T)this;
  }
  
  @Override
  public int classId(){
    return 100;
  }
  
  @Override
  public boolean serialize(){
    return true;
  }
  
  @Override
  public int id(){
    return id;
  }
  
  @Override
  public void id(int id){
    this.id = id;
  }
  
  @Override
  public float clipSize(){
    return 20f;
  }
  
  @Override
  public void set(float x, float y){
    this.x = x;
    this.y = y;
  }
  
  @Override
  public void set(Position position){
    set(position.getX(), position.getY());
  }
  
  @Override
  public void trns(float x, float y) {
    set(this.x + x, this.y + y);
  }
  
  @Override
  public void trns(Position position){
    trns(position.getX(), position.getY());
  }
  
  @Override
  public int tileX() {
    return World.toTile(x);
  }
  
  @Override
  public int tileY() {
    return World.toTile(y);
  }
  
  @Override
  public Floor floorOn(){
    Tile tile = this.tileOn();
    return tile != null && tile.block() == Blocks.air ? tile.floor() : (Floor)Blocks.air;
  }
  
  @Override
  public Block blockOn(){
    Tile tile = this.tileOn();
    return tile == null ? Blocks.air : tile.block();
  }
  
  @Override
  public boolean onSolid(){
    return false;
  }
  
  @Override
  public Tile tileOn(){
    return Vars.world.tileWorld(this.x, this.y);
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
  public ItemModule items(){
    return null;
  }
  
  @Override
  public LiquidModule liquids(){
    return null;
  }
}
