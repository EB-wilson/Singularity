package singularity.world.distribution.request;

import arc.Core;
import arc.struct.Seq;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistributeNetwork;

public abstract class DistRequestBase<S>{
  private long frameId;
  
  protected boolean initialized, sleeping, killed;
  
  public final DistMatrixUnitBuildComp sender;
  public DistributeNetwork target;
  
  public DistRequestBase(DistMatrixUnitBuildComp sender){
    this.sender = sender;
  }
  
  public int priority(){
    return 0;
  }
  
  public boolean finished(){
    return killed || !sender.getBuilding().isAdded();
  }
  
  public boolean sleeping(){
    return sleeping || frameId < Core.graphics.getFrameId() - 1;
  }
  
  public void kill(){
    killed = true;
  }
  
  public void sleep(){
    sleeping = true;
  }
  
  public void weak(){
    sleeping = false;
  }
  
  public void init(DistributeNetwork target){
    this.target = target;
    initialized = true;
  }
  
  public void update(){
    frameId = Core.graphics.getFrameId();
  }
  
  public void checkStatus(){
    if(!initialized)
      throw new RequestStatusException("handle a uninitialized request");
    if(killed){
      throw new RequestStatusException("handle a death request");
    }
  }
  
  public void preHandle(){}
  
  public void afterHandle(){}
  
  public abstract void handle();
  
  public abstract Seq<S> getList();
  
  public static class RequestStatusException extends RuntimeException{
    public RequestStatusException(String info){
      super(info);
    }
  }
}
