package singularity.core;

import arc.struct.Seq;
import mindustry.entities.units.BuildPlan;

public class SglEventTypes {
  public static class BuildPlanRotateEvent{
    public Seq<BuildPlan> plans;
    public int direction;
  }

  public static class BuildFlipRotateEvent{
    public Seq<BuildPlan> plans;
    public boolean x;
  }
}
