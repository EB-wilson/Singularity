package singularity.world.blockComp.distributeNetwork;

import arc.struct.Seq;
import singularity.world.modules.DistributeModule;
import universeCore.entityComps.blockComps.BuildCompBase;

public interface DistElementBuildComp extends BuildCompBase{
  DistributeModule distributor();
  
  int priority();
  
  Seq<DistElementBuildComp> getLinked();
  
  default DistElementBlockComp getDistBlock(){
    return getBlock(DistElementBlockComp.class);
  }
}
