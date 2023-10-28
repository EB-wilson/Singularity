package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.struct.ObjectMap;
import mindustry.world.meta.StatUnit;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.meta.SglStat;
import universecore.util.NumberStrify;

public class NetPluginComp extends DistNetBlock{
  public int computingPower = 0;
  public int topologyCapacity = 0;

  protected ObjectMap<DistBufferType<?>, Integer> bufferSize = new ObjectMap<>();

  public NetPluginComp(String name){
    super(name);
    update = true;
  }

  @Override
  public void setStats(){
    super.setStats();
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

  public void setBufferSize(DistBufferType<?> buffer, int size){
    bufferSize.put(buffer, size);
  }

  public class NetPluginCompBuild extends DistNetBuild{
    @Override
    public NetPluginComp block(){
      return (NetPluginComp) block;
    }

    @Override
    public void updateNetStat() {
      distributor.network.handleCalculatePower(computingPower);
      distributor.network.handleTopologyCapacity(topologyCapacity);
      for (ObjectMap.Entry<DistBufferType<?>, Integer> entry : bufferSize) {
        distributor.network.handleBufferCapacity(entry.key, entry.value);
      }
    }
  }
}
