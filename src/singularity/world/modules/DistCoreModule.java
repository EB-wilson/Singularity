package singularity.world.modules;

import arc.struct.ObjectMap;
import singularity.world.blockComp.distributeNetwork.DistElementBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;

import java.util.PriorityQueue;

public class DistCoreModule extends DistributeModule{
  public PriorityQueue<DistRequestBase> requestTasks = new PriorityQueue<>();
  public int calculatePower;
  
  public ObjectMap<DistBuffers<?>, Object> buffers = new ObjectMap<>();
  
  public DistCoreModule(DistElementBuildComp entity){
    super(entity);
    for(DistBuffers<?> buffer: DistBuffers.all){
      buffers.put(buffer, buffer.get());
    }
  }
  
  @SuppressWarnings("unchecked")
  public <T extends BaseBuffer<?, ?>> T getBuffer(DistBuffers<T> buffer){
    return (T)buffers.get(buffer);
  }
  
  public void update(){
    if(network.netValid()){
      network.update();
  
      for(DistRequestBase request: requestTasks){
        if(!request.sleeping()) request.preHandle();
      }
      
      for(DistRequestBase request: requestTasks){
        if(request.finished()){
          requestTasks.remove(request);
        }
        else{
          if(!request.sleeping()) request.handle();
        }
      }
    }
  }
  
  public void receive(DistRequestBase request){
    if(requestTasks.size() <= calculatePower){
      requestTasks.add(request);
    }
  }
}
