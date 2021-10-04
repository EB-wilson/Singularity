package singularity.world.blocks.function;

import arc.func.Func;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.world.consumers.ConsumePower;
import singularity.Sgl;
import singularity.Singularity;
import singularity.type.Gas;
import singularity.ui.SglStyles;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blocks.SglBlock;

public class GasCompressor extends SglBlock{
  public Func<GasCompressorBuild, Float> accessPressure = entity -> Sgl.atmospheres.current.getCurrPressure();
  public Func<Float, Float> compressPowerCons = pressure -> pressure/2;
  
  public GasCompressor(String name){
    super(name);
    consumesPower = hasPower = hasItems = hasLiquids = hasGases = true;
    outputsLiquid = outputGases = true;
    
    consumes.add(new ConsumePower(1, powerCapacity, false){
      @Override
      public float requestedPower(Building e){
        GasCompressorBuild entity = (GasCompressorBuild) e;
        return compressPowerCons.get(entity.currentPressure)*Mathf.num(entity.gases.getPressure() > entity.pressure());
      }
    });
  }
  
  public class GasCompressorBuild extends SglBuilding{
    float currentPressure;
  
    @Override
    public void handleGas(GasBuildComp source, Gas gas, float amount){
      super.handleGas(source, gas, amount);
    }
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return source.getBuilding().team == getBuilding().team && getGasBlock().hasGases() && gases.getPressure() < currentPressure;
    }
  
    @Override
    public float pressure(){
      return accessPressure.get(this);
    }
  
    @Override
    public void moveGas(GasBuildComp other, Gas gas){
      if(!other.getGasBlock().hasGases() || gases().get(gas) <= 0) return;
      float present = gases().get(gas)/gases().total();
  
      float fract = (gases.getPressure() - other.pressure())/Math.max(getGasBlock().maxGasPressure(), other.getGasBlock().maxGasPressure());
      float flowRate = Math.min(fract*getGasBlock().maxGasPressure()*getGasBlock().gasCapacity()*present, gases().get(gas));
  
      flowRate = Math.min(flowRate, other.getGasBlock().gasCapacity()*other.getGasBlock().maxGasPressure() - other.gases().get(gas));
      if(flowRate > 0.0F && fract > 0 && other.acceptGas(this, gas)){
        other.handleGas(this, gas, flowRate);
        gases().remove(gas, flowRate);
      }
    }
  
    @Override
    public void buildConfiguration(Table table){
      table.table(Styles.black6, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image(Singularity.getModAtlas("icon_pressure")).size(40)).size(50);
        t.slider(0, maxGasPressure, 0.01f, currentPressure, f -> currentPressure = f).size(200, 50).padLeft(8).padRight(8).get().setStyle(SglStyles.sliderLine);
        t.add("0").size(50).update(lable -> lable.setText(Strings.autoFixed(currentPressure*100, 2) + "kPa"));
      });
    }
  }
}
