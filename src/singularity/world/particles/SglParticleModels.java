package singularity.world.particles;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.Bullet;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import universecore.world.particles.MultiParticleModel;
import universecore.world.particles.Particle;
import universecore.world.particles.ParticleModel;
import universecore.world.particles.models.DrawDefaultTrailParticle;
import universecore.world.particles.models.RandDeflectParticle;
import universecore.world.particles.models.ShapeParticle;
import universecore.world.particles.models.TrailFadeParticle;

public class SglParticleModels{
  public static final String OWNER = "owner";

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
      new ShapeParticle(),
      new DrawDefaultTrailParticle()
  ){
    {
      color = Pal.reactorPurple;
      trailColor = Pal.reactorPurple;
    }
  },

  heatBulletTrail = new ParticleModel(){
    static final Particle.Cloud tmp1 = new Particle.Cloud();
    static final Particle.Cloud tmp2 = new Particle.Cloud();

    {
      color = Pal.lighterOrange;
      trailColor = Pal.lighterOrange;
    }

    @Override
    public void drawTrail(Particle c) {
      for (Particle.Cloud cloud : c) {
        tmp1.color.set(Color.black);
        tmp1.x = cloud.x;
        tmp1.y = cloud.y;
        tmp1.size = cloud.size/2;
        tmp1.perCloud = cloud.perCloud;
        tmp1.nextCloud = tmp2;

        tmp2.x = cloud.nextCloud.x;
        tmp2.y = cloud.nextCloud.y;
        tmp2.size = cloud.nextCloud.size/2;

        Draw.z(Layer.bullet - 1);
        tmp1.draw();
        Draw.z(Layer.effect);
        cloud.draw();
        tmp1.draw();
      }
    }

    @Override
    public void draw(Particle p) {
      Draw.z(Layer.bullet - 1);
      Fill.circle(p.x, p.y, p.size);
      Draw.z(Layer.effect);
      Lines.stroke(1, Pal.lighterOrange);
      Lines.circle(p.x, p.y, p.size);
    }

    @Override
    public float currSize(Particle p) {
      return p.getVar(OWNER) instanceof Bullet b && b.isAdded()? p.defSize: Mathf.approachDelta(p.size, 0, 0.04f);
    }

    @Override
    public void update(Particle p) {
      super.update(p);
      if (p.getVar(OWNER) instanceof Bullet b) {
        if (b.isAdded()) {
          p.set(b.x, b.y);
        }
        else p.setVar(OWNER, null);
      }
    }

    @Override
    public void updateTrail(Particle p, Particle.Cloud c) {
      super.updateTrail(p, c);
      c.size -= 0.03f*Time.delta;
      float dx = c.nextCloud.x - c.x;
      float dy = c.nextCloud.y - c.y;
    }

    @Override
    public boolean isFinal(Particle p) {
      return !(p.getVar(OWNER) instanceof Bullet b && b.isAdded()) && p.size <= 0.04f;
    }
  };
}
