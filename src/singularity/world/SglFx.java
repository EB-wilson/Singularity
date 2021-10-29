package singularity.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import mindustry.entities.Effect;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import universeCore.util.Functions;

import static arc.graphics.g2d.Draw.color;
import static arc.math.Angles.randLenVectors;

public class SglFx{
  public static Effect gasLeak = new Effect(90, e -> {
    if(!(e.data() instanceof Number)) return;
    float param = ((Number)e.data()).floatValue();
    
    Draw.color(e.color, Color.lightGray, e.fin());
    Draw.alpha(0.75f*param*e.fout());
    
    Draw.z(Layer.bullet);
    
    randLenVectors(e.id, 1, 8f + e.fin()*(param + 3), (x, y) -> {
      Fill.circle(e.x + x, e.y + y, 0.55f+e.fslope()*4.5f);
    });
  }),
  
  crystalConstructed = new Effect(60, e -> {
    Draw.color(Color.valueOf("FF756F"));
    Lines.stroke(4*e.fout());
    
    Draw.z(Layer.effect);
    Lines.square(e.x, e.y, 12*e.fin(), 45);
  }),
  
  hadronReconstruct = new Effect(60, e -> {
    Draw.color(Pal.reactorPurple);
    Lines.stroke(3f*e.fout());
    
    Draw.z(Layer.effect);
    randLenVectors(e.id, 3, 12, (x, y) -> {
      Lines.square(e.x + x, e.y + y, (14 + Mathf.randomSeed(e.id + (int)(x*y), -2, 2))*e.fin(), e.fin()*Mathf.randomSeed(e.id + (int)(x*y), -90, 90));
    });
  }),
  
  polymerConstructed = new Effect(60, e -> {
    Draw.color(Pal.reactorPurple);
    Lines.stroke(6*e.fout());
    
    Draw.z(Layer.effect);
    Lines.square(e.x, e.y, 30*e.fin());
    Lines.square(e.x, e.y, 30*e.fin(), 45);
  }),
  
  forceField = new Effect(45, e -> {
    float end = 4;
  
    Draw.color(e.color);
    if(e.data instanceof float[]){
      if(((float[])e.data)[0] > 0) end = 3;
      Draw.alpha(((float[])e.data)[1]);
    }
    float endRot = ((int)Math.ceil(e.rotation/45) + 1)*45;
    
    Draw.z(Layer.effect);
    Lines.stroke(Mathf.lerp(1.5f, 0.4f, e.fin()));
    Lines.square(e.x, e.y, Mathf.lerp(35, end, e.fin()), Mathf.lerp(e.rotation, endRot, e.fin()));
  }),
  
  FEXsmoke = new Effect(80, e -> {
    float move = Mathf.clamp(e.fin()/0.35f);
    float size = 1 - Mathf.clamp((e.fin() - 0.65f)/0.35f);
    
    randLenVectors(e.id, 6, 4f + (float)Functions.lerp(0, 9, 0.1f, move*40), (x, y) -> {
      color(Color.valueOf("FF756F"), Color.lightGray, Mathf.clamp(e.fin() + Mathf.random(-0.1f, 0.1f)));
      Fill.square(e.x + x, e.y + y, 0.2f + size * 2f + Mathf.random(-0.15f, 0.15f), 45);
    });
  });
}
