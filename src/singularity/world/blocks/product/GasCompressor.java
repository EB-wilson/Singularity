package singularity.world.blocks.product;

import arc.util.Log;
import singularity.content.Gases;
import singularity.type.Gas;
import singularity.world.consumers.SglConsumers;
import singularity.world.products.Producers;
import mindustry.content.Items;

import java.lang.reflect.Field;

public class GasCompressor extends NormalCrafter{
  public float craftTime = 120f;
  public float powerUse = 2f;
  
  public GasCompressor(String name){
    super(name);
  }
  
  /**配方自动生成，此方法已无效*/
  @Override
  public SglConsumers newConsume(){
    return null;//方法已无效
  }
  
  /**配方自动生成，此方法已无效*/
  @Override
  public Producers newProduce(){
    return null;//方法已无效
  }
  
  
  public class GasCompressorBuild extends NormalCrafterBuild{
  
  }
}
