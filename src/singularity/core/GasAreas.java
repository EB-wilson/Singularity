package singularity.core;

import arc.math.Mathf;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.world.Tile;
import singularity.type.Gas;
import singularity.world.atmosphere.LeakGasArea;

public class GasAreas{
  public IntMap<Seq<LeakGasArea>> areas = new IntMap<>();
  
  public void pour(Tile tile, Gas gas, float flow){
    Seq<LeakGasArea> area = areas.get(tile.pos());
    
    if(area == null){
      area = new Seq<>();
      areas.put(tile.pos(), area);
    }
    
    LeakGasArea gasArea = area.find(e -> e.gas == gas);
    if(gasArea != null){
      gasArea.flow(flow);
    }
    else{
      gasArea = LeakGasArea.create();
      area.add(gasArea);
      gasArea.set(gas, flow, tile);
      areas.put(tile.pos(), area);
      gasArea.add();
    }
  }
  
  public void add(LeakGasArea area){
    Seq<LeakGasArea> seq = areas.get(area.tile.pos(), areas.put(area.tile.pos(), new Seq<>()));
    if(!seq.contains(area)) seq.add(area);
  }
  
  public Seq<LeakGasArea> get(Tile tile){
    return areas.get(tile.pos());
  }
  
  public LeakGasArea get(Tile tile, int index){
    Seq<LeakGasArea> map = areas.get(tile.pos());
    if(index >= map.size) return null;
    return map.get(index);
  }
  
  public void remove(Tile tile){
    areas.remove(tile.pos());
  }
  
  public void remove(LeakGasArea area){
    areas.get(area.tile.pos()).remove(area);
  }
  
  public void remove(Tile tile, int index){
    Seq<LeakGasArea> map = areas.get(tile.pos());
    if(index >= map.size) return;
    map.remove(index);
  }
  
  public void clear(){
    areas.forEach(e -> {
      e.value.each(LeakGasArea::remove);
    });
  }
}
