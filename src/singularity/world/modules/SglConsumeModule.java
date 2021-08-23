package singularity.world.modules;

import universeCore.entityComps.blockComps.ConsumerBuildComp;
import universeCore.world.blockModule.BaseConsumeModule;
import universeCore.world.consumers.BaseConsumers;

import java.util.ArrayList;

public class SglConsumeModule extends BaseConsumeModule{
  public SglConsumeModule(ConsumerBuildComp entity, ArrayList<BaseConsumers> cons, ArrayList<BaseConsumers> optional){
    super(entity, cons, optional);
  }
}
