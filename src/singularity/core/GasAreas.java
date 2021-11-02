package singularity.core;

import arc.struct.IntMap;
import mindustry.world.Tile;
import singularity.type.Gas;
import singularity.world.atmosphere.LeakGasArea;

public class GasAreas{
  public IntMap<LeakGasArea> areas = new IntMap<>();
  
  public void pour(Tile tile, Gas gas, float flow){
    LeakGasArea area = areas.get(tile.pos());
    
    if(area != null){
      area.flow(gas, flow);
    }
    else{
      area = LeakGasArea.create();
      area.set(gas, flow, tile);
      areas.put(tile.pos(), area);
      area.add();
    }
  }
  
  public void add(LeakGasArea area){
    if(!areas.containsKey(area.tile.pos())) areas.put(area.tile.pos(), area);
  }
  
  public LeakGasArea get(Tile tile){
    return areas.get(tile.pos());
  }
  
  public void remove(Tile tile){
    areas.remove(tile.pos());
  }
  
  public void remove(LeakGasArea area){
    areas.remove(area.tile.pos());
  }
  
  public void clear(){
    areas.clear();
  }
}
