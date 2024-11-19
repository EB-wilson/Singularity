package singularity.game;

import singularity.game.researchs.ResearchProject;
import singularity.game.researchs.RevealGroup;

@SuppressWarnings("ClassCanBeRecord")
public class SglEvents {
  public static class ResearchCompletedEvent{
    public final ResearchProject research;

    public ResearchCompletedEvent(ResearchProject research) {
      this.research = research;
    }
  }

  public static class RevealedEvent{
    public final RevealGroup group;

    public RevealedEvent(RevealGroup group) {
      this.group = group;
    }
  }
}
