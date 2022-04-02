package singularity.world.blocks.gas;

import singularity.type.Gas;
import singularity.world.components.GasBuildComp;

public class GasVoid extends GasBlock{
  public GasVoid(String name){
    super(name);
    solid = true;
  }
  
  @Override
  public void setBars(){
    super.setBars();
    bars.remove("gasPressure");
  }
  
  public class LiquidVoidBuild extends SglBuilding{
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return enabled;
    }
    
    @Override
    public void handleGas(GasBuildComp source, Gas gas, float amount){
    }
  
    @Override
    public float pressure(){
      return 0;
    }
  }
}
