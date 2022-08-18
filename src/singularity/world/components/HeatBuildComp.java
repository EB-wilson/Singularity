package singularity.world.components;

import arc.func.Cons;
import arc.math.geom.Point2;
import mindustry.Vars;
import mindustry.ctype.MappableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Edges;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.contents.SglItems;
import singularity.type.Gas;
import singularity.world.modules.GasesModule;
import singularity.world.modules.SglLiquidModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;
import universecore.components.blockcomp.Takeable;
import universecore.util.handler.FieldHandler;

import static mindustry.Vars.world;
import static singularity.Sgl.atmospheres;

public interface HeatBuildComp extends BuildCompBase, Takeable{
  float heatR = 0.008314f;
  
  static float getLiquidHeatCapacity(Liquid liquid){
    return liquid.heatCapacity*8420;
  }
  
  static float getLiquidAbsTemperature(Liquid liquid){
    return (liquid.temperature)*730f;
  }
  
  static float getItemHeatCapacity(Item item){
    return item instanceof SglItems.SglItem? ((SglItems.SglItem) item).heatCapacity: 550;
  }
  
  static float getItemAbsTemperature(Item item){
    return item instanceof SglItems.SglItem? ((SglItems.SglItem) item).getTemperature()*((SglItems.SglItem) item).heatCapacity: atmospheres.current.getAbsTemperature()*550;
  }
  
  static float getTemperature(float temperature){
    return temperature - 273.15f;
  }
  
  @Annotations.BindField("heat")
  default float heat(){
    return 0;
  }
  
  @Annotations.BindField("heat")
  default void heat(float heat){}
  
  @Annotations.BindField("heatCapacity")
  default float heatCapacity(){
    return 0;
  }
  
  @Annotations.BindField("heatCapacity")
  default void heatCapacity(float value){}
  
  default void handleHeat(float delta){
    heat(heat() + delta);
  }
  
  default void swapHeat(){
    HeatBuildComp other = (HeatBuildComp)getNext("heat", e -> {
      if(!(e instanceof HeatBuildComp)) return false;
      return getMoveCoff((HeatBuildComp) e) >= 0 && ((HeatBuildComp) e).absTemperature() < absTemperature();
    });
    
    if(other != null){
      float otherTemp = other.absTemperature();
      float rate = getSwapRate(absTemperature(), otherTemp);
  
      Point2[] nearby = Edges.getEdges(getBlock().size);
      int count = 0;
      for(Point2 point : nearby){
        if(other == world.build(getBuilding().tile.x + point.x, getBuilding().tile.y + point.y)) count++;
      }
      float moveHeat = count*count*rate*getMoveCoff(other);
      handleHeat(-moveHeat);
      other.handleHeat(moveHeat);
    }
    
    float atmoTemp = atmospheres.current.getAbsTemperature();
    float moveHeat = getBlock().size*getBlock().size*getMoveCoff(null)*getSwapRate(absTemperature(), atmoTemp)*getBuilding().delta();

    handleHeat(-moveHeat);
    if(Vars.state.isCampaign()) atmospheres.current.handleHeat(moveHeat);
  }
  
  default float getSwapRate(float temp1, float temp2){
    float highTemp = Math.max(temp1, temp2);
    float lowTemp = Math.min(temp1, temp2);
  
    if(highTemp - lowTemp < 0.01f) return 0;
    float result = lowTemp == 0? 1: (temp1 - temp2)/(float)Math.log(highTemp/lowTemp)/60;
    if(Float.isNaN(result)) return 0;
    return result;
  }
  
  default float getMoveCoff(HeatBuildComp other){
    return other != null? (getHeatBlock().blockHeatCoff() + other.getHeatBlock().blockHeatCoff())/2: getHeatBlock().heatCoefficient();
  }
  
  default HeatBuildComp getHeatBuild(){
    return getBuilding(HeatBuildComp.class);
  }
  
  default HeatBlockComp getHeatBlock(){
    return getBlock(HeatBlockComp.class);
  }
  
  default float accurateHeatCapacity(){
    float[] gasHeatCapacity = {0};
    float[] baseHeatCapacity = {getHeatBlock().baseHeatCapacity()};
    
    if(this instanceof GasBuildComp gasComp){
      if(gasComp.getGasBlock().hasGases()){
        gasComp.gases().each((gas, amount) -> {
          gasHeatCapacity[0] += gas.heatCapacity*amount;
        });
      }
    }
    
    if(getBlock().hasLiquids) liquids().each((liquid, amount) -> {
      baseHeatCapacity[0] += getLiquidHeatCapacity(liquid)*amount;
    });
    
    if(getBlock().hasItems) items().each((item, amount) -> {
      baseHeatCapacity[0] += getItemHeatCapacity(item)*amount;
    });
    
    return this instanceof GasBuildComp? gasHeatCapacity[0] + baseHeatCapacity[0]: gasHeatCapacity[0];
  }
  
  default float getHeat(MappableContent target){
    if(target instanceof Liquid){
      return getLiquidAbsTemperature((Liquid) target)*getLiquidHeatCapacity((Liquid) target);
    }
    
    if(target instanceof Gas){
      return ((Gas) target).temperature*((Gas) target).heatCapacity;
    }
    
    if(target instanceof Item){
      return getItemAbsTemperature((Item) target);
    }
    
    return 0;
  }
  
  default void onOverTemperature(){}
  
  default void updateHeatCapacity(Liquid liquid, float amount){
    heatCapacity(heatCapacity() + getLiquidHeatCapacity(liquid)*amount);
  }
  
  default void updateHeatCapacity(Gas gas, float amount){
    heatCapacity(heatCapacity() + gas.heatCapacity*amount);
  }
  
  default void updateHeatCapacity(Item item, float amount){
    heatCapacity(heatCapacity() + amount*550f);
  }
  
  default float absTemperature(){
    return heat()/heatCapacity();
  }
  
  default float temperature(){
    return getTemperature(absTemperature());
  }
  
  default void setLiquidModule(Cons<LiquidModule> setter){
    setter.get(new SglLiquidModule(){
      @Override
      public void add(Liquid liquid, float amount){
        super.add(liquid, amount);
        handleHeat(getHeat(liquid)*amount);
        updateHeatCapacity(liquid, amount);
      }
    });
  }
  
  default void setGasesModule(Cons<GasesModule> setter){
    setter.get(new GasesModule((GasBuildComp)this, true){
      @Override
      public void add(Gas gas, float amount){
        super.add(gas, amount);
        handleHeat(getHeat(gas)*amount);
        updateHeatCapacity(gas, amount);
      }
    });
  }
  
  default void setItemModule(Cons<ItemModule> setter){
    setter.get(new ItemModule(){
      @Override
      public void add(Item item, int amount){
        super.add(item, amount);
        handleHeat(getHeat(item)*amount);
        updateHeatCapacity(item, amount);
      }
  
      @Override
      public void add(ItemModule items){
        items.each(this::add);
      }
  
      @Override
      public void remove(Item item, int amount){
        super.remove(item, amount);
        handleHeat(-getHeat(item)*amount);
        updateHeatCapacity(item, -amount);
      }
  
      @Override
      public void set(Item item, int amount){
        float delta = amount - items[item.id];
        total += delta;
        items[item.id] = amount;
        
        handleHeat(getHeat(item)*delta);
        updateHeatCapacity(item, delta);
      }
    });
  }
  
  /**重设所有物质存储模块用于更新比热容以及物质热
   * */
  default void setModules(){
    if(getBlock().hasItems) setItemModule(i -> FieldHandler.setValueDefault(this, "items", i));
    if(getBlock().hasLiquids) setLiquidModule(l -> FieldHandler.setValueDefault(this, "liquids", l));
    if(getBlock() instanceof GasBlockComp && getBlock(GasBlockComp.class).hasGases()) setGasesModule(g -> FieldHandler.setValueDefault(this, "gases", g));

    heat(atmospheres.current.getTemperature()*heatCapacity());
  }
}