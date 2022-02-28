package singularity.world.blocks.distribute.matrixGrid;

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
    
    new MatrixEdgeContainer().flow(this);
  }
  
  default void delink(MatrixGridEdge other){
    other.perEdge(null);
    nextEdge(null);
    
    new MatrixEdgeContainer().flow(other);
    new MatrixEdgeContainer().flow(this);
  }
  
  default Tile tile(){
    return getBuilding().tile();
  }
}
