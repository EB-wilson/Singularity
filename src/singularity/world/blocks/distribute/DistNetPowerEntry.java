package singularity.world.blocks.distribute;

import mindustry.gen.Building;
import mindustry.world.blocks.power.PowerGraph;
import singularity.world.components.distnet.DistElementBuildComp;

public class DistNetPowerEntry extends DistNetConsEntry{
  public DistNetPowerEntry(String name){
    super(name);

    hasPower = true;
    consumesPower = true;
  }

  public class DistNetPowerEntryBuild extends DistNetConsEntryBuild{
    @Override
    public void networkRemoved(DistElementBuildComp remove){
      if(remove != this || !distributor.network.netStructValid() || !(distributor.network.getCore() instanceof Building core)) return;

      core.power.links.add(pos());
      power.links.add(core.pos());

      new PowerGraph().reflow(this);
      new PowerGraph().reflow(core);
    }

    @Override
    public void networkUpdated(){
      if(!distributor.network.netStructValid() || !(distributor.network.getCore() instanceof Building core)) return;

      core.power.links.add(pos());
      power.links.add(core.pos());

      new PowerGraph().reflow(core);
    }
  }
}
