package singularity.world.blocks.distribute;

import mindustry.gen.Building;

public class DistNetConsModule extends DistNetBlock{
  public DistNetConsModule(String name){
    super(name);
  }

  public class DistNetConsModuleBuild extends DistNetBuild{
    @Override
    public void updateNetLinked(){
      netLinked.clear();
      for(Building other: proximity){
        if(other instanceof DistNetConsEntry.DistNetConsEntryBuild entry){
          netLinked.add(entry);
        }
      }
    }

    @Override
    public void onProximityAdded(){
      super.onProximityAdded();
      updateNetLinked();
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();
      updateNetLinked();
    }

    @Override
    public void onProximityRemoved(){
      super.onProximityRemoved();
      updateNetLinked();
    }
  }
}
