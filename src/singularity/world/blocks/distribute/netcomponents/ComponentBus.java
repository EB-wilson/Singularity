package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.struct.ObjectSet;
import arc.util.Strings;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;

public class ComponentBus extends DistNetBlock{
  public float matrixEnergyRequestMulti;

  public ComponentBus(String name){
    super(name);

    isNetLinker = true;
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.remove(SglStat.matrixEnergyUse);
    stats.remove(SglStat.topologyCapacity);

    stats.add(SglStat.topologyCapacity, Core.bundle.get("infos.compBusTopology"));
    if(matrixEnergyRequestMulti > 0) stats.add(SglStat.matrixEnergyUse, Strings.autoFixed(matrixEnergyRequestMulti*60, 2)
        + "*1.14^" + Core.bundle.get("infos.busCount")
        + " " + SglStatUnit.matrixEnergy.localized() + Core.bundle.get("misc.perSecond")
    );
  }

  public class ComponentBusBuild extends DistNetBuild{
    public CompBusGroup group;
    public ObjectSet<ComponentBusBuild> proximityBus = new ObjectSet<>();
    public ObjectSet<NetPluginComp.NetPluginCompBuild> proximityComp = new ObjectSet<>();

    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      new CompBusGroup().add(this);

      return this;
    }

    @Override
    public ComponentBus block(){
      return ComponentBus.this;
    }

    public boolean linkable(Tile tile){
      return true;
    }

    public void updateConnectedBus(){
      proximityBus.clear();
      proximityComp.clear();

      for(Building building: proximity){
        if(building instanceof ComponentBusBuild bus && bus.linkable(tile) && linkable(bus.tile)){
          if(building.block == block){
            if((bus.tileX() == tileX() || bus.tileY() == tileY())){
              proximityBus.add(bus);
            }
          }
          else proximityBus.add(bus);
        }
        else if(building instanceof NetPluginComp.NetPluginCompBuild comp && linkable(comp.tile)){
          proximityComp.add(comp);
        }
      }
    }

    @Override
    public void onProximityAdded() {
      updateConnectedBus();
      for(ComponentBusBuild bus: proximityBus){
        if(bus.group != group) bus.group.add(group);
      }

      for(ComponentBusBuild child: group.children){
        child.updateConnectedBus();
        child.updateNetLinked();
      }
      new DistributeNetwork().flow(this);
    }

    @Override
    public void updateNetLinked(){
      super.updateNetLinked();

      netLinked.add(group);
      netLinked.add(proximityBus.toSeq());
      netLinked.add(proximityComp.toSeq());
    }

    @Override
    public void onProximityRemoved(){
      updateConnectedBus();
      group.remove(this);
      for(ComponentBusBuild child: group.children){//remove后，原容器保存的信息此时并没有清空，故可正确遍历原有所有子元素
        if(child.group == group) continue;

        child.updateConnectedBus();
        child.updateNetLinked();
      }
      super.onProximityRemoved();
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      updateConnectedBus();
      updateNetLinked();
    }

    @Override
    public float matrixEnergyConsume(){
      return 0;
    }
  }
}
