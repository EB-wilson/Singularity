package singularity.world.blocks.product;

import singularity.world.components.MediumBuildComp;
import singularity.world.components.MediumComp;
import singularity.world.consumers.SglConsumeType;
import singularity.world.products.ProduceMedium;
import singularity.world.products.SglProduceType;
import universecore.annotations.Annotations;
import universecore.world.producers.BaseProducers;

@Annotations.ImplEntries
public class MediumCrafter extends NormalCrafter implements MediumComp{
  public float mediumCapacity = 16;
  public float lossRate = 0.01f;
  public float mediumMoveRate = 1.325f;
  public boolean outputMedium;

  public MediumCrafter(String name){
    super(name);
  }

  @Override
  public void init(){
    super.init();
    ProduceMedium<?> m;
    for(BaseProducers producer: producers){
      outputMedium |= (m = producer.get(SglProduceType.medium)) != null && (mediumMoveRate = m.product) > 0;
    }
  }

  @Annotations.ImplEntries
  public class MediumCrafterBuild extends NormalCrafterBuild implements MediumBuildComp{
    @Override
    public boolean acceptMedium(MediumBuildComp source){
      return consumer.current != null && consumer.current.get(SglConsumeType.medium) != null && MediumBuildComp.super.acceptMedium(source);
    }
  }
}
