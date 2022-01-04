package singularity.world.blocks.distribute;

import arc.struct.ObjectMap;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.world.blockComp.distributeNetwork.DistComponent;
import singularity.world.blockComp.distributeNetwork.DistNetworkCoreComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.modules.DistCoreModule;

public class DistNetCore extends DistNetBlock implements DistComponent{
  public ObjectMap<DistBuffers<?>, Integer> bufferSize = DistBuffers.defBufferCapacity;
  public int computingPower = 32;
  public int frequencyOffer = 8;

  public DistNetCore(String name){
    super(name);
    frequencyUse = 0;
  }
  
  @Override
  public ObjectMap<DistBuffers<?>, Integer> bufferSize(){
    return bufferSize;
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
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      distributor = new DistCoreModule(this);
      return this;
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
