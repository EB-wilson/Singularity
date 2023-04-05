package singularity.graphic;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.Shader;
import arc.util.Disposable;
import mindustry.game.EventType;
import mindustry.graphics.Layer;
import singularity.Singularity;

import static singularity.graphic.SglDraw.*;

public class Distortion implements Disposable {
  static FrameBuffer samplerBuffer = new FrameBuffer();
  static Shader baseShader;

  static {
    baseShader = new Shader(Core.files.internal("shaders/screenspace.vert"), Singularity.getInternalFile("shaders").child("dist_base.frag"));

    Events.run(EventType.Trigger.drawOver, () -> {
      Draw.draw(Layer.min, () -> {
        samplerBuffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        samplerBuffer.begin(Color.clear);
      });
      Draw.draw(Layer.end, () -> {
        samplerBuffer.end();
        samplerBuffer.blit(baseShader);
      });
    });
  }

  Shader distortion;
  FrameBuffer buffer, pingpong;
  boolean buffering, disposed;

  public Distortion(){
    distortion = new Shader(Core.files.internal("shaders/screenspace.vert"), Singularity.getInternalFile("shaders").child("distortion.frag"));

    buffer = new FrameBuffer();
    pingpong = new FrameBuffer();

    init();
  }

  public void init(){
    distortion.bind();
    distortion.setUniformi("u_texture0", 0);
    distortion.setUniformi("u_texture1", 1);
    distortion.setUniformf("strength", -64);

    resize();
  }

  public void resize(){
    buffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
    pingpong.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
  }

  public void capture(){
    if (buffering) return;

    samplerBuffer.end();
    pingpong.begin();
    samplerBuffer.blit(baseShader);
    pingpong.end();
    samplerBuffer.begin();

    buffering = true;
    buffer.begin(Color.clear);
  }

  public void render(){
    buffering = false;

    buffer.end();

    pingpong.getTexture().bind(1);
    distortion.bind();
    distortion.setUniformf("width", pingpong.getWidth());
    distortion.setUniformf("height", pingpong.getHeight());

    buffer.blit(distortion);
  }

  public void setStrength(float strength){
    distortion.bind();
    distortion.setUniformf("strength", strength);
  }

  @Override
  public void dispose() {
    buffer.dispose();
    pingpong.dispose();
    distortion.dispose();
    disposed = true;
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
