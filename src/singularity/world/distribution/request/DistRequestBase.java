package singularity.world.distribution.request;

import singularity.world.distribution.DistributeNetwork;

public abstract class DistRequestBase{
  public DistributeNetwork target;
  
  public void init(DistributeNetwork target){
    this.target = target;
  }
  
  public abstract void handle();
  
  public boolean valid(){
    return true;
  }
}
