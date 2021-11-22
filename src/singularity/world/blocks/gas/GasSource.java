package singularity.world.blocks.gas;

import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.world.blocks.ItemSelection;
import singularity.type.Gas;
import singularity.type.SglContents;

public class GasSource extends GasBlock{
  public GasSource(String name){
    super(name);
    solid = true;
    gasCapacity = 10f;
    maxGasPressure = 100f;
    configurable = true;
    outputGases = true;
    saveConfig = true;
    noUpdateDisabled = true;
    compressProtect = true;
  }
  
  @Override
  public void appliedConfig(){
    config(Gas.class, (GasSourceBuild tile, Gas g) -> tile.source = g);
    configClear((GasSourceBuild tile) -> tile.source = null);
  }
  
  @Override
  public void setBars(){
    super.setBars();
    
    bars.remove("gasPressure");
  }
  
  @Override
  public void drawRequestConfig(BuildPlan req, Eachable<BuildPlan> list){
    drawRequestConfigCenter(req, req.config, "center", true);
  }
  
  public class GasSourceBuild extends SglBuilding{
    public Gas source = null;
    
    @Override
    public void updateTile(){
      gases.clear();
      if(source == null){
        Seq<Gas> allGases = SglContents.gases();
        for(Gas gas: allGases){
          gases.set(gas, gasCapacity*((float) 1/allGases.size));
        }
        dumpGas();
      }else{
        gases.set(source, gasCapacity);
        dumpGas(source);
      }
    }
  
    @Override
    public float pressure(){
      return 100;
    }
  
    @Override
    public void draw(){
      super.draw();
      
      Draw.rect(region, x, y);
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
      ItemSelection.buildTable(table, SglContents.gases(), () -> source, this::configure);
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
      source = id == -1 ? null : SglContents.gas(id);
    }
  }
}
