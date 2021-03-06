package singularity.world.blocks.gas;

import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
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
import singularity.Sgl;
import singularity.type.Gas;
import singularity.type.SglContents;
import singularity.world.blocks.SglBlock;
import singularity.world.components.GasBuildComp;
import singularity.world.meta.SglBlockGroup;
import singularity.world.modules.GasesModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.Takeable;

/**液体提取器，可连通周围方块的气体槽，并送向下一个方块
 * 类似物品装卸器，气体压力来自目标方块
 * @see mindustry.world.blocks.storage.Unloader*/
public class GasUnloader extends GasBlock{
/**液体提取器，可从周围的方块中抽取液体并送向下一个方块*/
  public GasUnloader(String name){
    super(name);
    update = true;
    solid = true;
    unloadable = false;
    configurable = true;
    outputGases = true;
    saveConfig = true;
    displayFlow = false;
    compressProtect = true;
    group = SglBlockGroup.gas;
  }
  
  @Override
  public void appliedConfig(){
    config(Gas.class, (GasUnloaderBuild tile, Gas gas) -> tile.current = gas);
    configClear((GasUnloaderBuild tile) -> tile.current = null);
  }
  
  @Override
  public void setBars(){
    super.setBars();
    bars.remove("gasPressure");
  }

  @Override
  public void drawRequestConfig(BuildPlan req, Eachable<BuildPlan> list){
    drawRequestConfigCenter(req, req.config, "center");
  }
  
  @Annotations.ImplEntries
  public class GasUnloaderBuild extends SglBlock.SglBuilding implements Takeable{
    public @Nullable Gas current = null;
    public GasesModule tempGases;
  
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      tempGases = gases;
      return this;
    }
  
    @Override
    public void updateTile(){
      Building next = getNext("gases", e -> {
        if(!(e instanceof GasBuildComp)) return  false;
        return ((GasBuildComp) e).getGasBlock().hasGases() && e.canUnload();
      });
      if(next != null){
        gases = ((GasBuildComp)next).gases();
      }
      else gases = tempGases;
      
      if(gases != tempGases){
        if(current != null){
          dumpGas(current);
        }
        else dumpGas();
      }
    }
  
    @Override
    public void dumpGas(){
      GasBuildComp other = (GasBuildComp)getNext("gases", e -> {
        if(!(e instanceof GasBuildComp)) return false;
        return ((GasBuildComp)e).getGasBlock().hasGases() && ((GasBuildComp)e).gases() != gases;
      });
      if(other != null) moveGas(other);
    }
  
    @Override
    public void draw(){
      Draw.rect(region, x, y);
      
      if(current != null){
        Draw.color(current.color);
        Draw.rect(Sgl.modName + "-gases_center", x, y);
        Draw.color();
      }
    }

    @Override
    public void buildConfiguration(Table table){
      ItemSelection.buildTable(table, SglContents.gases(), () -> current, this::configure);
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
    public Gas config(){
      return current;
    }
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return false;
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
      current = id == -1 ? null : SglContents.gas(id);
    }
  }
}
