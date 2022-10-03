package singularity.world.blocks.distribute;

import arc.Core;
import arc.util.Strings;
import mindustry.core.UI;
import mindustry.ui.Bar;
import singularity.graphic.SglDrawConst;
import singularity.world.components.distnet.DistElementBuildComp;

public class DistEnergyBuffer extends DistEnergyEntry{
  public float extraRequire = -1;

  public DistEnergyBuffer(String name){
    super(name);
  }

  @Override
  public void init(){
    super.init();
    if(extraRequire == -1){
      extraRequire = matrixEnergyCapacity*0.1f;
    }
  }

  @Override
  public void setBars(){
    super.setBars();
    addBar("energyBuffered", (DistEnergyBufferBuild e) -> new Bar(
        () -> Core.bundle.format("bar.energyBuffered",
            e.matrixEnergyBuffered >= 1000 ? UI.formatAmount((long) e.matrixEnergyBuffered): Strings.autoFixed(e.matrixEnergyBuffered, 1),
            matrixEnergyCapacity >= 1000 ? UI.formatAmount((long) matrixEnergyCapacity): Strings.autoFixed(matrixEnergyCapacity, 1)),
        () -> SglDrawConst.matrixNet,
        () -> e.matrixEnergyBuffered/matrixEnergyCapacity
    ));
  }

  public class DistEnergyBufferBuild extends DistEnergyEntryBuild{
    @Override
    public boolean linkable(DistElementBuildComp other){
      return false;
    }

    @Override
    public float extraEnergyRequire(){
      return matrixEnergyBuffered < matrixEnergyCapacity? 1: 0;
    }
  }
}
