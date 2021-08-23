package singularity.world.power;

import singularity.world.blocks.product.NormalCrafter.NormalCrafterBuild;

/**发电机方案的基本类*/
public abstract class BaseGenerator {
  public NormalCrafterBuild entity;
  public BaseGenerator(NormalCrafterBuild entity){
    this.entity = entity;
  }
  
  public abstract void update();
  
  public abstract void trigger();
  
  public void draw(){}
  
  /**特殊触发器*/
  public void subTrigger(Object... parameter){
    
  }
}
