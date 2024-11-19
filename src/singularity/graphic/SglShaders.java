package singularity.graphic;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.gl.Shader;
import arc.math.geom.Vec2;
import arc.util.Time;
import singularity.Singularity;

import static mindustry.Vars.renderer;

public class SglShaders {
  // scene space shaders
  public static Shader baseShader, simpleScreen;
  public static SglSurfaceShader boundWater;
  public static MaskShader alphaMask;
  public static MirrorFieldShader mirrorField;
  public static AlphaAdjust linearAlpha, lerpAlpha;

  // local space shaders
  public static WaveShader wave;

  private static final Fi internalShaderDir = Singularity.getInternalFile("shaders");

  public static void load() {
    simpleScreen = new Shader(Core.files.internal("shaders/screenspace.vert"), internalShaderDir.child("simple.frag"));
    baseShader = new Shader(Core.files.internal("shaders/screenspace.vert"), internalShaderDir.child("dist_base.frag"));
    boundWater = new SglSurfaceShader("boundwater");
    alphaMask = new MaskShader("alpha_mask");
    mirrorField = new MirrorFieldShader();
    wave = new WaveShader("wave");
    linearAlpha = new AlphaAdjust(internalShaderDir.child("linear_alpha_adjust.frag"));
    lerpAlpha = new AlphaAdjust(internalShaderDir.child("lerp_alpha_adjust.frag"));
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

  public static class MirrorFieldShader extends Shader {
    public Color waveMix = Color.white;
    public Vec2 offset = new Vec2(0, 0);
    public float stroke = 2;
    public float gridStroke = 0.8f;
    public float mixAlpha = 0.4f;
    public float alpha = 0.2f;
    public float maxThreshold = 1;
    public float minThreshold = 0.7f;
    public float waveScl = 0.03f;
    public float sideLen = 10;

    public MirrorFieldShader() {
      super(Core.files.internal("shaders/screenspace.vert"), internalShaderDir.child( "mirrorfield.frag"));
    }

    @Override
    public void apply(){
      setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2, Core.camera.position.y - Core.camera.height / 2);
      setUniformf("u_resolution", Core.camera.width, Core.camera.height);
      setUniformf("u_time", Time.time);

      setUniformf("offset", offset);
      setUniformf("u_step", stroke);
      setUniformf("mix_color", waveMix);
      setUniformf("mix_alpha", mixAlpha);
      setUniformf("u_stroke", gridStroke);
      setUniformf("u_alpha", alpha);
      setUniformf("wave_scl", waveScl);
      setUniformf("max_threshold", maxThreshold);
      setUniformf("min_threshold", minThreshold);
      setUniformf("side_len", sideLen);
    }
  }

  public static class MaskShader extends Shader {
    public Texture texture;

    public MaskShader(String fragment) {
      super(Core.files.internal("shaders/screenspace.vert"), internalShaderDir.child(fragment + ".frag"));
    }

    @Override
    public void apply() {
      setUniformi("u_texture", 1);
      setUniformi("u_mask", 0);

      texture.bind(1);
    }
  }

  public static class AlphaAdjust extends Shader{
    public float coef;

    public AlphaAdjust(Fi fragmentShader) {
      super(Core.files.internal("shaders/screenspace.vert"), fragmentShader);
    }

    @Override
    public void apply() {
      setUniformf("u_coef", coef);
    }
  }
}
