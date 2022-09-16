package singularity.world.blocks.distribute.netcomponents;

import arc.struct.ObjectSet;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.world.blocks.distribute.DistNetBlock;

public class ComponentBus extends DistNetBlock{
  public ComponentBus(String name){
    super(name);
  }

  public class ComponentBusBuild extends DistNetBuild{
    public ObjectSet<ComponentBusBuild> proximityBus = new ObjectSet<>();
    public ObjectSet<NetPluginComp.NetPluginCompBuild> proximityComp = new ObjectSet<>();

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
    public void updateNetLinked(){
      super.updateNetLinked();

      updateConnectedBus();

      netLinked.add(proximityBus.toSeq());
      netLinked.add(proximityComp.toSeq());
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      updateNetLinked();
    }
  }
}
