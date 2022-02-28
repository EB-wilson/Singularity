package singularity.world.blockComp;

import arc.func.Cons;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.gen.Posc;
import singularity.world.modules.ChainsModule;
import universeCore.annotations.Annotations;
import universeCore.entityComps.blockComps.BuildCompBase;

public interface ChainsBuildComp extends BuildCompBase, Posc{
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
      other.chains().container.add(chains().container);
    }
  }
  
  default boolean canChain(ChainsBuildComp other){
    return getChainsBlock().chainable(other.getChainsBlock());
  }
  
  @Annotations.MethodEntry(entryMethod = "onProximityRemoved")
  default void onChainsRemoved(){
    chains().container.remove(this);
  }
  
  default Seq<ChainsBuildComp> chainBuilds(){
    Seq<ChainsBuildComp> result = new Seq<>();
    for(Building other: getBuilding().proximity){
      if(other instanceof ChainsBuildComp && canChain((ChainsBuildComp) other)){
        result.add((ChainsBuildComp) other);
      }
    }
    return result;
  }
  
  default void iterator(Cons<ChainsBuildComp> cons){
    chains().each(cons);
  }
  
  default void setChainsListeners(){}
}
