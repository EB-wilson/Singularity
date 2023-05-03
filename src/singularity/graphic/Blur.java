package singularity.graphic;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Gl;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.Shader;
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
  public static final float[] DEf_C = {
      0.0045418484119f, 0.0539998665132f, 0.2419867245191f,
                        0.3989431211116f,
      0.2419867245191f, 0.0539998665132f, 0.0045418484119f,
  };
  public static final float[] DEf_D = {
      0.0245418484119f, 0.0639998665132f, 0.2519867245191f,
                        0.3189431211116f,
      0.2519867245191f, 0.0639998665132f, 0.0245418484119f,
  };
  public static final float[] DEf_E = {
      0.019615710072f, 0.2054255182127f,
      0.5599175434306f,
      0.2054255182127f, 0.019615710072f,
  };
  public static final float[] DEf_F = {
      0.0702702703f,  0.3162162162f,
      0.2270270270f,
      0.3162162162f, 0.0702702703f,
  };
  public static final float[] DEf_G = {
      0.2079819330264f,
      0.6840361339472f,
      0.2079819330264f,
  };
  public static final float[] DEf_H = {
      0.2561736558128f,
      0.4876526883744f,
      0.2561736558128f,
  };

  private final float[] convolution = new float[16];
  private int convLen;

  Shader blurShader;
  FrameBuffer buffer, pingpong;

  boolean capturing;

  public float blurSpace = 3.26f;

  public Blur(){
    blurShader = new Shader(Core.files.internal("shaders/screenspace.vert"), Singularity.getInternalFile("shaders").child("gaussian_blur.frag"));

    buffer = new FrameBuffer();
    pingpong = new FrameBuffer();

    blurShader.bind();
    blurShader.setUniformi("u_texture0", 0);
    blurShader.setUniformi("u_texture1", 1);

    setConvolution(DEf_F);
  }

  public void resize(int width, int height){
    buffer.resize(width, height);
    pingpong.resize(width, height);

    blurShader.bind();
    blurShader.setUniformf("size", width, height);
  }

  public void setConvolution(float... values){
    System.arraycopy(values, 0, convolution, 0, values.length);
    convLen = values.length;
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
    blurShader.setUniformMatrix4("convolution", convolution);
    blurShader.setUniformf("conv_len", convLen);
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
