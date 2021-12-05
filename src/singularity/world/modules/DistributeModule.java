package singularity.world.modules;

import arc.util.io.Writes;
import mindustry.world.modules.BlockModule;
import singularity.world.blockComp.distributeNetwork.DistElementBuildComp;
import singularity.world.distribution.DistributeNetwork;

public class DistributeModule extends BlockModule{
  public final DistElementBuildComp entity;
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
  
  public void update(){
    if(network.netValid()) network.update();
  }
  
  @Override
  public void write(Writes write){
  
  }
}
