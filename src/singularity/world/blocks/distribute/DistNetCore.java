package singularity.world.blocks.distribute;

import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.world.components.distnet.DistComponent;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.modules.DistCoreModule;

public class DistNetCore extends DistNetBlock{
  public int computingPower = 32;
  public int frequencyOffer = 8;

  public DistNetCore(String name){
    super(name);
    frequencyUse = 0;
    hasItems = true;
    hasLiquids = true;
    hasGases = true;
  }
  
  public class DistNetCoreBuild extends DistNetBuild implements DistNetworkCoreComp, DistComponent{
    DistCoreModule distCore;

    @Override
    public int computingPower(){
      return computingPower;
    }

    @Override
    public int frequencyOffer(){
      return frequencyOffer;
    }

    @Override
    public Building create(Block block, Team team){
      distCore = new DistCoreModule(this);
      super.create(block, team);
      items = distCore.getBuffer(DistBuffers.itemBuffer).generateBindModule();
      return this;
    }
  
    @Override
    public DistCoreModule distCore(){
      return distCore;
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      updateDistNetwork();
    }
  }
}
