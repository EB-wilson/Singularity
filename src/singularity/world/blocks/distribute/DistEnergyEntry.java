package singularity.world.blocks.distribute;

import mindustry.gen.Building;
import singularity.world.components.distnet.DistElementBuildComp;

public class DistEnergyEntry extends DistNetBlock{
  public DistEnergyEntry(String name){
    super(name);

    frequencyUse = 0;
  }

  public class DistEnergyEntryBuild extends DistNetBuild{
    public DistEnergyManager.DistEnergyManagerBuild ownerManager;

    @Override
    public boolean linkable(DistElementBuildComp other){
      return false;
    }

    @Override
    public void onProximityAdded(){
      ownerManager = null;
      for(Building building: proximity){
        if(building instanceof DistEnergyManager.DistEnergyManagerBuild manager){
          manager.distributor.network.add(this);
          ownerManager = manager;
        }
      }
    }
  }
}
