package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.struct.ObjectMap;
import mindustry.gen.Building;
import mindustry.world.meta.StatUnit;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.components.distnet.DistComponent;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.meta.SglStat;
import universecore.util.NumberStrify;

public class CoreNeighbourComponent extends DistNetBlock{
  public int topologyCapaity = 0;
  public int computingPower = 0;

  public ObjectMap<DistBufferType<?>, Integer> bufferSize = new ObjectMap<>();

  public CoreNeighbourComponent(String name){
    super(name);
    topologyUse = 0;
    isNetLinker = false;
  }

  @Override
  public void setStats(){
    super.setStats();
    if(topologyCapaity > 0) stats.add(SglStat.topologyCapacity, topologyCapaity);
    if(computingPower > 0) stats.add(SglStat.computingPower, computingPower*60, StatUnit.perSecond);
    if(bufferSize.size > 0){
      stats.add(SglStat.bufferSize, t -> {
        t.defaults().left().fillX().padLeft(10);
        t.row();
        for(ObjectMap.Entry<DistBufferType<?>, Integer> entry: bufferSize){
          if(entry.value <= 0) continue;
          t.add(Core.bundle.get("content." + entry.key.targetType().name() + ".name") + ": " + NumberStrify.toByteFix(entry.value, 2));
          t.row();
        }
      });
    }
  }

  public class CoreNeighbourComponentBuild extends DistNetBuild implements DistComponent{
    @Override
    public ObjectMap<DistBufferType<?>, Integer> bufferSize(){
      return bufferSize;
    }

    @Override
    public int topologyCapacity(){
      return topologyCapaity;
    }

    @Override
    public int computingPower() {
      return computingPower;
    }

    @Override
    public boolean componentValid(){
      for(Building building: proximity){
        if(building instanceof DistNetworkCoreComp core && distributor.network.getCore() == core) return true;
      }
      return false;
    }

    @Override
    public boolean linkable(DistElementBuildComp other){
      return false;
    }

    @Override
    public void updateNetLinked(){
      super.updateNetLinked();
      for(Building building: proximity){
        if(building instanceof DistNetworkCoreComp core){
          netLinked.add(core);
        }
      }
    }
  }
}
