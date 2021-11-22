package singularity.world.blockComp;

import arc.func.Cons;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.gen.Posc;
import singularity.world.modules.ChainsModule;
import universeCore.entityComps.blockComps.BuildCompBase;

public interface ChainsBuildComp extends BuildCompBase, Posc{
  ChainsModule chains();
  
  default ChainsBlockComp getChainsBlock(){
    return getBlock(ChainsBlockComp.class);
  }
  
  default void onChainsUpdate(){
    for(ChainsBuildComp other : chainBuilds()){
      other.chains().container.add(chains().container);
    }
  }
  
  default  boolean canChain(ChainsBuildComp other){
    return getChainsBlock().chainable(other.getChainsBlock());
  }
  
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
}
