package singularity.world.reaction;

import arc.math.geom.Position;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pool;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.ctype.MappableContent;
import mindustry.entities.EntityGroup;
import mindustry.entities.Puddles;
import mindustry.gen.Entityc;
import mindustry.gen.Groups;
import mindustry.gen.Unitc;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.type.Reaction;
import singularity.world.modules.GasesModule;

public class ReactionPoint implements Entityc, Pool.Poolable, ReactContainer{
  public Reaction<?, ?, ?> reaction;
  public Tile tile;
  
  public boolean added;
  public transient int id = EntityGroup.nextId();
  public float x, y;
  
  public float time;
  public float lifetime = 60f;
  
  public float heat = 0;
  
  public ItemModule inItems;
  public LiquidModule inLiquids;
  public GasesModule inGases;
  
  public ItemModule outItems;
  public LiquidModule outLiquids;
  public GasesModule outGases;
  
  public static ReactionPoint create(){
    return new ReactionPoint();
  }
  
  public void set(Tile tile, Reaction<?, ?, ?> reaction){
    this.tile = tile;
    this.reaction = reaction;
    set(tile.drawx(), tile.drawy());
  }
  
  public void addMaterial(MappableContent input, float amount){
    if(!reaction.accept(input)) return;
    if(input instanceof Item) inItems.add((Item)input, (int)amount);
    if(input instanceof Liquid) inLiquids.add((Liquid)input, amount);
    if(input instanceof Gas) inGases.add((Gas)input, amount);
  }
  
  @Override
  public void read(Reads reads){}
  
  @Override
  public void write(Writes writes){}
  
  @Override
  public void afterRead(){}
  
  @Override
  public boolean isAdded(){
    return added;
  }
  
  @Override
  public void heat(float heat){
    this.heat = heat;
  }
  
  @Override
  public ItemModule inItems(){
    return inItems;
  }
  
  @Override
  public LiquidModule inLiquids(){
    return inLiquids;
  }
  
  @Override
  public GasesModule inGases(){
    return inGases;
  }
  
  @Override
  public ItemModule outItems(){
    return outItems;
  }
  
  @Override
  public LiquidModule outLiquids(){
    return outLiquids;
  }
  
  @Override
  public GasesModule outGases(){
    return outGases;
  }
  
  @Override
  public void update(){
    reaction.doReact(this);
    
    if(outLiquids.total() > 0.001){
      outLiquids.each((liquid, amount) -> {
        Puddles.deposit(tile, liquid, amount);
        outLiquids.remove(liquid, amount);
      });
    }
    
    if(outGases.total() > 0.001){
      outGases.each(stack -> {
        Sgl.gasAreas.pour(tile, stack.gas, stack.amount);
      });
    }
    
    time = Math.min(time + Time.delta, lifetime);
    if(time >= lifetime && inItems.empty() && inLiquids.total() <= 0.001 && inGases.total() <= 0.001){
      remove();
    }
  }
  
  @Override
  public void remove(){
    if(this.added) {
      Groups.all.remove(this);
      this.added = false;
      Sgl.reactionPoints.remove(this);
      Groups.queueFree(this);
    }
  }
  
  @Override
  public void add(){
    if (!added) {
      Groups.all.add(this);
      added = true;
    }
  }
  
  @Override
  public boolean isLocal(){
    if(this instanceof Unitc){
      Unitc u = (Unitc) this;
      return u.controller() != Vars.player;
    }
    
    return true;
  }
  
  @Override
  public boolean isRemote(){
    if (this instanceof Unitc) {
      Unitc u = (Unitc)this;
      return u.isPlayer() && !this.isLocal();
    }
    return false;
  }
  
  @Override
  public void reset(){
    reaction = null;
    tile = null;
    id = EntityGroup.nextId();
    time = 0;
    lifetime = 0;
    heat = 0;
    inItems = null;
    inLiquids = null;
    inGases = null;
    outItems = null;
    outLiquids = null;
    outGases = null;
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
    return 30;
  }
  
  @Override
  public boolean serialize(){
    return true;
  }
  
  @Override
  public void id(int id){
    this.id = id;
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
  public void x(float x){
    this.x = x;
  }
  
  @Override
  public void y(float y){
    this.y = y;
  }
}
