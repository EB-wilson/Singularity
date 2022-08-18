package singularity.world.reaction;

import arc.math.geom.Position;
import arc.util.Log;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pool;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.EntityGroup;
import mindustry.entities.Puddles;
import mindustry.gen.Building;
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
import singularity.world.components.GasBlockComp;
import singularity.world.components.HeatBlockComp;
import singularity.world.modules.GasesModule;
import singularity.world.modules.ReactionModule;
import singularity.world.modules.SglLiquidModule;

import static singularity.Sgl.atmospheres;

@SuppressWarnings("unchecked")
public class ReactionPoint implements Entityc, Pool.Poolable, ReactContainer{
  public Tile tile;
  
  public boolean added;
  public transient int id = EntityGroup.nextId();
  public float x, y;
  
  public float time;
  public float lifetime = 60f;
  public float heatCapacity;
  
  public float heat = 0;
  
  public ReactionModule reacts;
  
  public static HeatBlockComp heatBlock = new HeatBlockComp(){
    @Override
    public float heatCoefficient(){
      return 10;
    }
  
    @Override
    public float blockHeatCoff(){
      return 0;
    }
  
    @Override
    public float baseHeatCapacity(){
      return 1;
    }
  
    @Override
    public float maxTemperature(){
      return 10;
    }
  };
  
  @SuppressWarnings("all")
  public static GasBlockComp gasBlock = new GasBlockComp(){
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
      return 100;
    }
  
    @Override
    public boolean compressProtect(){
      return false;
    }
  };
  
  public ItemModule items;
  public SglLiquidModule liquids;
  public GasesModule gases;
  
  private ReactionPoint(){
    reacts = new ReactionModule(this);
    
    setModules();
  }
  
  public static ReactionPoint create(){
    return new ReactionPoint();
  }
  
  public void set(Tile tile){
    this.tile = tile;
    set(tile.drawx(), tile.drawy());
  }
  
  public void addMaterial(UnlockableContent input, float amount){
    if(input instanceof Item) items.add((Item)input, (int)amount);
    if(input instanceof Liquid) liquids.add((Liquid)input, amount);
    if(input instanceof Gas) gases.add((Gas)input, amount);
    reacts.matchAll(input);
    
    heat += getHeat(input)*amount;
  }
  
  @Override
  public void setModules(){
    setItemModule(i -> items = i);
    setLiquidModule(l -> liquids = (SglLiquidModule) l);
    setGasesModule(g -> gases = g);

    heat(atmospheres.current.getTemperature()*heatCapacity());
  }
  
  @Override
  public HeatBlockComp getHeatBlock(){
    return heatBlock;
  }
  
  @Override
  public GasBlockComp getGasBlock(){
    return gasBlock;
  }
  
  @Override
  public void read(Reads reads){}
  
  @Override
  public void write(Writes writes){}
  
  @Override
  public void afterRead(){}
  
  @Override
  public int id(){
    return id;
  }
  
  @Override
  public boolean isAdded(){
    return added;
  }
  
  @Override
  public float heatCapacity(){
    return heatCapacity;
  }
  
  @Override
  public void heatCapacity(float value){
    heatCapacity = value;
  }
  
  @Override
  public void heat(float heat){
    this.heat = heat;
  }
  
  @Override
  public Tile tile(){
    return tile;
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
  public float y(){
    return y;
  }
  
  @Override
  public ReactionModule reacts(){
    return reacts;
  }
  
  @Override
  public ItemModule items(){
    return items;
  }
  
  @Override
  public LiquidModule liquids(){
    return liquids;
  }
  
  @Override
  public GasesModule gases(){
    return gases;
  }
  
  @Override
  public float heat(){
    return heat;
  }
  
  @Override
  public float pressure(){
    return Sgl.atmospheres.current.getCurrPressure();
  }
  
  @Override
  public float getX(){
    return x;
  }
  
  @Override
  public void update(){
    reacts.update();
    
    if(liquids.total() > 0.001){
      liquids.each((liquid, amount) -> {
        Puddles.deposit(tile, liquid, amount/2);
        liquids.remove(liquid, amount/2);
      });
    }
    
    if(gases.total() > 0.001){
      gases.each((gas, amount) -> {
        Sgl.gasAreas.pour(tile, gas, amount/2);
        gases.remove(gas, amount/2);
      });
    }
    
    time = Math.min(time + Time.delta, lifetime);
    if(!reacts.any() && time >= lifetime && items.empty() && liquids.total() <= 0.001 && gases.total() <= 0.001){
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
  public void reset(){
    tile = null;
    id = EntityGroup.nextId();
    time = 0;
    lifetime = 0;
    heat = 0;
    items = null;
    liquids = null;
    gases = null;
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
    return 101;
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
  public Building buildOn(){
    return tile.build;
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
