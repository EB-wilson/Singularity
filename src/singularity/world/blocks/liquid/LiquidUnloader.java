package singularity.world.blocks.liquid;

import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.util.Eachable;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.meta.BlockGroup;
import mindustry.world.modules.LiquidModule;
import universeCore.annotations.Annotations;
import universeCore.entityComps.blockComps.Takeable;

import static mindustry.Vars.content;

/**液体提取器，可从周围的方块中抽取液体并送向下一个方块
 * 类似物品装卸器
 * @see mindustry.world.blocks.storage.Unloader*/
public class LiquidUnloader extends Block{
  /**液体提取器，可从周围的方块中抽取液体并送向下一个方块*/
  public LiquidUnloader(String name){
    super(name);
    update = true;
    solid = true;
    unloadable = false;
    hasLiquids = true;
    liquidCapacity = 10f;
    configurable = true;
    outputsLiquid = true;
    saveConfig = true;
    displayFlow = false;
    group = BlockGroup.liquids;

    config(Liquid.class, (LiquidUnloadedBuild tile, Liquid l) -> tile.current = l);
    configClear((LiquidUnloadedBuild tile) -> tile.current = null);
  }

  @Override
  public void setBars(){
    super.setBars();
    bars.remove("liquid");
  }

  @Override
  public void drawRequestConfig(BuildPlan req, Eachable<BuildPlan> list){
    drawRequestConfigCenter(req, req.config, "center");
  }

  @Annotations.ImplEntries
  public class LiquidUnloadedBuild extends Building implements Takeable{
    public @Nullable Liquid current = null;
    
    public LiquidModule tempLiquid = new LiquidModule();
    public ObjectMap<String, Heaps<?>> dumps = new ObjectMap<>();
  
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      tempLiquid = liquids;
      return this;
    }
  
    @Override
    public void updateTile(){
      Building next = getNext("liquids" , e -> e.block.hasLiquids && e.canUnload());
      if(next != null){
        liquids = next.liquids;
      }
      else liquids = tempLiquid;
      
      if(liquids != tempLiquid){
        if(current != null){
          dumpLiquid(current);
        }
        else liquids.each((l, a) -> dumpLiquid(l));
      }
    }
  
    @Override
    public boolean canDumpLiquid(Building to, Liquid liquid){
      return super.canDumpLiquid(to, liquid) && to.liquids != liquids;
    }
  
    @Override
    public void draw(){
      Draw.rect(region, x, y);
      
      if(current != null){
        Draw.color(current.color);
        Draw.rect(name + "_top", x, y);
        Draw.color();
      }
    }

    @Override
    public void buildConfiguration(Table table){
      ItemSelection.buildTable(table, content.liquids(), () -> current, this::configure);
    }

    @Override
    public boolean onConfigureTileTapped(Building other){
      if(this == other){
        deselect();
        configure(null);
        return false;
      }
      return true;
    }

    @Override
    public Liquid config(){
      return current;
    }
    
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return false;
    }
    
    @Override
    public boolean acceptItem(Building source, Item item){
      return false;
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.s(current == null ? -1 : current.id);
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      int id = revision == 1 ? read.s() : read.b();
      current = id == -1 ? null : content.liquid(id);
    }
  }
}
