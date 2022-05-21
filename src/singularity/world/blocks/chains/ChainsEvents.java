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
    public ChainsContainer container;
    public ChainsContainer oldContainer;
    
    public AddedBlockEvent(ChainsBuildComp target, ChainsContainer container, ChainsContainer oldContainer){
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
    public ChainsContainer newContainer;
    
    public InitChainContainerEvent(ChainsBuildComp target, ChainsContainer newContainer){
      super(target);
      this.newContainer = newContainer;
    }
  }
  
  public static class ConstructFlowEvent extends ChainsEvent{
    public ChainsContainer container;
    public ChainsContainer oldContainer;
    
    public ConstructFlowEvent(ChainsBuildComp target, ChainsContainer container, ChainsContainer oldContainer){
      super(target);
      this.container = container;
      this.oldContainer = oldContainer;
    }
  }
}
