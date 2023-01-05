package singularity.graphic;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.gl.Shader;
import arc.util.Time;
import singularity.Singularity;

import static mindustry.Vars.renderer;

public class SglShaders {
  public static SglSurfaceShader boundWater;

  private static final Fi internalShaderDir = Singularity.getInternalFile("shaders");

  public static void load() {
    boundWater = new SglSurfaceShader("boundwater");
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
}
