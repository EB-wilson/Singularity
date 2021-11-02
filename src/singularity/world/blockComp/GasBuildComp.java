package singularity.world.blockComp;

import arc.struct.Seq;
import singularity.type.Gas;
import singularity.type.GasStack;
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
  
  default float moveGas(GasBuildComp other, Gas gas){
    if(gas == null) return moveGas(other);
    
    if(!other.getGasBlock().hasGases() || gases().get(gas) <= 0) return 0;
    float present = gases().get(gas)/gases().total();
    
    float fract = (outputPressure() - other.pressure())/Math.max(getGasBlock().maxGasPressure(), other.getGasBlock().maxGasPressure());
    float flowRate = Math.min(fract*getGasBlock().maxGasPressure()*getGasBlock().gasCapacity()*present, gases().get(gas));
    
    flowRate = Math.min(flowRate, other.getGasBlock().gasCapacity()*other.getGasBlock().maxGasPressure() - other.gases().get(gas));
    if(flowRate > 0.0F && fract > 0 && other.acceptGas(this, gas)){
      other.handleGas(this, gas, flowRate);
      gases().remove(gas, flowRate);
      other.onMoveGasThis(this, Seq.with(new GasStack(gas, flowRate)));
    }
    
    return flowRate;
  }
  
  default float moveGas(GasBuildComp other){
    if(!other.getGasBlock().hasGases()) return 0;
    float total = gases().total();
    float otherTotal = other.gases().total();
    
    float fract = (outputPressure() - other.pressure())/Math.max(getGasBlock().maxGasPressure(), other.getGasBlock().maxGasPressure());
    float flowRate = Math.min(fract*getGasBlock().maxGasPressure()*getGasBlock().gasCapacity(), total);
    flowRate = Math.min(flowRate, other.getGasBlock().gasCapacity()*other.getGasBlock().maxGasPressure() - otherTotal);
    
    float finalFlowRate = flowRate;
    Seq<GasStack> gases = new Seq<>();
    gases().each(stack -> {
      float present = gases().get(stack.gas)/total;
      
      float f = finalFlowRate*present;
      if(f > 0.0F && f > 0 && other.acceptGas(this, stack.gas)){
        gases.add(new GasStack(stack.gas, f));
        other.handleGas(this, stack.gas, f);
        gases().remove(stack.gas, f);
      }
    });
    
    if(gases.size > 0) other.onMoveGasThis(this, gases);
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
  
  default void onMoveGasThis(GasBuildComp source, Seq<GasStack> gases){}
  
  default void dumpGas(Gas gas){
    GasBuildComp other = (GasBuildComp) getDump(e -> {
      if(!(e instanceof GasBuildComp)) return false;
      return ((GasBuildComp)e).getGasBlock().hasGases() && ((GasBuildComp)e).acceptGas(this, gas);
    });
    if(other != null) moveGas(other, gas);
  }
  
  default void dumpGas(){
    GasBuildComp other = (GasBuildComp) getDump(e -> {
      if(!(e instanceof GasBuildComp)) return false;
      return ((GasBuildComp)e).getGasBlock().hasGases();
    });
    if(other != null) moveGas(other);
  }
  
  default boolean acceptGas(GasBuildComp source, Gas gas){
    return source.getBuilding().team == getBuilding().team && getGasBlock().hasGases() && pressure() < getGasBlock().maxGasPressure();
  }
}
