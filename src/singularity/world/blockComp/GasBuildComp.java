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
    if(!other.getGasBlock().hasGases()) return;
    float pressureDiff = gases().getPressure() - other.gases().getPressure();
    if(pressureDiff < 0) return;
    if(other.acceptGas(this, gas, pressureDiff/2)){
      other.handleGas(this, gas, pressureDiff/2);
      handleGas(other, gas, -pressureDiff/2);
    }
  }
  
  default void dumpGas(Gas gas){
    GasBuildComp other = (GasBuildComp) getDump(e -> {
      if(!(e instanceof GasBuildComp)) return false;
      float pressureDiff = gases().getPressure() - ((GasBuildComp)e).gases().getPressure();
      return ((GasBuildComp)e).getGasBlock().hasGases() && ((GasBuildComp)e).acceptGas(this, gas, pressureDiff/2);
    });
    moveGas(other, gas);
  }
  
  default boolean acceptGas(GasBuildComp source, Gas gas, float amount){
    return source.getBuilding().team == getBuilding().team && gases().getPressure() + amount/getGasBlock().gasCapacity() < getGasBlock().maxGasPressure();
  }
}
