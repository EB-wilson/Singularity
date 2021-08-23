package singularity.world.reaction;

import arc.struct.IntMap;
import arc.struct.ObjectMap;
import mindustry.ctype.MappableContent;
import mindustry.world.Tile;
import singularity.type.Reaction;

public class ReactionPoints{
  private static final ObjectMap<Reaction<?, ?, ?>, ReactionPoint> empty = new ObjectMap<>();
  
  public IntMap<ObjectMap<Reaction<?, ?, ?>, ReactionPoint>> points = new IntMap<>();
  
  public void transfer(Tile tile, Reaction<?, ?, ?> reaction, MappableContent input, float amount){
    ReactionPoint point = points.get(tile.pos(), empty).get(reaction);
    if(point == null || point.reaction != reaction){
      point = ReactionPoint.create();
      point.set(tile, reaction);
      points.get(tile.pos()).put(reaction, point);
      point.add();
    }
    point.addMaterial(input, amount);
  }
  
  public void remove(Tile tile){
    points.remove(tile.pos());
  }
  
  public void remove(ReactionPoint point){
    points.get(point.tile.pos()).remove(point.reaction);
  }
  
  public void remove(Tile tile, Reaction<?, ?, ?> reaction){
    ObjectMap<Reaction<?, ?, ?>, ReactionPoint> map = points.get(tile.pos());
    map.remove(reaction);
  }
}
