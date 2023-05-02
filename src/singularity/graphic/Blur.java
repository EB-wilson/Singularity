package singularity.graphic;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Gl;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.Shader;
import arc.math.Mat;
import singularity.Singularity;

public class Blur {
  public static final float[] DEf_A = {
      0.0086973240159f, 0.0359949776755f, 0.1093610049784f,
      0.2129658870149f, 0.2659615230194f, 0.2129658870149f,
      0.1093610049784f, 0.0359949776755f, 0.0086973240159f,
  };
  public static final float[] DEf_B = {
      0.0444086447005f, 0.0779944219933f, 0.1159966211046f,
      0.1673080561213f, 0.1885769121606f, 0.1673080561213f,
      0.1159966211046f, 0.0779944219933f, 0.0444086447005f,
  };

  public final Mat convolution = new Mat(DEf_B);
  Shader blurShader;
  FrameBuffer buffer, pingpong;

  boolean capturing;

  public float blurSpace = 2.36f;

  public Blur(){
    blurShader = new Shader(Core.files.internal("shaders/screenspace.vert"), Singularity.getInternalFile("shaders").child("gaussian_blur.frag"));

    buffer = new FrameBuffer();
    pingpong = new FrameBuffer();

    blurShader.bind();
    blurShader.setUniformi("u_texture0", 0);
    blurShader.setUniformi("u_texture1", 1);
  }

  public void resize(int width, int height){
    buffer.resize(width, height);
    pingpong.resize(width, height);

    blurShader.bind();
    blurShader.setUniformf("size", width, height);
  }

  public void setConvolution(float... values){
    convolution.set(values);
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

    pingpong.begin();
    blurShader.bind();
    blurShader.setUniformMatrix("convolution", convolution);
    blurShader.setUniformf("dir", blurSpace, 0f);
    ScreenSampler.getSampler().bind(1);
    buffer.blit(blurShader);
    pingpong.end();

    blurShader.bind();
    blurShader.setUniformf("dir", 0f, blurSpace);
    pingpong.getTexture().bind(1);
    buffer.blit(blurShader);

    Gl.enable(Gl.blend);
    Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha);
  }
}
