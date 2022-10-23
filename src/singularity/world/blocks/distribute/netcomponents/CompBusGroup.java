package singularity.world.blocks.distribute.netcomponents;

import arc.math.Mathf;
import arc.struct.Seq;
import singularity.world.FinderContainerBase;
import singularity.world.components.distnet.DistElementBlockComp;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.modules.DistributeModule;
import universecore.util.Empties;

public class CompBusGroup extends FinderContainerBase<ComponentBus.ComponentBusBuild> implements DistElementBuildComp{
  public static final DistElementBlockComp block = new DistElementBlockComp(){
    @Override
    public int topologyUse(){
      return 2;
    }
  };

  public DistributeModule distributor;

  public float totalConsEnergy;
  public int totalConsumers;

  public Seq<ComponentBus.ComponentBusBuild> children = new Seq<>();

  public CompBusGroup(){
    distributor = new DistributeModule(this);
    distributor.setNet();
  }

  @Override
  public DistElementBlockComp getDistBlock(){
    return block;
  }

  public void add(CompBusGroup group){
    for(ComponentBus.ComponentBusBuild child: group.children){
      add(child);
    }
  }

  @Override
  public void add(ComponentBus.ComponentBusBuild componentBusBuild){
    if(componentBusBuild.group == this) return;

    componentBusBuild.group = this;
    children.add(componentBusBuild);
    if(componentBusBuild.block().matrixEnergyRequestMulti > 0) totalConsumers++;

    totalConsEnergy += componentBusBuild.block().matrixEnergyRequestMulti;
  }

  @Override
  public void flow(ComponentBus.ComponentBusBuild seed){
    excluded.clear();
    super.flow(seed);
  }

  public void flow(ComponentBus.ComponentBusBuild seed, ComponentBus.ComponentBusBuild excl){
    excluded.clear();
    excluded.add(excl);
    super.flow(seed);
  }

  public void remove(ComponentBus.ComponentBusBuild target){
    for(ComponentBus.ComponentBusBuild vertex: getLinkVertices(target)){
      if(vertex.group != this) continue;

      new CompBusGroup().flow(vertex, target);
    }
  }

  @Override
  public Iterable<ComponentBus.ComponentBusBuild> getLinkVertices(ComponentBus.ComponentBusBuild componentBusBuild){
    return componentBusBuild.proximityBus;
  }

  @Override
  public boolean isDestination(ComponentBus.ComponentBusBuild componentBusBuild, ComponentBus.ComponentBusBuild vert1){
    return false;
  }

  @Override
  public DistributeModule distributor(){
    return distributor;
  }

  @Override
  public Seq<DistElementBuildComp> netLinked(){
    return Empties.nilSeq();
  }

  @Override
  public float matrixEnergyConsume(){
    float ave = totalConsEnergy/totalConsumers;
    return ave*Mathf.pow(1.14f, totalConsumers);
  }
}
