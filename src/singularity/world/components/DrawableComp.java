package singularity.world.components;

import mindustry.gen.Posc;
import universecore.components.blockcomp.BuildCompBase;

public interface DrawableComp extends Posc, BuildCompBase{
  int rotation();
}
