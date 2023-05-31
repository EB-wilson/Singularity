package singularity.world.blocks.drills;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.modules.ItemModule;
import singularity.world.blocks.SglBlock;
import singularity.world.meta.SglStat;
import singularity.world.modules.SglLiquidModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ChainsBlockComp;
import universecore.components.blockcomp.ChainsBuildComp;
import universecore.components.blockcomp.SpliceBlockComp;
import universecore.components.blockcomp.SpliceBuildComp;
import universecore.world.DirEdges;
import universecore.world.blocks.modules.ChainsModule;

@Annotations.ImplEntries
public class ExtendMiner extends SglBlock implements SpliceBlockComp {
  public ExtendableDrill master;
  public boolean negativeSplice;

  public Effect mining;
  public float miningEffectChance = 0.02f;

  public final Seq<ItemStack> ores = new Seq<>();

  public ExtendMiner(String name){
    super(name);
    update = true;
    hasItems = true;
    outputItems = true;
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(SglStat.componentBelongs, t -> {
      t.defaults().left();
      t.image(master.fullIcon).size(35).padRight(8);
      t.add(master.localizedName);
    });
  }

  @Override
  public int maxHeight(){
    return master.maxChainsHeight;
  }

  @Override
  public int maxWidth(){
    return master.maxChainsWidth;
  }

  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotate){
    if(isMultiblock()){
      boolean re = false;
      for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
        re |= master.canMine(other);
      }
      return re;
    }
    else{
      return master.canMine(tile);
    }
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    Tile tile = Vars.world.tile(x, y);
    if(tile == null) return;

    master.getMines(tile, this, false, ores);

    if(ores.size > 0){
      int line = 0;
      for(ItemStack stack: ores){
        float width = stack.item.hardness <= master.bitHardness?
            //可挖掘的矿物显示
            drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / (master.drillTime + stack.item.hardness*master.hardMultiple) * stack.amount, 2), x, y - line, true):
            //不可挖掘的矿物显示
            drawPlaceText(Core.bundle.get("bar.drilltierreq"), x, y - line, false);
        float dx = x * Vars.tilesize + offset - width/2f - 4f, dy = y * Vars.tilesize + offset + size * Vars.tilesize / 2f + 5 - line*8f;
        Draw.mixcol(Color.darkGray, 1f);
        Draw.rect(stack.item.uiIcon, dx, dy - 1);
        Draw.reset();
        Draw.rect(stack.item.uiIcon, dx, dy);
        line++;
      }
    }
  }

  @Override
  public void init(){
    super.init();
    master.validChildType.add(this);
    hasItems = master.hasItems;
    hasLiquids = master.hasLiquids;
    itemCapacity = master.itemCapacity;
    liquidCapacity = master.liquidCapacity;
  }

  @Override
  public boolean chainable(ChainsBlockComp other){
    return other == this || other == master;
  }

  @Override
  public boolean interCorner(){
    return false;
  }

  @Annotations.ImplEntries
  public class ExtendMinerBuild extends SglBuilding implements SpliceBuildComp {
    public int splice;
    public int spliceDirBits;
    public ChainsModule chains;
    public ExtendableDrill.ExtendableDrillBuild masterDrill;
    public Seq<ItemStack> mines = new Seq<>();
    public boolean updateSplice;
    public float warmup;

    @Override
    public void updateTile(){
      chains.container.update();
      if(masterDrill != null){
        for(ItemStack item: masterDrill.outputItems){
          dump(item.item);
        }
      }

      warmup = Mathf.lerpDelta(warmup, masterDrill == null? 0: masterDrill.warmup, master.warmupSpeed);

      if(mining != null && Mathf.chanceDelta(miningEffectChance*warmup)){
        mining.at(x, y);
      }

      if(updateSplice){
        updateSplice = false;
        splice(getSplice());
      }
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      chains = new ChainsModule(this);
      chains.newContainer();
      return this;
    }

    @Annotations.EntryBlocked
    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      master.getMines(tile, block, mines);

      updateSplice = true;
    }

    @Override
    public Seq<Item> outputItems(){
      return masterDrill.outputItems();
    }

    @Override
    public void onChainsUpdated(){
      masterDrill = null;
      items = new ItemModule();
      liquids = new SglLiquidModule();
      for(ChainsBuildComp comp: chains.container.all){
        if(comp instanceof ExtendableDrill.ExtendableDrillBuild ext && ext.block == master){
          if(masterDrill != null){
            masterDrill = null;
            break;
          }
          masterDrill = ext;
          items = masterDrill.items;
          liquids = masterDrill.liquids;
        }
      }
    }

    @Override
    public boolean acceptItem(Building source, Item item){
      return masterDrill != null && masterDrill.acceptItem(source, item);
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return masterDrill != null && masterDrill.acceptLiquid(source, liquid);
    }

    @Override
    public boolean canChain(ChainsBuildComp other){
      if(!SpliceBuildComp.super.canChain(other)) return false;
      Building oth = null;
      t: for(int i = 0; i < 4; i++){
        if(oth != null) break;
        for(Point2 point: DirEdges.get(size, i)){
          if(oth == null){
            oth = nearby(point.x, point.y);
            if(oth != other){
              oth = null;
              continue t;
            }
          }
          else if(oth != nearby(point.x, point.y)){
            oth = null;
            break t;
          }
        }
      }
      return oth != null;
    }

    @Override
    public int splice(){
      return splice;
    }

    @Override
    public void splice(int arr){
      splice = arr;
      spliceDirBits = 0;
      for(int i = 0; i < 4; i++){
        if ((splice & 1 << i*2) != 0) spliceDirBits |= 1 << i;
      }
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.f(warmup);
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      warmup = read.f();
    }
  }
}
