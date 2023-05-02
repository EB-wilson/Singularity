package singularity.graphic;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Gl;
import arc.graphics.g2d.Draw;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.Shader;
import singularity.Singularity;

import static singularity.graphic.SglShaders.baseShader;

public class Blur {
  public Shader blurShader;
  FrameBuffer buffer, pingpong1, pingpong2;

  boolean capturing;

  public int blurPasses = 4;

  public Blur(){
    blurShader = new Shader(Core.files.internal("bloomshaders/blurspace.vert"), Singularity.getInternalFile("shaders").child("gaussian_blur.frag"));

    buffer = new FrameBuffer();
    pingpong1 = new FrameBuffer();
    pingpong2 = new FrameBuffer();

    blurShader.bind();
    blurShader.setUniformi("u_texture0", 0);
    blurShader.setUniformi("u_texture1", 1);
  }

  public void resize(int width, int height){
    buffer.resize(width, height);
    pingpong1.resize(width, height);
    pingpong2.resize(width, height);

    blurShader.bind();
    blurShader.setUniformf("size", width, height);
  }

  public void capture(){
    if (!capturing) {
      buffer.begin(Color.clear);

      capturing = true;
    }
  }

  public void render(){
    if (!capturing) return;
    capturing = false;
    buffer.end();

    Gl.disable(Gl.blend);
    Gl.disable(Gl.depthTest);
    Gl.depthMask(false);

    pingpong1.begin();
    Draw.blit(ScreenSampler.getSampler(), baseShader);
    pingpong1.end();

    for(int i = 0; i < blurPasses; i++){
      pingpong2.begin();
      blurShader.bind();
      blurShader.setUniformf("dir", 1f, 0f);
      pingpong1.getTexture().bind(1);
      buffer.blit(blurShader);
      pingpong2.end();

      pingpong1.begin();
      blurShader.bind();
      blurShader.setUniformf("dir", 0f, 1f);
      pingpong2.getTexture().bind(1);
      buffer.blit(blurShader);
      pingpong1.end();
    }
    pingpong1.blit(baseShader);

    Gl.enable(Gl.blend);
    Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha);
  }
}
