package singularity.world.modules;

import universeCore.entityComps.blockComps.ProducerBuildComp;
import universeCore.world.blockModule.BaseProductModule;
import universeCore.world.producers.BaseProducers;

import java.util.List;

public class SglProductModule extends BaseProductModule{
  
  public SglProductModule(ProducerBuildComp entity, List<BaseProducers> producers){
    super(entity, producers);
  }
}
