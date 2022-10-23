package singularity.world.blocks.distribute.netcomponents;

import mindustry.gen.Building;
import mindustry.world.Tile;

public class JumpLine extends ComponentBus{
  public JumpLine(String name){
    super(name);
    rotate = true;
    size = 1;
  }

  public class JumpLineBuild extends ComponentBusBuild{
    @Override
    public boolean linkable(Tile tile){
      return nearby(rotation) == tile.build || nearby((rotation + 2)%4) == tile.build;
    }

    @Override
    public void updateConnectedBus(){
      proximityBus.clear();

      for(Building building: proximity){
        if(building instanceof ComponentBusBuild bus && bus.linkable(tile) && linkable(bus.tile)){
          proximityBus.add(bus);
        }
      }
    }
  }
}
