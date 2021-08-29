package singularity.world.blocks.gas;

import mindustry.world.meta.BlockGroup;
import singularity.type.Gas;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blocks.SglBlock;

public class GasVoid extends SglBlock{
  public GasVoid(String name){
    super(name);
    hasGases = true;
    solid = true;
    update = true;
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
