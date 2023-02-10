package singularity.world.blocks.distribute;

import arc.Core;
import arc.util.Strings;
import mindustry.core.UI;
import mindustry.gen.Building;
import mindustry.ui.Bar;
import singularity.graphic.SglDrawConst;

public class DistEnergyManager extends DistNetBlock{
  public DistEnergyManager(String name){
    super(name);

    matrixEnergyCapacity = 2048;
    isNetLinker = true;
  }

  @Override
  public void setBars(){
    super.setBars();
    addBar("energyBuffered", (DistEnergyManagerBuild e) -> new Bar(
        () -> Core.bundle.format("bar.energyBuffered",
            e.matrixEnergyBuffered >= 1000 ? UI.formatAmount((long) e.matrixEnergyBuffered): Strings.autoFixed(e.matrixEnergyBuffered, 1),
            matrixEnergyCapacity >= 1000 ? UI.formatAmount((long) matrixEnergyCapacity): Strings.autoFixed(matrixEnergyCapacity, 1)),
        () -> SglDrawConst.matrixNet,
        () -> e.matrixEnergyBuffered/matrixEnergyCapacity
    ));
  }

  public class DistEnergyManagerBuild extends DistNetBuild{
    @Override
    public void updateNetLinked(){
      super.updateNetLinked();
      for(Building building: proximity){
        if(building instanceof DistEnergyEntry.DistEnergyEntryBuild entry){
          netLinked.add(entry);
        }
      }
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      updateNetLinked();
    }
  }
}
