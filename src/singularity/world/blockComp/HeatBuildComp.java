package singularity.world.blockComp;

import arc.util.Log;
import mindustry.ctype.MappableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.type.Gas;
import singularity.world.modules.GasesModule;
import universeCore.entityComps.blockComps.BuildCompBase;
import universeCore.util.handler.FieldHandler;

import java.lang.reflect.Field;

import static singularity.Sgl.atmospheres;

public interface HeatBuildComp extends BuildCompBase{
  static float getLiquidHeatCapacity(Liquid liquid){
    return liquid.heatCapacity*8420;
  }
  
  static float getLiquidAbsTemperature(Liquid liquid){
    return (liquid.temperature)*730f;
  }
  
  static float getTemperature(float temperature){
    return temperature - 273.15f;
  }
  
  default float heat(){
    return getField(float.class, "heat");
  }
  
  default void heat(float heat){
    FieldHandler.setValue(this.getClass(), "heat", this, heat);
  }
  
  default void handleHeat(float delta){
    heat(heat() + delta);
  }
  
  default void swapHeat(){
    float atmoTemp = atmospheres.current.getTemperature();
    float highTemp = Math.max(absTemperature(), atmoTemp);
    float lowTemp = Math.max(atmoTemp/2, Math.min(absTemperature(), atmoTemp));
    
    float rate = (atmoTemp - absTemperature())/(float)Math.log(highTemp/lowTemp)/60;
    
    handleHeat(getBlock().size*getBlock().size*getHeatBlock().heatCoefficient()*rate*getBuilding().delta());
  }
  
  default HeatBuildComp getHeatBuild(){
    return getBuilding(HeatBuildComp.class);
  }
  
  default HeatBlockComp getHeatBlock(){
    return getBlock(HeatBlockComp.class);
  }
  
  default float accurateHeatCapacity(){
    float[] gasHeatCapacity = {0};
    float[] baseHeatCapacity = {getBlock().hasItems? items().total(): 0};
    
    if(this instanceof GasBuildComp){
      GasBuildComp gasComp = (GasBuildComp) this;
      if(gasComp.getGasBlock().hasGases()){
        gasComp.gases().each(stack -> {
          gasHeatCapacity[0] += stack.gas.heatCapacity*stack.amount;
        });
      }
    }
    
    liquids().each((liquid, amount) -> {
      baseHeatCapacity[0] += getLiquidHeatCapacity(liquid)*amount;
    });
    
    return (this instanceof GasBuildComp?
        (gasHeatCapacity[0] + baseHeatCapacity[0])/2:
        baseHeatCapacity[0]) + getHeatBlock().baseHeatCapacity();
  }
  
  default float getHeat(MappableContent target){
    if(target instanceof Liquid){
      return getTemperature(getLiquidAbsTemperature((Liquid) target))*getLiquidHeatCapacity((Liquid) target);
    }
    
    if(target instanceof Gas){
      return ((Gas) target).temperature*((Gas) target).heatCapacity;
    }
    
    if(target instanceof Item){
      return 1;
    }
    
    return 0;
  }
  
  default void onOverTemperature(){}
  
  default float heatCapacity(){
    return getField(float.class, "heatCapacity");
  }
  
  default void heatCapacity(float value){
    FieldHandler.setValue(this.getClass(), "heatCapacity", this, value);
  }
  
  default void updateHeatCapacity(Liquid liquid, float amount){
    heatCapacity(heatCapacity() + getLiquidHeatCapacity(liquid)*amount);
  }
  
  default void updateHeatCapacity(Gas gas, float amount){
    heatCapacity(heatCapacity() + gas.heatCapacity*amount);
  }
  
  default void updateHeatCapacity(Item item, float amount){
    heatCapacity(heatCapacity() + amount*459.29f);
  }
  
  default float absTemperature(){
    return heat()/heatCapacity();
  }
  
  default float temperature(){
    return getTemperature(absTemperature());
  }
  
  default void setLiquidModule(Field liquids){
    if(!LiquidModule.class.isAssignableFrom(liquids.getType())) throw new RuntimeException("error of set module to a non-LiquidModule var");
    
    FieldHandler.setValue(liquids, this, new LiquidModule(){
      @Override
      public void add(Liquid liquid, float amount){
        super.add(liquid, amount);
        handleHeat(getHeat(liquid)*amount);
        updateHeatCapacity(liquid, amount);
      }
    });
  }
  
  default void setGasesModule(Field gases){
    if(!(this instanceof GasBuildComp)) throw new RuntimeException("cannot set GasesModule on a Non-GasBuild building");
    if(!GasesModule.class.isAssignableFrom(gases.getType())) throw new RuntimeException("error of set module to a non-GasesModule var");
  
    FieldHandler.setValue(gases, this, new GasesModule((GasBuildComp) this){
      @Override
      public void add(Gas gas, float amount){
        super.add(gas, amount);
        handleHeat(getHeat(gas)*amount);
        updateHeatCapacity(gas, amount);
      }
    });
  }
  
  default void setItemModule(Field items){
    if(!ItemModule.class.isAssignableFrom(items.getType())) throw new RuntimeException("error of set module to a non-ItemModule var");
  
    FieldHandler.setValue(items, this, new ItemModule(){
      @Override
      public void add(Item item, int amount){
        super.add(item, amount);
        handleHeat(amount);
        updateHeatCapacity(item, amount);
      }
  
      @Override
      public void add(ItemModule items){
        items.each(this::add);
      }
  
      @Override
      public void remove(Item item, int amount){
        super.remove(item, amount);
        handleHeat(-amount);
        updateHeatCapacity(item, -amount);
      }
  
      @Override
      public void set(Item item, int amount){
        total += (amount - items[item.id]);
        items[item.id] = amount;
        
        handleHeat(total);
        updateHeatCapacity(item, total);
      }
    });
  }
  
  /**重设所有物质存储模块用于更新比热容以及物质热
   * */
  default void setModules(){
    try{
      if(getBlock().hasItems) setItemModule(this.getClass().getField("items"));
      if(getBlock().hasLiquids) setLiquidModule(this.getClass().getField("liquids"));
      if(this instanceof GasBuildComp && ((GasBuildComp)this).getGasBlock().hasGases()) setGasesModule(this.getClass().getField("gases"));
      
      heat(atmospheres.current.getTemperature()*heatCapacity());
    }catch(NoSuchFieldException e){
      Log.info(e);
    }
  }
}