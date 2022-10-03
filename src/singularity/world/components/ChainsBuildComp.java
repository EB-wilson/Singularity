package singularity.world.components;

import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.gen.Posc;
import singularity.world.blocks.chains.ChainsContainer;
import singularity.world.modules.ChainsModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;

import java.util.Iterator;

public interface ChainsBuildComp extends BuildCompBase, Posc, Iterable<ChainsBuildComp>{
  @Annotations.BindField("chains")
  default ChainsModule chains(){
    return null;
  }
  
  default ChainsBlockComp getChainsBlock(){
    return getBlock(ChainsBlockComp.class);
  }
  
  @Annotations.MethodEntry(entryMethod = "onProximityAdded")
  default void onChainsUpdate(){
    for(ChainsBuildComp other : chainBuilds()){
      if(canChain(other) && other.canChain(this)) other.chains().container.add(chains().container);
    }
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
}
