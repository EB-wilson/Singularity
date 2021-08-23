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
  
  @Override
  public void init(){
    Field[] gases = Gases.class.getFields();
    consumers.clear();
    producers.clear();
    for(Field gasF: gases){
      try{
        Gas gas = (Gas)gasF.get(null);
        if(!gas.compressible && gas.tank == null) continue;
        super.newConsume();
        consume.gas(gas, (gas.tank != null? gas.tankContains: gas.compressRequire)/craftTime);
        consume.time(craftTime);
        consume.power(powerUse);
        
        super.newProduce();
        produce.item(gas.tank != null? gas.tank: gas.compressItem, 1);
      }
      catch(IllegalArgumentException | IllegalAccessException e){
        Log.err(e);
      }
    }
    super.init();
  }
  
  public class GasCompressorBuild extends NormalCrafterBuild{
  
  }
}
