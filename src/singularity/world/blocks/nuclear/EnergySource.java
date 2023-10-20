package singularity.world.blocks.nuclear;

import arc.math.Mathf;
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
    energyCapacity = 65536;
    outputEnergy = true;
    consumeEnergy = false;
    configurable = true;
    saveConfig = true;
    noUpdateDisabled = true;
    envEnabled = Env.any;
  }
  
  public class EnergySourceBuild extends NuclearNodeBuild {
    @Override
    public float moveEnergy(NuclearEnergyBuildComp next) {
      float adding = next.energyCapacity() - next.getEnergy();
      next.energy().handle(adding);
      energyMoved(next, adding);
      return adding;
    }

    @Override
    public float getEnergyMoveRate(NuclearEnergyBuildComp next) {
      return 1;
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
    public boolean acceptEnergy(NuclearEnergyBuildComp source){
      return false;
    }

    @Override
    public byte version() {
      return 5;
    }

    @Override
    public void read(Reads read, byte b){
      super.read(read, b);
      if (b < 5) read.f();
    }
  }
}
