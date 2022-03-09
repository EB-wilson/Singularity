package singularity.world.distribution.request;

import arc.Core;
import arc.struct.Seq;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistributeNetwork;

public abstract class DistRequestBase<S>{
  private long updateMark, executeMark;
  
  protected boolean initialized, sleeping, killed, blocked;
  
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
    return sleeping || updateMark < Core.graphics.getFrameId() - 1;
  }
  
  public void block(boolean blocked){
    this.blocked = blocked;
  }
  
  public boolean isBlocked(){
    return blocked;
  }
  
  public boolean executing(){
    return executeMark == Core.graphics.getFrameId();
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
    updateMark = Core.graphics.getFrameId();
  }
  
  public void checkStatus(){
    if(!initialized)
      throw new RequestStatusException("handle a uninitialized request");
    if(killed){
      throw new RequestStatusException("handle a death request");
    }
  }
  
  public boolean preHandle(){
    return true;
  }
  
  public boolean afterHandle(){
    return true;
  }
  
  public abstract boolean handle();
  
  public abstract Seq<S> getList();
  
  public void onExecute(){
    executeMark = Core.graphics.getFrameId();
  }
  
  public static class RequestStatusException extends RuntimeException{
    public RequestStatusException(String info){
      super(info);
    }
  }
}
