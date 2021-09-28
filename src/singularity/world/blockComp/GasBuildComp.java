package singularity.world.blockComp;

import singularity.type.Gas;
import singularity.world.modules.GasesModule;
import universeCore.entityComps.blockComps.BuildCompBase;
import universeCore.entityComps.blockComps.Dumpable;
import universeCore.entityComps.blockComps.FieldGetter;

/**Gases组件，为方块的Build增加气体相关操作
 * 必须创建的变量：
 * <pre>{@code
 *   GasesModule [gases]
 * }<pre/>
 * 若使用非默认命名则需要重写调用方法*/
public interface GasBuildComp extends BuildCompBase, FieldGetter, Dumpable{
  default GasBlockComp getGasBlock(){
    return getBlock(GasBlockComp.class);
  }
  
  default GasesModule gases(){
    return getField(GasesModule.class, "gases");
  }
  
  default void handleGas(GasBuildComp source, Gas gas, float amount){
    gases().add(gas, amount);
  }
  
  default void moveGas(GasBuildComp other, Gas gas){
    if(!other.getGasBlock().hasGases() || gases().get(gas) <= 0) return;
    float present = gases().get(gas)/gases().total();
    
    float fract = (pressure() - other.pressure())/Math.max(getGasBlock().maxGasPressure(), other.getGasBlock().maxGasPressure());
    float flowRate = Math.min(fract*getGasBlock().maxGasPressure()*getGasBlock().gasCapacity()*present, gases().get(gas));
    
    flowRate = Math.min(flowRate, other.getGasBlock().gasCapacity()*other.getGasBlock().maxGasPressure() - other.gases().get(gas));
    if(flowRate > 0.0F && fract > 0 && other.acceptGas(this, gas)){
      other.handleGas(this, gas, flowRate);
      gases().remove(gas, flowRate);
    }
  }
  
  default float pressure(){
    return gases().getPressure();
  }
  
  default void dumpGas(Gas gas){
    GasBuildComp other = (GasBuildComp) getDump(e -> {
      if(!(e instanceof GasBuildComp)) return false;
      return ((GasBuildComp)e).getGasBlock().hasGases() && ((GasBuildComp)e).acceptGas(this, gas);
    });
    if(other != null) moveGas(other, gas);
  }
  
  default void dumpGas(){
    gases().each(stack -> dumpGas(stack.gas));
  }
  
  default boolean acceptGas(GasBuildComp source, Gas gas){
    return source.getBuilding().team == getBuilding().team && getGasBlock().hasGases() && pressure() < getGasBlock().maxGasPressure();
  }
}
