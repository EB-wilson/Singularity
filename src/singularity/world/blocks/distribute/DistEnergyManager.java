package singularity.world.blocks.distribute;

import mindustry.gen.Building;

public class DistEnergyManager extends DistNetBlock{
  public DistEnergyManager(String name){
    super(name);

    isNetLinker = true;
  }

  public class DistEnergyManagerBuild extends DistNetBuild{
    @Override
    public void updateNetLinked(){
      super.updateNetLinked();
      for(Building building: proximity){
        if(building instanceof DistEnergyEntry.DistEnergyEntryBuild entry){
          netLinked.add(entry);
        }
      }
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      updateNetLinked();
    }
  }
}
