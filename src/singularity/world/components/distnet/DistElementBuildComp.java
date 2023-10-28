package singularity.world.components.distnet;

import arc.struct.IntSeq;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.modules.DistributeModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;

public interface DistElementBuildComp extends BuildCompBase {
  DistributeModule distributor();

  @Annotations.BindField("priority")
  default int priority(){
    return 0;
  }

  @Annotations.BindField("priority")
  default void priority(int priority){}

  @Annotations.BindField(value = "netLinked", initialize = "new arc.struct.Seq<>()")
  default Seq<DistElementBuildComp> netLinked(){
    return null;
  }

  @Annotations.BindField("matrixEnergyBuffered")
  default float matrixEnergyBuffered(){
    return 0;
  }

  @Annotations.BindField("matrixEnergyBuffered")
  default void matrixEnergyBuffered(float set){}

  default void updateNetStat(){}
  
  default void networkValided(){}

  default void networkUpdated(){}

  default void linked(DistElementBuildComp target){}

  default void delinked(DistElementBuildComp target){}

  default boolean linkable(DistElementBuildComp other){
    return true;
  }

  default DistElementBlockComp getDistBlock(){
    return getBlock(DistElementBlockComp.class);
  }

  default void networkRemoved(DistElementBuildComp remove){
    distributor().distNetLinks.removeValue(remove.getBuilding().pos());
  }

  default void link(DistElementBuildComp target){
    if(!linkable(target) || !target.linkable(this)) return;

    if(getDistBlock().isNetLinker()) distributor().distNetLinks.addUnique(target.getBuilding().pos());
    if(target.getDistBlock().isNetLinker()) target.distributor().distNetLinks.addUnique(getBuilding().pos());
  
    updateNetLinked();
    target.updateNetLinked();

    if(target.getDistBlock().isNetLinker()){
      distributor().network.add(target.distributor().network);
    }
    else{
      target.distributor().network.remove(target);
      distributor().network.add(target);
    }

    target.linked(this);
    linked(target);
  }
  
  default void deLink(DistElementBuildComp target){
    distributor().distNetLinks.removeValue(target.getBuilding().pos());
    target.distributor().distNetLinks.removeValue(getBuilding().pos());
  
    updateNetLinked();
    target.updateNetLinked();

    for(DistElementBuildComp element: distributor().network){
      element.networkRemoved(target);
    }
    
    new DistributeNetwork().flow(this);
    new DistributeNetwork().flow(target);

    target.delinked(this);
    delinked(target);
  }
  
  default void updateNetLinked(){
    netLinked().clear();

    if(getDistBlock().isNetLinker()){
      for(int i = 0; i < distributor().distNetLinks.size; i++){
        Tile entity = Vars.world.tile(distributor().distNetLinks.get(i));
        if(entity == null || !(entity.build instanceof DistElementBuildComp)) continue;
        if(!netLinked().contains((DistElementBuildComp) entity.build)) netLinked().add((DistElementBuildComp) entity.build);
      }
    }
  }

  @Annotations.MethodEntry(entryMethod = "onProximityAdded")
  default void distNetAdd(){
    updateNetLinked();
    for(DistElementBuildComp comp: netLinked()){
      if(getDistBlock().isNetLinker()){
        comp.distributor().network.add(distributor().network);
      }
      else comp.distributor().network.add(this);
    }
  }

  @Annotations.MethodEntry(entryMethod = "onProximityRemoved")
  default void distNetRemove(){
    distributor().distNetLinks.clear();
    distributor().network.remove(this);
    updateNetLinked();
  }
  
  default int frequencyUse(){
    return getDistBlock().topologyUse();
  }

  default float matrixEnergyConsume(){
    return getDistBlock().matrixEnergyUse();
  }

  default float matrixEnergyProduct(){
    return 0;
  }
}
