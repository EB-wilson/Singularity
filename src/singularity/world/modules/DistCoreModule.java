package singularity.world.modules;

import arc.struct.ObjectMap;
import singularity.world.blockComp.distributeNetwork.DistElementBuildComp;
import singularity.world.blockComp.distributeNetwork.DistNetworkCoreComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;

import java.util.Iterator;
import java.util.PriorityQueue;

@SuppressWarnings("rawtypes")
public class DistCoreModule extends DistributeModule{
  public PriorityQueue<DistRequestBase> requestTasks = new PriorityQueue<>((a, b) -> a.priority() - b.priority());
  public int calculatePower;
  public final DistNetworkCoreComp core;
  
  public ObjectMap<DistBuffers<?>, BaseBuffer<?, ?, ?>> buffers = new ObjectMap<>();
  
  public DistCoreModule(DistElementBuildComp entity){
    super(entity);
    core = (DistNetworkCoreComp) entity;
    for(DistBuffers<?> buffer: DistBuffers.all){
      buffers.put(buffer, buffer.get());
    }
  }
  
  @SuppressWarnings("unchecked")
  public <T extends BaseBuffer<?, ?, ?>> T getBuffer(DistBuffers<T> buffer){
    return (T)buffers.get(buffer);
  }
  
  public void update(){
    if(network.netValid()){
      network.update();
  
      Iterator<DistRequestBase> itr = requestTasks.iterator();
      while(itr.hasNext()){
        DistRequestBase request = itr.next();
        if(request.finished()){
          itr.remove();
        }
        else{
          request.checkStatus();
          if(!request.sleeping()) request.preHandle();
        }
      }
      
      for(DistRequestBase request: requestTasks){
        if(!request.sleeping()) request.handle();
      }
  
      for(DistRequestBase request : requestTasks){
        if(!request.sleeping()) request.afterHandle();
      }
  
      for(BaseBuffer<?, ?, ?> buffer : buffers.values()){
        buffer.update(core.updateState());
        buffer.bufferContAssign(network);
      }
    }
  }
  
  public void receive(DistRequestBase request){
    if(requestTasks.size() <= calculatePower){
      requestTasks.add(request);
    }
  }
}
