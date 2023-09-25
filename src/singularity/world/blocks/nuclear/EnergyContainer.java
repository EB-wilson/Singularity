package singularity.world.blocks.nuclear;

import arc.func.Cons;
import arc.util.Nullable;
import mindustry.world.meta.Stats;
import singularity.world.blocks.SglBlock;

public class EnergyContainer extends SglBlock {
  public float energyPotential = 256;
  @Nullable public Cons<EnergyContainerBuild> nonCons;
  @Nullable public Cons<Stats> setStats;

  public EnergyContainer(String name) {
    super(name);
  }

  @Override
  public void setStats() {
    super.setStats();
    if (setStats != null) setStats.get(stats);
  }

  public class EnergyContainerBuild extends SglBuilding {
    @Override
    public void updateTile() {
      super.updateTile();
      dumpEnergy();

      if (!consumers.isEmpty() && consumer != null && nonCons != null && !consumeValid()) nonCons.get(this);
    }

    @Override
    public float getInputPotential() {
      return Math.min(energyPotential, getEnergy());
    }


    @Override
    public float getOutputPotential() {
      return Math.min(energyPotential, getEnergy());
    }
  }
}
