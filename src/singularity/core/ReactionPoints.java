package singularity.core;

import arc.struct.IntMap;
import mindustry.ctype.UnlockableContent;
import mindustry.world.Tile;
import singularity.world.reaction.ReactionPoint;

public class ReactionPoints{
  public IntMap<ReactionPoint> points = new IntMap<>();
  
  public void transfer(Tile tile, UnlockableContent input, float amount){
    ReactionPoint point = points.get(tile.pos());
    if(point == null){
      point = ReactionPoint.create();
      point.set(tile);
      points.put(tile.pos(), point);
      point.add();
    }
    point.addMaterial(input, amount);
  }
  
  public void remove(Tile tile){
    points.remove(tile.pos());
  }
  
  public void remove(ReactionPoint point){
    points.remove(point.tile.pos());
  }
}
