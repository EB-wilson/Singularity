package singularity.world.blocks.distribute.netcomponents;

import arc.struct.ObjectMap;
import mindustry.gen.Building;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.components.distnet.DistComponent;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.DistBuffers;

public class CoreNeighbourComponent extends DistNetBlock{
  public int frequencyOffer = 0;
  public int computingPower = 0;

  public ObjectMap<DistBuffers<?>, Integer> bufferSize = new ObjectMap<>();

  public CoreNeighbourComponent(String name){
    super(name);
    frequencyUse = 0;
  }

  public class CoreNeighbourComponentBuild extends DistNetBuild implements DistComponent{
    @Override
    public ObjectMap<DistBuffers<?>, Integer> bufferSize(){
      return bufferSize;
    }

    @Override
    public int frequencyOffer(){
      return frequencyOffer;
    }

    @Override
    public int computingPower() {
      return computingPower;
    }

    @Override
    public boolean componentValid(){
      for(Building building: proximity){
        if(building instanceof DistNetworkCoreComp core && distributor.network.getCore() == core) return true;
      }
      return false;
    }

    @Override
    public boolean linkable(DistElementBuildComp other){
      return false;
    }

    @Override
    public void updateNetLinked(){
      super.updateNetLinked();
      for(Building building: proximity){
        if(building instanceof DistNetworkCoreComp core){
          netLinked.add(core);
        }
      }
    }
  }
}
