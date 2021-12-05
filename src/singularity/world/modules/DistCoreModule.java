package singularity.world.modules;

import arc.struct.ObjectMap;
import arc.struct.Queue;
import singularity.world.blockComp.distributeNetwork.DistElementBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.request.DistRequestBase;

public class DistCoreModule extends DistributeModule{
  public Queue<DistRequestBase> requestTasks = new Queue<>();
  
  public ObjectMap<DistBuffers<?>, Object> buffers = new ObjectMap<>();
  
  public DistCoreModule(DistElementBuildComp entity){
    super(entity);
    for(DistBuffers<?> buffer: DistBuffers.all){
      buffers.put(buffer, buffer.get());
    }
  }
  
  @SuppressWarnings("unchecked")
  public <T> T getBuffer(DistBuffers<T> buffer){
    return (T)buffers.get(buffer);
  }
  
  @Override
  public void update(){
    super.update();
    
    DistRequestBase request;
    while(!requestTasks.isEmpty()){
      request = requestTasks.removeFirst();
    }
  }
}
