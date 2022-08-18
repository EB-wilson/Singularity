package singularity.world.components;

import arc.math.Mathf;
import singularity.type.Gas;
import singularity.world.modules.GasesModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;
import universecore.components.blockcomp.Takeable;

/**Gases组件，为方块的Build增加气体相关操作
 * 若使用非默认命名则需要重写调用方法*/
public interface GasBuildComp extends BuildCompBase, Takeable{
  default GasBlockComp getGasBlock(){
    return getBlock(GasBlockComp.class);
  }
  
  @Annotations.BindField("gases")
  default GasesModule gases(){
    return null;
  }

  @Annotations.BindField("smoothPressure")
  default float smoothPressure(){
    return 0;
  }

  @Annotations.BindField("smoothPressure")
  default void smoothPressure(float value){}

  @Annotations.MethodEntry(entryMethod = "update", insert = Annotations.InsertPosition.HEAD)
  default void updateGases(){
    if(getGasBlock().hasGases()){
      gases().update(compressing());
      smoothPressure(Mathf.lerpDelta(smoothPressure(), outputPressure(), 0.02f));
    }
  }
  
  default void handleGas(GasBuildComp source, Gas gas, float amount){
    gases().add(gas, amount);
  }
  
  default float swellCoff(GasBuildComp source){
    return 1;
  }
  
  default float moveGas(GasBuildComp other, Gas gas){
    if(gas == null) return moveGas(other);
    
    if(!other.getGasBlock().hasGases() || gases().get(gas) <= 0) return 0;
    
    other = other.getGasDestination(this, gas);
    float present = gases().get(gas)/gases().total();
  
    float diff = outputPressure() - other.pressure();
    if(diff < 0.001f) return 0;
    float fract = diff*getGasBlock().gasCapacity();
    float oFract = diff*other.getGasBlock().gasCapacity();

    float flowRate = Math.min(Math.min(Math.min(fract, oFract)/other.swellCoff(this), gases().total()),
        (other.getGasBlock().maxGasPressure() - other.pressure())*other.getGasBlock().gasCapacity())*present;
    if(flowRate > 0 && fract > 0 && other.acceptGas(this, gas)){
      other.handleGas(this, gas, flowRate);
      gases().remove(gas, flowRate);
    }
    
    return flowRate;
  }
  
  default float moveGas(GasBuildComp other){
    if(!other.getGasBlock().hasGases()) return 0;
    float total = gases().total();
    
    float diff = outputPressure() - other.pressure();
    if(diff < 0.001f) return 0;
    float fract = diff*getGasBlock().gasCapacity();
    float oFract = diff*other.getGasBlock().gasCapacity();
    
    float flowRate = Math.min(Math.min(Math.min(fract, oFract)/other.swellCoff(this), gases().total()),
        (other.getGasBlock().maxGasPressure() - other.pressure())*other.getGasBlock().gasCapacity());

    gases().each((gas, amount) -> {
      GasBuildComp next = other.getGasDestination(this, gas);
      float present = gases().get(gas)/total;
      
      float f = flowRate*present;
      if(f > 0 && next.acceptGas(this, gas)){
        next.handleGas(this, gas, f);
        gases().remove(gas, f);
      }
    });

    return flowRate;
  }
  
  /**当前方块内部是否可以进行气体压缩*/
  default boolean compressing(){
    return !getGasBlock().compressProtect();
  }
  
  default float pressure(){
    return gases().getPressure();
  }
  
  default float outputPressure(){
    return pressure();
  }
  
  default void dumpGas(Gas gas){
    GasBuildComp other = (GasBuildComp) this.getNext("gases", e -> {
      if(!(e instanceof GasBuildComp)) return false;
      return ((GasBuildComp)e).getGasBlock().hasGases() && ((GasBuildComp)e).acceptGas(this, gas);
    });
    if(other != null) moveGas(other, gas);
  }
  
  default void dumpGas(){
    GasBuildComp other = (GasBuildComp) this.getNext("gases", e -> {
      if(!(e instanceof GasBuildComp)) return false;
      return ((GasBuildComp)e).getGasBlock().hasGases() && outputPressure() > ((GasBuildComp) e).pressure();
    });
    if(other != null) moveGas(other);
  }
  
  default boolean acceptGas(GasBuildComp source, Gas gas){
    return source.getBuilding().interactable(getBuilding().team) && getGasBlock().hasGases() && pressure() < getGasBlock().maxGasPressure();
  }
  
  default GasBuildComp getGasDestination(GasBuildComp entity, Gas gas){
    return this;
  }
}
