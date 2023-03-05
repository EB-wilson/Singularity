package singularity.graphic;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.Shader;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Disposable;
import mindustry.game.EventType;
import singularity.Singularity;
import universecore.util.Empties;
import universecore.util.handler.FieldHandler;

import java.lang.reflect.Field;
import java.util.Iterator;

import static singularity.graphic.SglDraw.*;

public class Distortion implements Disposable {
  Shader distortion, baseShader;
  FrameBuffer buffer, samplerBuffer;

  boolean buffering, disposed;
  boolean sampleComplete;

  final float sampleEndLayer;

  final Runnable r;

  public Distortion(float sampleMin, float sampleMax){
    init();

    sampleEndLayer = sampleMax;

    Events.run(EventType.Trigger.draw, r = () -> {
      Draw.draw(sampleMin, this::startSampling);
      Draw.draw(sampleMax, this::endSampling);
    });
  }

  public void init(){
    baseShader = new Shader(Core.files.internal("shaders/screenspace.vert"), Singularity.getInternalFile("shaders").child("dist_base.frag"));

    distortion = new Shader(Core.files.internal("shaders/screenspace.vert"), Singularity.getInternalFile("shaders").child("distortion.frag"));

    distortion.bind();
    distortion.setUniformi("u_texture0", 0);
    distortion.setUniformi("u_texture1", 1);
    distortion.setUniformf("strength", -64);

    buffer = new FrameBuffer();
    samplerBuffer = new FrameBuffer();
  }

  public void resize(){
    buffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
    samplerBuffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
  }

  public void startSampling(){
    if (!sampleComplete) return;
    samplerBuffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
    samplerBuffer.begin(Color.clear);

    sampleComplete = false;
  }

  public void endSampling(){
    if (sampleComplete) return;

    samplerBuffer.end();
    samplerBuffer.blit(baseShader);

    sampleComplete = true;
  }

  public void capture(){
    if (!sampleComplete)
      throw new RuntimeException("sample was not completed");

    if (buffering) return;

    buffering = true;
    buffer.begin(Color.clear);
  }

  public void pause(){
    if (!buffering) return;

    buffering = false;
    buffer.end();
  }

  public void continueDis(){
    if (buffering) return;

    buffering = true;
    buffer.begin();
  }

  public void render(){
    buffering = false;

    buffer.end();

    samplerBuffer.getTexture().bind(1);
    distortion.bind();
    distortion.setUniformf("width", samplerBuffer.getWidth());
    distortion.setUniformf("height", samplerBuffer.getHeight());
    buffer.blit(distortion);
  }

  public void setStrength(float strength){
    distortion.bind();
    distortion.setUniformf("strength", strength);
  }

  @Override
  public void dispose() {
    buffer.dispose();
    samplerBuffer.dispose();
    distortion.dispose();
    disposed = true;

    ObjectMap<Object, Seq<Cons<?>>> map = FieldHandler.getValueDefault(Events.class, "events");
    Iterator<Cons<?>> itr = map.get(EventType.Trigger.draw, Empties.nilSeq()).iterator();
    if(!itr.hasNext()) return;
    a: for(Cons<?> c = itr.next(); itr.hasNext(); c = itr.next()){
      for(Field field: c.getClass().getDeclaredFields()){
        if(FieldHandler.getValueDefault(c, field.getName()) == r){
          itr.remove();
          break a;
        }
      }
    }

  }

  @Override
  public boolean isDisposed() {
    return disposed;
  }

  public static void drawVoidDistortion(float x, float y, float radius, float len){
    drawVoidDistortion(x, y, radius, len, true, Lines.circleVertices(radius));
  }

  public static void drawVoidDistortion(float x, float y, float radius, float len, boolean inside){
    drawVoidDistortion(x, y, radius, len, inside, Lines.circleVertices(radius));
  }

  public static void drawVoidDistortion(float x, float y, float radius, float len, boolean inside, int sides){
    v1.set(radius, 0);
    v2.set(radius, 0);
    v3.set(radius + len, 0);
    v4.set(radius + len, 0);
    v5.set(inside? -1: 1, 0);
    v6.set(inside? -1: 1, 0);

    float step = 360f/sides;
    for (int i = 0; i < sides; i++){
      v1.setAngle(step*i);
      v2.setAngle(step*(i+1));
      v3.setAngle(step*i);
      v4.setAngle(step*(i+1));
      v5.setAngle(step*i);
      v6.setAngle(step*(i+1));

      float cf1 = c1.set((v5.x + 1)/2, (v5.y + 1)/2, inside? 1: 0, inside? 1: 0).toFloatBits();
      float cf2 = c1.set((v6.x + 1)/2, (v6.y + 1)/2, inside? 1: 0, inside? 1: 0).toFloatBits();
      float cf3 = c1.set((v5.x + 1)/2, (v5.y + 1)/2, inside? 0: 1, inside? 0: 1).toFloatBits();
      float cf4 = c1.set((v6.x + 1)/2, (v6.y + 1)/2, inside? 0: 1, inside? 0: 1).toFloatBits();

      Fill.quad(
          x + v1.x, y + v1.y, cf1,
          x + v2.x, y + v2.y, cf2,
          x + v4.x, y + v4.y, cf4,
          x + v3.x, y + v3.y, cf3
      );
    }
  }
}
