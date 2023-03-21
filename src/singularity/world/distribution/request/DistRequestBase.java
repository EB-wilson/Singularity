package singularity.world.distribution.request;

import arc.Core;
import arc.func.Boolf;
import arc.func.Boolp;
import org.jetbrains.annotations.Nullable;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistributeNetwork;
import universecore.annotations.Annotations;
import universecore.components.ExtraVariableComp;

public abstract class DistRequestBase{
  private long updateMark, executeMark;
  
  protected boolean initialized, sleeping, killed, blocked;

  public Boolf<? extends DistElementBuildComp> waker;
  public final DistElementBuildComp sender;
  public DistributeNetwork target;

  @Nullable RequestTask preHandleCallback, handleCallBack, afterHandleCallBack;
  
  public DistRequestBase(DistElementBuildComp sender){
    this.sender = sender;
  }

  public <T extends DistElementBuildComp> void setWaker(Boolf<T> waker){
    this.waker = waker;
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

  public void update(RequestTask handle){
    update(null, handle, null);
  }

  public void update(RequestTask pre, RequestTask after){
    update(pre, null, after);
  }

  public void update(RequestTask pre, RequestTask handle, RequestTask after){
    preHandleCallback = pre;
    handleCallBack = handle;
    afterHandleCallBack = after;
    update();
  }

  @SuppressWarnings("unchecked")
  public void checkWaking(){
    if(waker != null){
      if(((Boolf<DistElementBuildComp>)waker).get(sender)){
        if(sleeping) weak();
      }
      else if(!sleeping) sleep();
    }
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
    if(preHandleCallback != null){
      return preHandleCallback.run(this::preHandleTask);
    }
    else return preHandleTask();
  }

  public boolean handle(){
    if(handleCallBack != null){
      return handleCallBack.run(this::handleTask);
    }
    else return handleTask();
  }
  
  public boolean afterHandle(){
    if(afterHandleCallBack != null){
      return afterHandleCallBack.run(this::afterHandleTask);
    }
    else return afterHandleTask();
  }

  protected abstract boolean preHandleTask();
  protected abstract boolean handleTask();
  protected abstract boolean afterHandleTask();

  public void resetCallBack(){
    preHandleCallback = null;
    handleCallBack = null;
    afterHandleCallBack = null;
  }
  
  public void onExecute(){
    executeMark = Core.graphics.getFrameId();
  }
  
  public static class RequestStatusException extends RuntimeException{
    public RequestStatusException(String info){
      super(info);
    }
  }

  public interface RequestTask{
    boolean run(Boolp callTask);
  }
}
