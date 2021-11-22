package singularity.world.blocks.chains;

import singularity.world.blockComp.ChainsBuildComp;

public class ChainsEvents{
  public static class ChainsEvent{}
  
  public static class TargetChainsEvent extends ChainsEvent{
    public ChainsBuildComp target;
    
    public TargetChainsEvent(ChainsBuildComp target){
      this.target = target;
    }
  }
  
  public static class AddedBlockEvent extends TargetChainsEvent{
    public ChainContainer container;
    public ChainContainer oldContainer;
    
    public AddedBlockEvent(ChainsBuildComp target, ChainContainer container, ChainContainer oldContainer){
      super(target);
      this.container = container;
      this.oldContainer = oldContainer;
    }
  }
  
  public static class RemovedBlockEvent extends TargetChainsEvent{
    public RemovedBlockEvent(ChainsBuildComp target){
      super(target);
    }
  }
  
  public static class InitChainContainerEvent extends TargetChainsEvent{
    public ChainContainer newContainer;
    
    public InitChainContainerEvent(ChainsBuildComp target, ChainContainer newContainer){
      super(target);
      this.newContainer = newContainer;
    }
  }
  
  public static class ConstructFlowEvent extends TargetChainsEvent{
    public ChainContainer container;
    public ChainContainer oldContainer;
    
    public ConstructFlowEvent(ChainsBuildComp target, ChainContainer container, ChainContainer oldContainer){
      super(target);
      this.container = container;
      this.oldContainer = oldContainer;
    }
  }
  
  public static class UpdateEvent extends ChainsEvent{}
}
