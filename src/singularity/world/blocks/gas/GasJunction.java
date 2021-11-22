package singularity.world.blocks.gas;

import arc.graphics.g2d.Draw;
import singularity.type.Gas;
import singularity.world.blockComp.GasBuildComp;

public class GasJunction extends GasBlock{
  public GasJunction(String name){
    super(name);
    outputGases = true;
    hasPower = false;
    showGasFlow = false;
    compressProtect = true;
  }
  
  @Override
  public void setBars(){
    super.setBars();
    bars.remove("gasPressure");
  }
  
  public class GasJunctionBuild extends SglBuilding{
    @Override
    public void draw(){
      Draw.rect(region, x, y);
    }
  
    @Override
    public GasBuildComp getGasDestination(GasBuildComp source, Gas gas){
      if(!enabled) return this;
  
      int dir = source.getBuilding().relativeTo(tile.x, tile.y);
      dir = (dir + 4) % 4;
      GasBuildComp next = nearby(dir) instanceof GasBuildComp? (GasBuildComp) nearby(dir) :null;
      if(next == null || (!next.acceptGas(this, gas) && !(next.getBlock() instanceof GasJunction))){
        return this;
      }
      return next.getGasDestination(this, gas);
    }
  }
}
