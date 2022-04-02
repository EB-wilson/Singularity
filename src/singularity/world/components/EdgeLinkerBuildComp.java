package singularity.world.components;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.world.blocks.distribute.matrixGrid.EdgeContainer;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;

public interface EdgeLinkerBuildComp extends BuildCompBase{
  @Annotations.BindField("edges")
  default EdgeContainer getEdges(){
    return null;
  }
  
  @Annotations.BindField("edges")
  default void setEdges(EdgeContainer edges){}
  
  @Annotations.BindField("nextEdge")
  default EdgeLinkerBuildComp nextEdge(){
    return null;
  }
  
  @Annotations.BindField("nextEdge")
  default void nextEdge(EdgeLinkerBuildComp edge){}
  
  @Annotations.BindField("perEdge")
  default EdgeLinkerBuildComp perEdge(){
    return null;
  }
  
  @Annotations.BindField("perEdge")
  default void perEdge(EdgeLinkerBuildComp edge){}
  
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
  
  @Annotations.MethodEntry(entryMethod = "update")
  default void updateLinking(){
    if(nextPos() != -1 && (nextEdge() == null || !nextEdge().getBuilding().isAdded() || nextEdge().tile().pos() != nextPos())){
      if(nextEdge() != null && !nextEdge().getBuilding().isAdded()){
        nextPos(-1);
      }
      Building build = nextPos() == -1? null: Vars.world.build(nextPos());
      if(build instanceof EdgeLinkerBuildComp){
        link((EdgeLinkerBuildComp) build);
      }
    }
    else if(nextPos() == -1 && nextEdge() != null){
      delink(nextEdge());
    }
  }
  
  default void linked(EdgeLinkerBuildComp next){}
  
  default void delinked(EdgeLinkerBuildComp next){}
  
  default void link(EdgeLinkerBuildComp other){
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
    
    new EdgeContainer().flow(this);
    linked(other);
  }
  
  default void delink(EdgeLinkerBuildComp other){
    other.perEdge(null);
    nextEdge(null);
    
    nextPos(-1);
    
    new EdgeContainer().flow(other);
    new EdgeContainer().flow(this);
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
