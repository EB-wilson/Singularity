package singularity.world.blocks.gas;

import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import singularity.Singularity;
import singularity.type.Gas;
import singularity.ui.SglStyles;
import singularity.world.blockComp.GasBuildComp;

public class PressureValve extends GasBlock{
  public PressureValve(String name){
    super(name);
    outputGases = true;
    saveConfig = true;
    configurable = true;
    canOverdrive = false;
  }
  
  @Override
  public void appliedConfig(){
    config(Float.class, (PressureValveBuild e, Float f) -> e.outputPressure = f);
    configClear((PressureValveBuild e) -> e.outputPressure = 0);
  }
  
  public class PressureValveBuild extends SglBuilding{
    public float outputPressure;
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return source.getBuilding().team == getBuilding().team && getGasBlock().hasGases() && pressure() < maxGasPressure;
    }
  
    @Override
    public Object config(){
      return outputPressure;
    }
  
    @Override
    public void updateTile(){
      dumpGas();
    }
  
    @Override
    public float outputPressure(){
      return Math.min(outputPressure, pressure());
    }
  
    @Override
    public void buildConfiguration(Table table){
      table.table(Styles.black6, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image(Singularity.getModAtlas("icon_pressure")).size(40)).size(50);
        t.slider(0, maxGasPressure, 0.01f, outputPressure, this::configure).size(200, 50).padLeft(8).padRight(8).get().setStyle(SglStyles.sliderLine);
        t.add("0").size(50).update(lable -> lable.setText(Strings.autoFixed(outputPressure*100, 2) + "kPa"));
      });
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      write.f(outputPressure);
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      outputPressure = read.f();
    }
  }
}
