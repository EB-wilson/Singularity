package singularity.world.modules;

import arc.struct.IntSeq;
import arc.util.io.Writes;
import mindustry.world.modules.BlockModule;
import singularity.world.blockComp.distributeNetwork.DistElementBuildComp;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.request.DistRequestBase;

public class DistributeModule extends BlockModule{
  public final DistElementBuildComp entity;
  public DistRequestBase lastAssign;
  public IntSeq distNetLinks = new IntSeq();
  public DistributeNetwork network;
  
  public DistributeModule(DistElementBuildComp entity){
    this.entity = entity;
  }
  
  public void setNet(){
    setNet(null);
  }
  
  public void setNet(DistributeNetwork net){
    if(net == null){
      new DistributeNetwork().add(entity);
    }
    else network = net;
  }
  
  public void assign(DistRequestBase request){
    if(network.netValid()){
      DistCoreModule core = network.cores.get(0).distributor();
      if(!core.requestTasks.contains(request)){
        core.receive(request);
        lastAssign = request;
      }
    }
  }
  
  @Override
  public void write(Writes write){
  
  }
}
