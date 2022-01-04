package singularity.world.distribution.request;

import singularity.world.distribution.DistributeNetwork;

public abstract class DistRequestBase{
  protected boolean sleeping, killed;
  
  public DistributeNetwork target;
  
  public int priority(){
    return 0;
  }
  
  public boolean finished(){
    return killed;
  }
  
  public boolean sleeping(){
    return sleeping;
  }
  
  public void kill(){
    killed = true;
  }
  
  public void sleep(){
    sleeping = true;
  }
  
  public void noSleep(){
    sleeping = false;
  }
  
  public void init(DistributeNetwork target){
    this.target = target;
  }
  
  public void preHandle(){}
  
  public abstract void handle();
}
