package singularity.graphic;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.gl.FrameBuffer;
import mindustry.Vars;
import mindustry.game.EventType;

import static singularity.graphic.SglShaders.baseShader;

public class ScreenSampler {
  private final static FrameBuffer samplerBuffer = new FrameBuffer(), pingpong = new FrameBuffer();

  public static void init() {
    Events.run(EventType.Trigger.preDraw, () -> {
      if (!Vars.state.isMenu()) {
        samplerBuffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        samplerBuffer.begin(Color.clear);
      }
    });
    Events.run(EventType.Trigger.uiDrawBegin, () -> {
      if (Vars.state.isMenu()) {
        samplerBuffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        samplerBuffer.begin(Color.clear);
      }
    });
    Events.run(EventType.Trigger.uiDrawEnd, () -> {
      samplerBuffer.end();
      samplerBuffer.blit(baseShader);
    });
  }

  public static Texture getSampler(){
    pingpong.resize(Core.graphics.getWidth(), Core.graphics.getHeight());

    pingpong.begin();
    samplerBuffer.blit(baseShader);
    pingpong.end();

    return pingpong.getTexture();
  }
}
