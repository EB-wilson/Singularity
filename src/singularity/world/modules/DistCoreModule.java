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
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import universecore.util.colletion.TreeSeq;

import java.util.Arrays;

public class DistCoreModule extends BlockModule{
  private static final ObjectSet<DistRequestBase> blocked = new ObjectSet<>();
  public static final DistRequestBase[] EMP_TMP = new DistRequestBase[0];
  public static final DistRequestBase[] EMPTY = new DistRequestBase[0];

  protected DistRequestBase[] taskStack;
  protected DistRequestBase[] taskQueue = new DistRequestBase[16];

  public TreeSeq<DistRequestBase> requestTasks = new TreeSeq<>((a, b) -> b.priority() - a.priority());
  protected int queueLength = 0;

  public int lastProcessed;

  public int calculatePower;
  public int executingAddress;
  public final DistNetworkCoreComp core;

  public float process;
  
  public DistCoreModule(DistElementBuildComp entity){
    core = (DistNetworkCoreComp) entity;
  }
  
  public void update(){
    DistributeNetwork network = core.distributor().network;

    if(network.netValid()){
      process += Time.delta*network.netEfficiency();
    }

    while(process >= 1){
      process -= 1;

      lastProcessed = 0;
      queueLength = 0;
      blocked.clear();
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

          if (taskQueue.length <= queueLength) taskQueue = Arrays.copyOf(taskQueue, taskQueue.length*2);

          taskQueue[queueLength] = taskStack[executingAddress];
          queueLength++;

          taskStack[executingAddress].onExecute();
          taskStack[executingAddress].checkWaking();
          runCounter--;
          lastProcessed++;
        }
      }

      for (int i = 0; i < queueLength; i++) {
        DistRequestBase request = taskQueue[i];
        request.checkStatus();

        if(!request.sleeping()){
          if(!request.preHandle()){
            blocked.add(request);
            request.block(true);
          }
          else request.block(false);
        }
      }

      for (int i = 0; i < queueLength; i++) {
        DistRequestBase request = taskQueue[i];
        if(!request.sleeping() && !blocked.contains(request)){
          if(!request.handle()){
            blocked.add(request);
            request.block(true);
          }
          else request.block(false);
        }
      }

      for(BaseBuffer<?, ?, ?> buffer : core.buffers().values()){
        buffer.bufferContAssign(network);
        buffer.update();
      }

      for (int i = 0; i < queueLength; i++) {
        DistRequestBase request = taskQueue[i];
        if(!request.sleeping() && !blocked.contains(request)){
          request.block(!request.afterHandle());
        }
      }
    }

    for (int i = 0; i < queueLength; i++) {
      DistRequestBase req = taskQueue[i];
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
