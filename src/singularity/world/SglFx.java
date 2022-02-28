package singularity.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Vec2;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import universeCore.util.Functions;

import static arc.graphics.g2d.Draw.alpha;
import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.lineAngle;
import static arc.graphics.g2d.Lines.stroke;
import static arc.math.Angles.randLenVectors;

public class SglFx{
  public static Effect gasLeak = new Effect(90, e -> {
    if(!(e.data() instanceof Number)) return;
    float param = ((Number)e.data()).floatValue();
    
    Draw.color(e.color, Color.lightGray, e.fin());
    Draw.alpha(0.75f*param*e.fout());
    
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
  
    Draw.z(Layer.effect);
    randLenVectors(e.id, 6, 4f + (float)Functions.lerp(0, 9, 0.1f, move*40), (x, y) -> {
      color(Color.valueOf("FF756F"), Color.lightGray, Mathf.clamp(e.fin() + Mathf.random(-0.1f, 0.1f)));
      Fill.square(e.x + x, e.y + y, 0.2f + size * 2f + Mathf.random(-0.15f, 0.15f), 45);
    });
  }),
  
  impWave = new Effect(10f, e -> {
    Draw.color(Color.white);
    
    Lines.stroke(e.fout());
    Lines.circle(e.x, e.y, Mathf.randomSeed(e.id, 8, 10)*e.fin());
  }),
  
  explodeImpWave = new Effect(50f, e -> {
    Draw.color(Pal.reactorPurple);
    float rate = Mathf.pow(e.fout(), 2);
    float h = 30f*rate;
    float w = 6f*rate;
    
    Fill.quad(
        e.x + h, e.y,
        e.x, e.y + w,
        e.x - h, e.y,
        e.x, e.y - w
    );
    Fill.quad(
        e.x + w, e.y,
        e.x, e.y + h,
        e.x - w, e.y,
        e.x, e.y - h
    );
    Lines.stroke(2f*e.fout());
    Lines.circle(e.x, e.y, 14*e.fout());
    Lines.stroke(5f*e.fout());
    Lines.circle(e.x, e.y, 38*(1 - Mathf.pow(e.fout(), 3)));
    
    int[] counter = {0};
    randLenVectors(e.id, 12, 26, (x, y) -> {
      float size = Mathf.randomSeed(e.id + counter[0]++, 4f, 7f);
      Fill.circle(e.x + x*e.fin(), e.y + y*e.fin(), size*e.fout());
    });
  }),
  
  reactorExplode = new Effect(180, e -> {
    if(e.time <= 2) Fx.reactorExplosion.at(e.x, e.y);
    float size = e.data() instanceof Float? e.data(): 120;
    
    float fin1 = Mathf.clamp(e.fin()/0.1f);
    float fin2 = Mathf.clamp((e.fin() - 0.1f)/0.3f);
    
    Draw.color(Pal.reactorPurple);
    Lines.stroke(6*e.fout());
    Lines.circle(e.x, e.y, size*(1 - Mathf.pow(e.fout(), 3)));
    
    float h, w;
    float rate = e.fin() > 0.1f? 1 - fin2: fin1;
    h = size/2*rate;
    w = h/5;
  
    Lines.stroke(3f*rate);
    Lines.circle(e.x, e.y, h/2);
  
    Fill.quad(
        e.x + h, e.y,
        e.x, e.y + w,
        e.x - h, e.y,
        e.x, e.y - w
    );
    Fill.quad(
        e.x + w, e.y,
        e.x, e.y + h,
        e.x - w, e.y,
        e.x, e.y - h
    );
  
    float intensity = size/32 - 2.2f;
    float baseLifetime = 25f + intensity * 11f;
  
    color(Pal.reactorPurple2);
    alpha(0.7f);
    for(int i = 0; i < 4; i++){
      Mathf.rand.setSeed(e.id*2L + i);
      float lenScl = Mathf.rand.random(0.4f, 1f);
      int fi = i;
      e.scaled(e.lifetime * lenScl, b -> {
        randLenVectors(b.id + fi - 1, b.fin(Interp.pow10Out), (int)(2.9f * intensity), 22f * intensity, (x, y, in, out) -> {
          float fout = b.fout(Interp.pow5Out) * Mathf.rand.random(0.5f, 1f);
          float rad = fout * ((2f + intensity) * 2.35f);
        
          Fill.circle(b.x + x, b.y + y, rad);
          Drawf.light(b.x + x, b.y + y, rad * 2.5f, Pal.reactorPurple, 0.5f);
        });
      });
    }
  
    e.scaled(baseLifetime, b -> {
      Draw.color();
      b.scaled(5 + intensity * 2f, i -> {
        stroke((3.1f + intensity/5f) * i.fout());
        Lines.circle(b.x, b.y, (3f + i.fin() * 14f) * intensity);
        Drawf.light(b.x, b.y, i.fin() * 14f * 2f * intensity, Color.white, 0.9f * b.fout());
      });
    
      color(Pal.lighterOrange, Pal.reactorPurple, b.fin());
      stroke((2f * b.fout()));
    
      Draw.z(Layer.effect + 0.001f);
      randLenVectors(b.id + 1, b.finpow() + 0.001f, (int)(8 * intensity), 28f * intensity, (x, y, in, out) -> {
        lineAngle(b.x + x, b.y + y, Mathf.angle(x, y), 1f + out * 4 * (4f + intensity));
        Drawf.light(b.x + x, b.y + y, (out * 4 * (3f + intensity)) * 3.5f, Draw.getColor(), 0.8f);
      });
    });
  }),
  
  steam = new Effect(90, e -> {
    Vec2 motion = e.data() instanceof Vec2? e.data(): new Vec2(0, 0);
    float len = motion.len();
    Draw.color(Color.white);
    Draw.alpha(0.75f*e.fout());
    
    for(int i=0; i<5; i++){
      Vec2 curr = motion.cpy().rotate(Mathf.randomSeed(e.id, - 20, 20)).setLength(len*e.finpow());
      Fill.circle(e.x + curr.x, e.y + curr.y, Mathf.randomSeed(e.id, 3.5f, 5)*(0.3f + 0.7f*e.fslope()));
    }
  }),
  
  steamBreakOut = new Effect(24, e -> {
    float[] data = e.data instanceof float[]? e.data(): new float[]{18, 24, 0.3f};
    
    float leng = Mathf.random(data[0], data[1]);
    for(int i=0; i<4; i++){
      if(Mathf.chanceDelta(data[2])) steam.at(e.x, e.y, 0, new Vec2(leng*Geometry.d8(i*2 + 1).x, leng*Geometry.d8(i*2 + 1).y));
    }
  });
}
