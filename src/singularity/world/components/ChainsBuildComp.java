package singularity.world.components;

import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Posc;
import singularity.world.blocks.chains.ChainsContainer;
import singularity.world.modules.ChainsModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;

import java.util.Iterator;

public interface ChainsBuildComp extends BuildCompBase, Posc, Iterable<ChainsBuildComp>{
  @Annotations.BindField(value = "loadingInvalidPos", initialize = "new arc.struct.IntSet()")
  default IntSet loadingInvalidPos(){
    return null;
  }

  @Annotations.BindField("chains")
  default ChainsModule chains(){
    return null;
  }
  
  default ChainsBlockComp getChainsBlock(){
    return getBlock(ChainsBlockComp.class);
  }
  
  @Annotations.MethodEntry(entryMethod = "onProximityAdded")
  default void onChainsAdded(){
    for(ChainsBuildComp other : chainBuilds()){
      if(loadingInvalidPos().contains(other.getTile().pos())) continue;
      if(canChain(other) && other.canChain(this)) other.chains().container.add(chains().container);
    }
    if(!loadingInvalidPos().isEmpty()) loadingInvalidPos().clear();
  }

  default boolean canChain(ChainsBuildComp other){
    if(!getChainsBlock().chainable(other.getChainsBlock())) return false;

    return chains().container.inlerp(this, other);
  }
  
  @Annotations.MethodEntry(entryMethod = "onProximityRemoved")
  default void onChainsRemoved(){
    chains().container.remove(this);
  }

  default Seq<ChainsBuildComp> chainBuilds(){
    Seq<ChainsBuildComp> result = new Seq<>();
    for(Building other: getBuilding().proximity){
      if(other instanceof ChainsBuildComp comp && canChain(comp) && comp.canChain(this)){
        result.add((ChainsBuildComp) other);
      }
    }
    return result;
  }
  
  default Iterator<ChainsBuildComp> iterator(){
    return chains().container.all.iterator();
  }

  default void containerCreated(ChainsContainer old){}

  default void chainsAdded(ChainsContainer old){}

  default void chainsRemoved(Seq<ChainsBuildComp> children){}

  default void chainsFlowed(ChainsContainer old){}

  default void onChainsUpdated(){}

  Seq<ChainsBuildComp> tmp = new Seq<>();

  @Annotations.MethodEntry(entryMethod = "write", paramTypes = "arc.util.io.Writes -> write")
  default void writeChains(Writes write){
    tmp.clear();
    for(Building building: getBuilding().proximity){
      if(building instanceof ChainsBuildComp chain && chain.chains().container != chains().container){
        tmp.add(chain);
      }
    }

    write.i(tmp.size);
    for(ChainsBuildComp comp: tmp){
      write.i(comp.getTile().pos());
    }
  }

  @Annotations.MethodEntry(entryMethod = "read", paramTypes = {"arc.util.io.Reads -> read", "byte"})
  default void readChains(Reads read){
    int size = read.i();
    for(int i = 0; i < size; i++){
      loadingInvalidPos().add(read.i());
    }
  }
}
