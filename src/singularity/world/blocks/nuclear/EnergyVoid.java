package singularity.world.blocks.nuclear;

import arc.scene.ui.layout.Table;
import mindustry.world.meta.Env;
import singularity.world.components.NuclearEnergyBuildComp;

public class EnergyVoid extends NuclearBlock {
  public EnergyVoid(String name){
    super(name);
    energyCapacity = 1024;
    outputEnergy = false;
    consumeEnergy = true;
    noUpdateDisabled = true;
    envEnabled = Env.any;
  }
  
  public class EnergyVoidBuild extends SglBuilding {
    @Override
    public void handleEnergy(float value){
    }
  
    @Override
    public void displayEnergy(Table table){
    }
  
    @Override
    public boolean acceptEnergy(NuclearEnergyBuildComp source){
      return true;
    }
  }
}
