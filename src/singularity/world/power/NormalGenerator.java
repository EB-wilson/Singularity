package singularity.world.power;

import singularity.world.blocks.product.NormalCrafter.NormalCrafterBuild;

/**普通的发电机，发电量在材料不足时逐渐减少，生产时间等于一次消耗的发电时长*/
public class NormalGenerator extends BaseGenerator{
  public NormalGenerator(NormalCrafterBuild entity){
    super(entity);
  }
  
  @Override
  public void update(){
    if(!entity.consValid() || !entity.shouldConsume()){
      if(entity.productionEfficiency > 0){
        entity.productionEfficiency -= entity.getProgressIncrease(entity.consumer.current.craftTime);
      }
      else entity.productionEfficiency = 0;
    }
  }
  
  @Override
  public void trigger(){
    entity.productionEfficiency = 1f;
  }
}
