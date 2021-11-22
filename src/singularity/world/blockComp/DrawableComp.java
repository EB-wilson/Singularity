package singularity.world.blockComp;

import mindustry.gen.Posc;
import universeCore.entityComps.blockComps.BuildCompBase;

public interface DrawableComp extends Posc, BuildCompBase{
  int rotation();
}
