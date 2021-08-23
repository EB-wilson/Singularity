package singularity.world.blocks.liquid;

import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import arc.util.Eachable;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.meta.BlockGroup;

import java.util.ArrayList;

import static mindustry.Vars.content;

/**液体提取器，可从周围的方块中抽取液体并送向下一个方块
 * 类似物品装卸器
 * @see mindustry.world.blocks.storage.Unloader*/
public class LiquidUnloader extends Block{
  /**提取速度，数值*60为每秒提取量*/
  public float speed = 0.5f;
  ArrayList<Building> unloading = new ArrayList<>();
  ArrayList<Building> outputs = new ArrayList<>();

/**液体提取器，可从周围的方块中抽取液体并送向下一个方块*/
  public LiquidUnloader(String name){
    super(name);
    update = true;
    solid = true;
    unloadable = false;
    hasLiquids = true;
    liquidCapacity = 0f;
    configurable = true;
    outputsLiquid = true;
    saveConfig = true;
    displayFlow = false;
    group = BlockGroup.liquids;

    config(Liquid.class, (LiquidUnloaderBuild tile, Liquid l) -> tile.current = l);
    configClear((LiquidUnloaderBuild tile) -> tile.current = null);
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

  public class LiquidUnloaderBuild extends Building{
    public @Nullable Liquid current = null;
    public int from = 0, to = 0;

    @Override
    public void updateTile(){
      if(current != null){
        unloading.clear();
        outputs.clear();
        
        for(int i=0; i<proximity.size; i++){
          Building curr = proximity.get(i);
          if(curr.team == this.team && curr.block.unloadable && curr.block.hasLiquids){
            unloading.add(curr);
          }
          if(curr.team == this.team && curr.block.hasLiquids && curr.acceptLiquid(this, current)){
            outputs.add(curr);
          }
        }
        
        if(unloading.size() == 0 || outputs.size() == 0) return;
        from %= unloading.size();
        to %= outputs.size();
        if(unloading.get(from) == outputs.get(to)){
          to++;
          return;
        }
        
        Building liFrom = unloading.get(from);
        Building liTo = outputs.get(to);
        
        float trans = Math.min(liTo.block.liquidCapacity - liTo.liquids.get(current), Math.min(speed, liFrom.liquids.get(current)));
        liFrom.liquids.remove(current, trans);
        liTo.liquids.add(current, trans);
        
        from++;
        to++;
      }
      else{
        from = to = 0;
      }
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
