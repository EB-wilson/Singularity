package singularity.world.blocks.gas;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.world.blocks.ItemSelection;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.type.SglContents;
import singularity.world.blockComp.GasBuildComp;

public class GasFilter extends GasJunction{
  public boolean through = true;
  
  public GasFilter(String name){
    super(name);
    configurable = true;
  }
  
  @Override
  public void appliedConfig(){
    config(Gas.class, (GasFilterBuild tile, Gas g) -> tile.gas = g);
    configClear((GasFilterBuild tile) -> tile.gas = null);
  }
  
  public class GasFilterBuild extends GasJunctionBuild{
    public Gas gas;
  
    @Override
    public GasBuildComp getGasDestination(GasBuildComp source, Gas gas){
      if(!enabled) return this;
  
      int dir = source.getBuilding().relativeTo(tile.x, tile.y);
      dir = (dir + 4) % 4;
      GasBuildComp next = nearby(dir) instanceof GasBuildComp? (GasBuildComp) nearby(dir) :null;
      int finalDir = dir;
      GasBuildComp other = (GasBuildComp) getDump(e -> e instanceof GasBuildComp &&
          ((nearby(Mathf.mod(finalDir + 1, 4)) == e || nearby(Mathf.mod(finalDir - 1, 4)) == e) && ((GasBuildComp) e).acceptGas(source, gas)));
      
      if(through){
        GasBuildComp temp = next;
        next = other;
        other = temp;
      }
      
      if(gas == this.gas && next != null && next.acceptGas(source, gas)){
        return next;
      }
      else if(other != null){
        return other;
      }
      else return this;
    }
  
    @Override
    public void draw(){
      super.draw();
    
      Draw.rect(region, x, y);
      if(gas == null){
        Draw.rect("cross", x, y);
      }else{
        Draw.color(gas.color);
        Draw.rect(Sgl.modName + "-gases_center", x, y);
        Draw.color();
      }
    }
    
    @Override
    public void buildConfiguration(Table table){
      ItemSelection.buildTable(table, SglContents.gases(), () -> gas, this::configure);
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
      return gas;
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      write.s(gas == null ? -1 : gas.id);
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      int id = revision == 1 ? read.s() : read.b();
      gas = id == -1 ? null : SglContents.gas(id);
    }
  }
}
