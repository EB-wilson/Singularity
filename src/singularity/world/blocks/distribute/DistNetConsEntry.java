package singularity.world.blocks.distribute;

import mindustry.gen.Building;
import singularity.world.components.distnet.DistElementBuildComp;

public class DistNetConsEntry extends DistNetBlock{

  public DistNetConsEntry(String name){
    super(name);

    frequencyUse = 0;
  }

  public class DistNetConsEntryBuild extends DistNetBuild{
    @Override
    public void link(DistElementBuildComp target){}

    @Override
    public void linked(DistElementBuildComp target){
      super.linked(target);

      deLink(target);
    }

    @Override
    public void onProximityAdded(){
      super.onProximityAdded();

      for(Building building: proximity){
        if(building instanceof DistNetConsModule.DistNetConsModuleBuild module){
          module.distributor.network.add(distributor.network);
        }
      }
    }
  }
}
