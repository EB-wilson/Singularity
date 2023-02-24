package singularity.graphic;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.gl.Shader;
import arc.util.Time;
import singularity.Singularity;

import static mindustry.Vars.renderer;

public class SglShaders {
  public static SglSurfaceShader boundWater;
  public static WaveShader wave;

  private static final Fi internalShaderDir = Singularity.getInternalFile("shaders");

  public static void load() {
    boundWater = new SglSurfaceShader("boundwater");
    wave = new WaveShader("wave");
  }

  public static class SglSurfaceShader extends Shader {
    Texture noiseTex;

    public SglSurfaceShader(String frag){
      super(Core.files.internal("shaders/screenspace.vert"), internalShaderDir.child(frag + ".frag"));
      loadNoise();
    }

    public SglSurfaceShader(String vertRaw, String fragRaw){
      super(vertRaw, fragRaw);
      loadNoise();
    }

    public void loadNoise(){
      Core.assets.load("sprites/noise.png", Texture.class).loaded = t -> {
        t.setFilter(Texture.TextureFilter.linear);
        t.setWrap(Texture.TextureWrap.repeat);
      };
    }

    @Override
    public void apply(){
      setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2, Core.camera.position.y - Core.camera.height / 2);
      setUniformf("u_resolution", Core.camera.width, Core.camera.height);
      setUniformf("u_time", Time.time);

      if(hasUniform("u_noise")){
        if(noiseTex == null){
          noiseTex = Core.assets.get("sprites/noise.png", Texture.class);
        }

        noiseTex.bind(1);
        renderer.effectBuffer.getTexture().bind(0);

        setUniformi("u_noise", 1);
      }
    }
  }

  public static class WaveShader extends Shader{
    public Color waveMix = Color.white;
    public float mixAlpha = 0.4f;
    public float mixOmiga = 0.75f;
    public float maxThreshold = 0.9f;
    public float minThreshold = 0.6f;
    public float waveScl = 0.2f;

    public WaveShader(String frag){
      super(Core.files.internal("shaders/screenspace.vert"), internalShaderDir.child(frag + ".frag"));
    }

    @Override
    public void apply(){
      setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2, Core.camera.position.y - Core.camera.height / 2);
      setUniformf("u_resolution", Core.camera.width, Core.camera.height);
      setUniformf("u_time", Time.time);

      setUniformf("mix_color", waveMix);
      setUniformf("mix_alpha", mixAlpha);
      setUniformf("mix_omiga", mixOmiga);
      setUniformf("wave_scl", waveScl);
      setUniformf("max_threshold", maxThreshold);
      setUniformf("min_threshold", minThreshold);
    }
  }
}
