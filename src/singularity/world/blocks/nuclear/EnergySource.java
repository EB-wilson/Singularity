package singularity.world.blocks.nuclear;

import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.world.meta.Env;
import singularity.Singularity;
import singularity.ui.SglStyles;
import singularity.world.components.NuclearEnergyBuildComp;

public class EnergySource extends NuclearNode {
  public EnergySource(String name){
    super(name);
    energyCapacity = 8192;
    outputEnergy = true;
    consumeEnergy = false;
    energyBuffered = true;
    configurable = true;
    saveConfig = true;
    noUpdateDisabled = true;
    envEnabled = Env.any;
  }
  
  @Override
  public void appliedConfig(){
    super.appliedConfig();
    config(Float.class, (EnergySourceBuild tile, Float value) -> tile.outputEnergy = value);
    configClear((EnergySourceBuild tile) -> tile.outputEnergy = 0);
  }
  
  public class EnergySourceBuild extends NuclearNodeBuild {
    protected float outputEnergy = 0;
  
    @Override
    public void updateTile(){
      energy.set(outputEnergy);
      
      dumpEnergy();

      super.updateTile();
    }
    
    @Override
    public void displayEnergy(Table table){
      //不执行任何操作
    }
  
    @Override
    public Object config(){
      return outputEnergy;
    }
    
    @Override
    public void buildConfiguration(Table table){
      table.table(Styles.black6, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image(Singularity.getModAtlas("nuclear")).size(40)).size(50);
        t.slider(0, energyCapacity, 0.01f, outputEnergy, this::configure).size(200, 50).padLeft(8).padRight(8).get().setStyle(SglStyles.sliderLine);
        t.add("0").size(50).update(lable -> lable.setText(Strings.autoFixed(outputEnergy, 2) + "NF"));
      });
    }
  
    @Override
    public boolean acceptEnergy(NuclearEnergyBuildComp source){
      return false;
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      write.f(outputEnergy);
    }
    
    @Override
    public void read(Reads read, byte b){
      super.read(read, b);
      outputEnergy = read.f();
    }
  }
}
