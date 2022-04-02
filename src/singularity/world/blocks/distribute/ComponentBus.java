package singularity.world.blocks.distribute;

import singularity.world.components.distnet.DistComponent;

public class ComponentBus extends DistNetBlock{
  public ComponentBus(String name){
    super(name);
  }

  public class ComponentBusBuild extends DistNetBuild implements DistComponent{

  }

}
