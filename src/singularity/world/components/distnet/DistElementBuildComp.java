package singularity.world.components.distnet;

import arc.struct.IntSeq;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.modules.DistributeModule;
import universecore.components.blockcomp.BuildCompBase;

public interface DistElementBuildComp extends BuildCompBase{
  DistributeModule distributor();
  
  int priority();
  
  void priority(int priority);
  
  Seq<DistElementBuildComp> netLinked();
  
  default void networkValided(){}
  
  default DistElementBlockComp getDistBlock(){
    return getBlock(DistElementBlockComp.class);
  }
  
  default void link(DistElementBuildComp target){
    distributor().distNetLinks.add(target.getBuilding().pos());
    target.distributor().distNetLinks.add(getBuilding().pos());
  
    updateNetLinked();
    target.updateNetLinked();
  
    distributor().network.add(target.distributor().network);
  }
  
  default void deLink(DistElementBuildComp target){
    distributor().distNetLinks.removeValue(target.getBuilding().pos());
    target.distributor().distNetLinks.removeValue(getBuilding().pos());
  
    updateNetLinked();
    target.updateNetLinked();
    
    new DistributeNetwork().flow(this);
    new DistributeNetwork().flow(target);
  }
  
  default void updateNetLinked(){
    netLinked().clear();
    for(int i=0; i<distributor().distNetLinks.size; i++){
      Tile entity = Vars.world.tile(distributor().distNetLinks.get(i));
      if(entity == null || !(entity.build instanceof DistElementBuildComp)) continue;
      if(!netLinked().contains((DistElementBuildComp) entity.build)) netLinked().add((DistElementBuildComp) entity.build);
    }
  }
  
  default void onDistNetRemoved(){
    IntSeq links = distributor().distNetLinks;
    for(int i=0; i<links.size; i++){
      Building other = Vars.world.build(links.get(i));
      if(other instanceof DistElementBuildComp){
        ((DistElementBuildComp) other).distributor().distNetLinks.removeValue(getBuilding().pos());
        ((DistElementBuildComp) other).updateNetLinked();
      }
    }
    links.clear();
    distributor().network.remove(this);
  }
  
  default int frequencyUse(){
    return getDistBlock().frequencyUse();
  }
}
