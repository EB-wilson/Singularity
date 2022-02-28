package singularity.world.blocks.distribute;

import singularity.world.blockComp.distributeNetwork.DistComponent;
import singularity.world.blockComp.distributeNetwork.DistNetworkCoreComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.modules.DistCoreModule;

public class DistNetCore extends DistNetBlock implements DistComponent{
  public int computingPower = 32;
  public int frequencyOffer = 8;

  public DistNetCore(String name){
    super(name);
    frequencyUse = 0;
    hasItems = true;
    hasLiquids = true;
    hasGases = true;
  }
  
  @Override
  public int computingPower(){
    return computingPower;
  }
  
  @Override
  public int frequencyOffer(){
    return frequencyOffer;
  }
  
  public class DistNetCoreBuild extends DistNetBuild implements DistNetworkCoreComp{
    protected float updateCounter;
  
    @Override
    public void assignNetModule(){
      distributor = new DistCoreModule(this);
      distributor.setNet();
      items = distributor().getBuffer(DistBuffers.itemBuffer).generateBindModule();
    }
  
    @Override
    public DistCoreModule distributor(){
      return (DistCoreModule) distributor;
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      updateDistNetwork();
    }
  }
}
