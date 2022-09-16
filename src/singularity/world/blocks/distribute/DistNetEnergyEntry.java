package singularity.world.blocks.distribute;

import singularity.world.components.NuclearEnergyBuildComp;
import singularity.world.components.distnet.DistElementBuildComp;

public class DistNetEnergyEntry extends DistNetConsEntry{

  public DistNetEnergyEntry(String name){
    super(name);

    hasEnergy = true;
    consumeEnergy = true;
  }

  public class DistNetEnergyEntryBuild extends DistNetConsEntryBuild{
    @Override
    public void networkRemoved(DistElementBuildComp remove){
      if(remove != this || !distributor.network.netStructValid() || !(distributor.network.getCore() instanceof NuclearEnergyBuildComp core)) return;

      core.link(this);
    }

    @Override
    public void networkUpdated(){
      if(!distributor.network.netStructValid() || !(distributor.network.getCore() instanceof NuclearEnergyBuildComp core)) return;

      core.deLink(this);
    }
  }
}
