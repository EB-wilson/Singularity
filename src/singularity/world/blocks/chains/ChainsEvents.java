package singularity.world.blocks.chains;

import singularity.world.components.ChainsBuildComp;

public class ChainsEvents{
  public enum ChainsTrigger{
    update,
    structUpdated
  }
  
  public static class ChainsEvent{
    public ChainsBuildComp target;
    
    public ChainsEvent(ChainsBuildComp target){
      this.target = target;
    }
  }
  
  public static class AddedBlockEvent extends ChainsEvent{
    public ChainContainer container;
    public ChainContainer oldContainer;
    
    public AddedBlockEvent(ChainsBuildComp target, ChainContainer container, ChainContainer oldContainer){
      super(target);
      this.container = container;
      this.oldContainer = oldContainer;
    }
  }
  
  public static class RemovedBlockEvent extends ChainsEvent{
    public RemovedBlockEvent(ChainsBuildComp target){
      super(target);
    }
  }
  
  public static class InitChainContainerEvent extends ChainsEvent{
    public ChainContainer newContainer;
    
    public InitChainContainerEvent(ChainsBuildComp target, ChainContainer newContainer){
      super(target);
      this.newContainer = newContainer;
    }
  }
  
  public static class ConstructFlowEvent extends ChainsEvent{
    public ChainContainer container;
    public ChainContainer oldContainer;
    
    public ConstructFlowEvent(ChainsBuildComp target, ChainContainer container, ChainContainer oldContainer){
      super(target);
      this.container = container;
      this.oldContainer = oldContainer;
    }
  }
}
