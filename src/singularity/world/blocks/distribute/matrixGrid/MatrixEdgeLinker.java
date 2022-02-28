package singularity.world.blocks.distribute.matrixGrid;

import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.ObjectSet;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import universeCore.annotations.Annotations;

import static mindustry.Vars.tilesize;

public interface MatrixEdgeLinker{
  ObjectSet<MatrixGridEdge> tmpSeq = new ObjectSet<>();
  
  @Annotations.BindField("linkLength")
  default int linkLength(){
    return 0;
  }
  
  @Annotations.BindField("linkRegion")
  default TextureRegion linkRegion(){
    return null;
  }
  
  @Annotations.MethodEntry(entryMethod = "drawPlace", paramTypes = {"int -> x", "int -> y", "int -> rotation", "boolean -> valid"})
  default void drawPlacing(int x, int y, int rotation, boolean valid){
    for(int i = 0; i < 4; i++){
      float dx = x*tilesize + getBlock().offset + Geometry.d4x(i)*getBlock().size*tilesize/2f;
      float dy = y*tilesize + getBlock().offset + Geometry.d4y(i)*getBlock().size*tilesize/2f;
      
      Drawf.dashLine(
          Pal.accent,
          dx,
          dy,
          dx + Geometry.d4x(i)*linkLength()*tilesize,
          dy + Geometry.d4y(i)*linkLength()*tilesize
      );
    }
  }
  
  default void drawConfiguring(MatrixGridEdge origin){
    Drawf.square(origin.getBuilding().x, origin.getBuilding().y, origin.getBlock().size*tilesize, Pal.accent);
    if(origin.perEdge() != null){
      Tmp.v2.set(
          Tmp.v1.set(origin.getBuilding().x, origin.getBuilding().y)
              .sub(origin.perEdge().getBuilding().x, origin.perEdge().getBuilding().y)
              .setLength(origin.perEdge().getBlock().size*tilesize/2f))
          .setLength(origin.getBlock().size*tilesize/2f);
      
      Drawf.square(
          origin.perEdge().getBuilding().x,
          origin.perEdge().getBuilding().y,
          origin.perEdge().getBlock().size*tilesize,
          45,
          Pal.place
      );
      Drawf.dashLine(
          Pal.accent,
          origin.perEdge().getBuilding().x + Tmp.v1.x,
          origin.perEdge().getBuilding().y + Tmp.v1.y,
          origin.getBuilding().x - Tmp.v2.x,
          origin.getBuilding().y - Tmp.v2.y
      );
    }
    
    if(origin.nextEdge() != null){
      Tmp.v2.set(
          Tmp.v1.set(origin.nextEdge().getBuilding().x, origin.nextEdge().getBuilding().y)
              .sub(origin.getBuilding().x, origin.getBuilding().y)
              .setLength(origin.getBlock().size*tilesize/2f))
          .setLength(origin.nextEdge().getBlock().size*tilesize/2f);
  
      Drawf.square(
          origin.nextEdge().getBuilding().x,
          origin.nextEdge().getBuilding().y,
          origin.nextEdge().getBlock().size*tilesize,
          45,
          Pal.accent
      );
      Lines.stroke(3, Pal.gray);
      Lines.line(
          origin.getBuilding().x + Tmp.v1.x,
          origin.getBuilding().y + Tmp.v1.y,
          origin.nextEdge().getBuilding().x - Tmp.v2.x,
          origin.nextEdge().getBuilding().y - Tmp.v2.y
      );
      Lines.stroke(1, Pal.accent);
      Lines.line(
          origin.getBuilding().x + Tmp.v1.x,
          origin.getBuilding().y + Tmp.v1.y,
          origin.nextEdge().getBuilding().x - Tmp.v2.x,
          origin.nextEdge().getBuilding().y - Tmp.v2.y
      );
    }
    
    for(MatrixGridEdge other : getLinkable(origin)){
      if(other == origin.nextEdge() || other == origin.perEdge()) continue;
      Drawf.select(other.getBuilding().x, other.getBuilding().y,
          other.getBlock().size * tilesize / 2f + 2f + Mathf.absin(Time.time, 4f, 1f), Pal.breakInvalid);
    }
  }
  
  default ObjectSet<MatrixGridEdge> getLinkable(MatrixGridEdge origin){
    tmpSeq.clear();
    for(int i = 0; i < 4; i++){
      int dx = Geometry.d4x(i);
      int dy = Geometry.d4y(i);
      for(int l = getBlock().size/2; l < linkLength() + getBlock().size/2; l++){
        Tile tile = origin.tile().nearby(dx*l, dy*l);
        if(tile == null) continue;
        Building build = tile.build;
        if(canLink(origin, build)) tmpSeq.add((MatrixGridEdge) build);
      }
    }
    
    return tmpSeq;
  }
  
  default Block getBlock(){
    return (Block) this;
  }
  
  default void link(MatrixGridEdge entity, Point2 pos){
    Building build = Vars.world.build(pos.pack());
    
    if(build instanceof MatrixGridEdge){
      if(entity.nextEdge() == build){
        entity.delink((MatrixGridEdge) build);
      }
      else entity.link((MatrixGridEdge) build);
    }
  }
  
  default boolean canLink(MatrixGridEdge origin, Building other){
    return other instanceof MatrixGridEdge && canLink(origin, (MatrixGridEdge) other);
  }
  
  default boolean canLink(MatrixGridEdge origin, MatrixGridEdge other){
    int xDistance = Math.abs(origin.tile().x - other.tile().x),
        yDistance = Math.abs(origin.tile().y - other.tile().y);
    
    return (yDistance < linkLength() + getBlock().size/2f + getBlock().offset && origin.tile().x == other.tile().x && origin.tile().y != other.tile().y)
        || (xDistance < linkLength() + getBlock().size/2f + getBlock().offset && origin.tile().x != other.tile().x && origin.tile().y == other.tile().y);
  }
}
