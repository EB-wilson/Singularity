package singularity.world.modules;

import arc.struct.ObjectSet;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.world.modules.BlockModule;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import universecore.util.colletion.TreeSeq;

@SuppressWarnings("rawtypes")
public class DistCoreModule extends BlockModule{
  private static final ObjectSet<DistRequestBase> blocked = new ObjectSet<>();
  private static final Seq<DistRequestBase> tempQueue = new Seq<>();
  public static final DistRequestBase[] EMP_TMP = new DistRequestBase[0];
  public static final DistRequestBase[] EMPTY = new DistRequestBase[0];

  public TreeSeq<DistRequestBase> requestTasks = new TreeSeq<>((a, b) -> b.priority() - a.priority());
  protected DistRequestBase[] taskStack;
  
  public int calculatePower;
  public int executingAddress;
  public final DistNetworkCoreComp core;

  public float process;
  
  public OrderedMap<DistBuffers<?>, BaseBuffer<?, ?, ?>> buffers = new OrderedMap<>();
  
  public DistCoreModule(DistElementBuildComp entity){
    core = (DistNetworkCoreComp) entity;
    for(DistBuffers<?> buffer: DistBuffers.all){
      buffers.put(buffer, buffer.get(core.bufferSize().get(buffer)));
    }
  }
  
  @SuppressWarnings("unchecked")
  public <T extends BaseBuffer<?, ?, ?>> T getBuffer(DistBuffers<T> buffer){
    return (T)buffers.get(buffer);
  }
  
  public void update(){
    DistributeNetwork network = core.distributor().network;

    if(network.netValid()){
      process += Time.delta*network.netEfficiency();
    }

    while(process >= 1){
      process -= 1;

      blocked.clear();
      tempQueue.clear();
      int runCounter = calculatePower;
      if(requestTasks.removeIf(
          task -> task.finished() || task.sender.distributor().network != network
      )) taskStack = requestTasks.toArray(EMP_TMP);

      if(!requestTasks.isEmpty()){
        int firstAddress = -1;
        while(runCounter > 0){
          executingAddress = (executingAddress + 1)%requestTasks.size();
          if(executingAddress == firstAddress) break;
          if(firstAddress == -1) firstAddress = executingAddress;

          tempQueue.add(taskStack[executingAddress]);
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
        buffer.bufferContAssign(network);
        buffer.update();
      }
    }

    for(DistRequestBase req: tempQueue){
      req.resetCallBack();
    }
  }
  
  public void receive(DistRequestBase request){
    requestTasks.add(request);
    taskStack = requestTasks.toArray(EMPTY);
  }
  
  @Override
  public void read(Reads read){
  
  }
  
  @Override
  public void write(Writes write){
  
  }
}
