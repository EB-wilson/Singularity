package singularity.world.particles;

import mindustry.graphics.Pal;
import universecore.world.particles.MultiParticleModel;
import universecore.world.particles.ParticleModel;
import universecore.world.particles.models.RandDeflectParticle;
import universecore.world.particles.models.ShapeParticle;
import universecore.world.particles.models.TrailFadeParticle;

public class SglParticleModels{
  public static ParticleModel nuclearParticle = new MultiParticleModel(
      new RandDeflectParticle(){{
        deflectAngle = 90;
        strength = 0.2f;
      }},
      new TrailFadeParticle(){{
        trailFade = 0.04f;
        fadeColor = Pal.lightishGray;
        colorLerpSpeed = 0.03f;
      }},
      new ShapeParticle()
  ){
    {
      color = Pal.reactorPurple;
      trailColor = Pal.reactorPurple;
    }
  };
}
