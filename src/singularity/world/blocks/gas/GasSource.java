package singularity.world.blocks.gas;

import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import arc.util.Eachable;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.meta.BlockGroup;
import singularity.type.Gas;
import singularity.type.SglContentType;
import singularity.world.blocks.SglBlock;

import static mindustry.Vars.content;

public class GasSource extends SglBlock{
  public GasSource(String name){
    super(name);
    update = true;
    solid = true;
    hasGases = true;
    gasCapacity = 100f;
    configurable = true;
    outputGases = true;
    saveConfig = true;
    noUpdateDisabled = true;
    
    config(Gas.class, (GasSourceBuild tile, Gas g) -> tile.source = g);
    configClear((GasSourceBuild tile) -> tile.source = null);
  }
  
  @Override
  public void setBars(){
    super.setBars();
    
    bars.remove("liquid");
  }
  
  @Override
  public void drawRequestConfig(BuildPlan req, Eachable<BuildPlan> list){
    drawRequestConfigCenter(req, req.config, "center", true);
  }
  
  public class GasSourceBuild extends SglBuilding{
    public Gas source = null;
    
    @Override
    public void updateTile(){
      if(source == null){
        gases.clear();
      }else{
        gases.add(source, liquidCapacity);
        dumpGas(source);
      }
    }
    
    @Override
    public void draw(){
      super.draw();
      
      if(source == null){
        Draw.rect("cross", x, y);
      }else{
        Draw.color(source.color);
        Draw.rect("center", x, y);
        Draw.color();
      }
    }
    
    @Override
    public void buildConfiguration(Table table){
      ItemSelection.buildTable(table, content.getBy(SglContentType.gas.value), () -> source, this::configure);
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
      return source;
    }
    
    @Override
    public byte version(){
      return 1;
    }
    
    @Override
    public void write(Writes write){
      super.write(write);
      write.s(source == null ? -1 : source.id);
    }
    
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      int id = revision == 1 ? read.s() : read.b();
      source = id == -1 ? null : content.getByID(SglContentType.gas.value, id);
    }
  }
}
