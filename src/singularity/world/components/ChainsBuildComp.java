package singularity.world.components;

import arc.func.Cons;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.gen.Posc;
import singularity.world.modules.ChainsModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;

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
    /*ChainContainer container = chains().container, otherCont = other.chains().container;
    if(container.inlerp(other.tileX(), other.tileY())) return true;

    float offset = (other.getBlock().size + other.getBlock().offset)/2;
    float offsetS = (getBlock().size + getBlock().offset)/2;
    return Math.max(container.maxX(), (int)(other.tileX() + offset)) - Math.min(container.minX(), (int)(other.tileX() - offset)) <= getChainsBlock().maxWidth()
    && Math.max(container.maxY(), (int)(other.tileY() + offset)) - Math.min(container.minY(), (int)(other.tileY() - offset)) <= getChainsBlock().maxWidth()
    && Math.max(otherCont.maxX(), (int)(tileX() + offsetS)) - Math.min(otherCont.minX(), (int)(tileX() - offsetS)) <= other.getChainsBlock().maxWidth()
    && Math.max(otherCont.maxY(), (int)(tileY() + offsetS)) - Math.min(otherCont.minY(), (int)(tileY() - offsetS)) <= other.getChainsBlock().maxWidth();*/
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
  
  default void setChainsListeners(){
    chains().setListeners(getChainsBlock().listeners());
    chains().setGlobalListeners(getChainsBlock().globalListeners());
  }
}
