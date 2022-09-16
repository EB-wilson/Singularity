package singularity.world.particles;

import arc.math.Mathf;
import mindustry.graphics.Pal;
import universecore.world.particles.ParticleModel;

public class SglParticleModels{
  public static ParticleModel nuclearParticle = new ParticleModel().setDefault()
      .setDeflect(90, 0.2f)
      .setColor(Pal.reactorPurple)
      .resetCloudUpdate()
      .setTailFade(f -> Mathf.lerpDelta(f,0, 0.04f))
      .tailGradient(Pal.lightishGray, 0.04f);
}
