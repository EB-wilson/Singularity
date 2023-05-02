package singularity.graphic;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.gl.FrameBuffer;
import mindustry.game.EventType;

import static singularity.graphic.SglShaders.baseShader;

public class ScreenSampler {
  private final static FrameBuffer samplerBuffer = new FrameBuffer();

  private static boolean activity;
  public static void setup() {
    if (activity)
      throw new RuntimeException("forbid setup sampler twice");

    Events.run(EventType.Trigger.preDraw, ScreenSampler::capture);
    Events.run(EventType.Trigger.uiDrawBegin, ScreenSampler::capture);
    Events.run(EventType.Trigger.uiDrawEnd, ScreenSampler::end);
    activity = true;
  }

  private static boolean capturing;

  private static void capture(){
    if (capturing) return;
    capturing = true;

    samplerBuffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
    samplerBuffer.begin(Color.clear);
  }

  private static void end(){
    capturing = false;

    samplerBuffer.end();
    samplerBuffer.blit(baseShader);
  }

  /**获取当前屏幕纹理，纹理对象是当前屏幕纹理的引用或者映射，它会随渲染过程同步变化，请勿使用此对象暂存屏幕数据
   * @return 屏幕采样纹理的引用对象*/
  public static Texture getSampler(){
    Draw.flush();
    return samplerBuffer.getTexture();
  }

  /**将当前屏幕纹理转存到一个{@linkplain FrameBuffer 帧缓冲区}，这将成为一份拷贝，可用于暂存屏幕内容
   *
   * @param target 用于转存屏幕纹理的目标缓冲区
   * @param clear 在转存之前是否清空帧缓冲区*/
  public static void getToBuffer(FrameBuffer target, boolean clear){
    target.resize(samplerBuffer.getWidth(), samplerBuffer.getHeight());
    if (clear){
      target.begin(Color.clear);
    }
    else target.begin();

    samplerBuffer.blit(baseShader);
    target.end();
  }
}
