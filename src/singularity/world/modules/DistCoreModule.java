package singularity.world.modules;

import arc.struct.ObjectSet;
import arc.struct.OrderedMap;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.world.modules.BlockModule;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import universecore.util.colletion.TreeSeq;

import java.util.LinkedList;

@SuppressWarnings("rawtypes")
public class DistCoreModule extends BlockModule{
  private static final ObjectSet<DistRequestBase> blocked = new ObjectSet<>();
  private static final LinkedList<DistRequestBase> tempQueue = new LinkedList<>();
  
  public TreeSeq<DistRequestBase> requestTasks = new TreeSeq<>((a, b) -> b.priority() - a.priority());
  protected DistRequestBase[] taskStack;
  
  public int calculatePower;
  public int executingAddress;
  public final DistNetworkCoreComp core;
  
  public OrderedMap<DistBuffers<?>, BaseBuffer<?, ?, ?>> buffers = new OrderedMap<>();
  
  public DistCoreModule(DistElementBuildComp entity){
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
    if(core.distributor().network.netValid()){
      core.distributor().network.update();
  
      blocked.clear();
      tempQueue.clear();
      int runCounter = calculatePower;
      if(requestTasks.removeIf(DistRequestBase::finished)) taskStack = requestTasks.toArray(new DistRequestBase[0]);
      if(!requestTasks.isEmpty()){
        int firstAddress = -1;
        while(runCounter > 0){
          executingAddress = (executingAddress + 1)%requestTasks.size();
          if(executingAddress == firstAddress) break;
          if(firstAddress == -1) firstAddress = executingAddress;
          tempQueue.addLast(taskStack[executingAddress]);
          taskStack[executingAddress].onExecute();
          taskStack[executingAddress].checkWaking();
          runCounter--;
        }
      }
      
      for(DistRequestBase request: tempQueue){
        request.checkStatus();

        if(!request.sleeping()){
          if(!request.preHandle()){
            blocked.add(request);
            request.block(true);
          }
          else request.block(false);
        }
      }
      
      for(DistRequestBase request: tempQueue){
        if(!request.sleeping() && !blocked.contains(request)){
          if(!request.handle()){
            blocked.add(request);
            request.block(true);
          }
          else request.block(false);
        }
      }
  
      for(DistRequestBase request : tempQueue){
        if(!request.sleeping() && !blocked.contains(request)){
          request.block(!request.afterHandle());
        }
      }
  
      for(BaseBuffer<?, ?, ?> buffer : buffers.values()){
        buffer.bufferContAssign(core.distributor().network);
        buffer.update();
      }
    }
  }
  
  public void receive(DistRequestBase request){
    requestTasks.add(request);
    taskStack = requestTasks.toArray(new DistRequestBase[0]);
  }
  
  @Override
  public void read(Reads read){
  
  }
  
  @Override
  public void write(Writes write){
  
  }
}
