package singularity.world.components;

import singularity.world.blocks.chains.ChainsEvents;

public interface StructCoreBuildComp extends StructBuildComp{
  @Override
  default void setChainsListeners(){
    chains().listenGlobal(ChainsEvents.AddedBlockEvent.class, "matching", e -> {
      getStructCore().structure().match(chains().container);
    });
  }
  
  default StructCoreComp getStructCore(){
    return getBlock(StructCoreComp.class);
  }
}
