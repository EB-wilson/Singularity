package singularity.world.blocks.gas;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.world.blocks.ItemSelection;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.type.SglContents;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.modules.GasesModule;

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
    public int outCount;
  
    @Override
    public float moveGas(GasBuildComp other){
      if(!other.getGasBlock().hasGases()) return 0;
      int index = getIndex(other);
      GasesModule gases = gasesBuffer[index];
      GasBuildComp sideL = (GasBuildComp) nearby(Mathf.mod(index + 1, 4)), sideR = (GasBuildComp) nearby(Mathf.mod(index - 1, 4));
    
      float total = gases.total();
      float[] flow = {0};
      
      gases.each(stack -> {
        float present = gases.get(stack.gas)/total;
        GasBuildComp out, oth;
        
        out = other;
        if(sideL != null && sideR != null){
          oth = outCount%2 == 0? sideL: sideR;
        }
        else oth = sideL != null? sideL: sideR;
        
        if(through){
          GasBuildComp tmp = out;
          out = oth;
          oth = tmp;
        }
        
        GasBuildComp dest;
        if(stack.gas == gas && out != null && out.acceptGas(this, stack.gas)){
          dest = out;
        }
        else if(oth != null && oth.acceptGas(this, stack.gas)){
          dest = oth;
        }
        else return;
        
        float otherTotal = dest.gases().total();
  
        float fract = (gases.getPressure() - dest.pressure())/Math.max(getGasBlock().maxGasPressure(), dest.getGasBlock().maxGasPressure());
        float flowRate = Math.min(fract*getGasBlock().maxGasPressure()*getGasBlock().gasCapacity(), total);
        flow[0] += flowRate = Math.min(flowRate, dest.getGasBlock().gasCapacity()*dest.getGasBlock().maxGasPressure() - otherTotal)*present;
  
        dest.handleGas(this, stack.gas, flowRate);
        gases.remove(stack.gas, flowRate);
        dest.onMoveGasThis(this, Seq.with(new GasStack(stack.gas, flowRate)));
      });
      
      outCount++;
      return flow[0];
    }
  
    @Override
    public void draw(){
      super.draw();
    
      Draw.rect(region, x, y);
      if(gas == null){
        Draw.rect("cross", x, y);
      }else{
        Draw.color(gas.color);
        Draw.rect("center", x, y);
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
