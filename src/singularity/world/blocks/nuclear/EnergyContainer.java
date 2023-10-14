package singularity.world.blocks.nuclear;

import arc.func.Cons;
import arc.math.Mathf;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.world.meta.Stats;
import singularity.world.blocks.SglBlock;
import singularity.world.components.NuclearEnergyBuildComp;

public class EnergyContainer extends SglBlock {
  public float energyPotential = 256;
  public float warmupSpeed = 0.04f;

  @Nullable public Cons<EnergyContainerBuild> nonCons;
  @Nullable public Cons<Stats> setStats;
  public EnergyContainer(String name) {
    super(name);
    hasEnergy = true;
  }

  @Override
  public void setStats() {
    super.setStats();
    if (setStats != null) setStats.get(stats);
  }

  public class EnergyContainerBuild extends SglBuilding {
    public float warmup;

    @Override
    public float warmup() {
      return warmup;
    }

    @Override
    public float consEfficiency() {
      return super.consEfficiency()*warmup;
    }

    @Override
    public void updateTile() {
      super.updateTile();
      dumpEnergy();

      warmup = Mathf.lerpDelta(warmup, shouldConsume() && consumeValid()? 1: 0, warmupSpeed);

      if (!consumers.isEmpty() && consumer != null && nonCons != null && (!consumeValid() || !shouldConsume())) nonCons.get(this);
    }

    @Override
    public float getEnergyPressure(NuclearEnergyBuildComp other) {
      return other instanceof EnergyContainerBuild? getEnergy() - other.getEnergy(): super.getEnergyPressure(other);
    }

    @Override
    public float getInputPotential() {
      return Math.min(energyPotential, getEnergy());
    }


    @Override
    public float getOutputPotential() {
      return Math.min(energyPotential, getEnergy());
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      warmup = read.f();
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.f(warmup);
    }
  }
}
