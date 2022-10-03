package singularity.world.blocks.drills;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.geom.Point2;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.modules.ItemModule;
import singularity.world.DirEdges;
import singularity.world.blocks.SglBlock;
import singularity.world.components.ChainsBlockComp;
import singularity.world.components.ChainsBuildComp;
import singularity.world.modules.ChainsModule;
import singularity.world.modules.SglLiquidModule;
import universecore.annotations.Annotations;

public class ExtendMiner extends SglBlock implements ChainsBlockComp{
  public ExtendableDrill master;

  public final Seq<ItemStack> ores = new Seq<>();

  public ExtendMiner(String name){
    super(name);
    update = true;
    hasItems = true;
    outputItems = true;
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
    itemCapacity = master.itemCapacity;
    liquidCapacity = master.liquidCapacity;
  }

  @Override
  public boolean chainable(ChainsBlockComp other){
    return other == this || other == master;
  }

  @Annotations.ImplEntries
  public class ExtendMinerBuild extends SglBuilding implements ChainsBuildComp{
    public ChainsModule chains;
    public ExtendableDrill.ExtendableDrillBuild masterDrill;
    public Seq<ItemStack> mines = new Seq<>();

    int chainsBits;

    @Override
    public void updateTile(){
      chains.container.update();
      if(masterDrill != null){
        for(ItemStack item: masterDrill.outputItems){
          dump(item.item);
        }
      }
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      chains = new ChainsModule(this);
      chains.newContainer();
      return this;
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      master.getMines(tile, block, mines);

      for(int i = 0; i < 4; i++){
        Building b = nearby(DirEdges.get(size, i)[0].x, DirEdges.get(size, i)[0].y);
        if(b instanceof ChainsBuildComp oth && canChain(oth)) chainsBits |= 0b0001 << i;
      }
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
      if(!ChainsBuildComp.super.canChain(other)) return false;
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
  }
}