package singularity.world.blocks.distribute.matrixGrid;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;
import universeCore.annotations.Annotations;
import universeCore.entityComps.blockComps.BuildCompBase;

public interface MatrixGridEdge extends BuildCompBase{
  @Annotations.BindField("edges")
  default MatrixEdgeContainer getEdges(){
    return null;
  }
  
  @Annotations.BindField("edges")
  default void setEdges(MatrixEdgeContainer edges){}
  
  @Annotations.BindField("nextEdge")
  default MatrixGridEdge nextEdge(){
    return null;
  }
  
  @Annotations.BindField("nextEdge")
  default void nextEdge(MatrixGridEdge edge){}
  
  @Annotations.BindField("perEdge")
  default MatrixGridEdge perEdge(){
    return null;
  }
  
  @Annotations.BindField("perEdge")
  default void perEdge(MatrixGridEdge edge){}
  
  @Annotations.MethodEntry(entryMethod = "onProximityRemoved")
  default void edgeRemoved(){
    getEdges().remove(this);
  }
  
  @Annotations.BindField("nextPos")
  default int nextPos(){
    return 0;
  }
  
  @Annotations.BindField("nextPos")
  default void nextPos(int pos){}
  
  @Annotations.MethodEntry(entryMethod = "updateTile")
  default void updateLinking(){
    if(nextPos() != -1 && (nextEdge() == null || !nextEdge().getBuilding().isAdded() || nextEdge().tile().pos() != nextPos())){
      if(nextEdge() != null && !nextEdge().getBuilding().isAdded()){
        nextPos(-1);
      }
      Building build = nextPos() == -1? null: Vars.world.build(nextPos());
      if(build instanceof MatrixGridEdge){
        link((MatrixGridEdge) build);
      }
    }
    else if(nextPos() == -1 && nextEdge() != null){
      delink(nextEdge());
    }
  }
  
  default void linked(MatrixGridEdge next){}
  
  default void delinked(MatrixGridEdge next){}
  
  default void link(MatrixGridEdge other){
    if(other.nextEdge() == this){
      other.delink(this);
    }
    if(nextEdge() != null){
      delink(nextEdge());
    }
    if(other.perEdge() != null){
      other.perEdge().delink(other);
    }
    
    other.perEdge(this);
    nextEdge(other);
    
    nextPos(other.tile().pos());
    
    new MatrixEdgeContainer().flow(this);
    linked(other);
  }
  
  default void delink(MatrixGridEdge other){
    other.perEdge(null);
    nextEdge(null);
    
    nextPos(-1);
    
    new MatrixEdgeContainer().flow(other);
    new MatrixEdgeContainer().flow(this);
    delinked(other);
  }
  
  @Annotations.MethodEntry(entryMethod = "read", paramTypes = {"arc.util.io.Reads -> read", "byte"})
  default void readLink(Reads read){
    nextPos(read.i());
  }
  
  @Annotations.MethodEntry(entryMethod = "write", paramTypes = {"arc.util.io.Writes -> write"})
  default void writeLink(Writes write){
    write.i(nextPos());
  }
  
  default Tile tile(){
    return getBuilding().tile();
  }
}
