package singularity.graphic.graphic3d;

import arc.graphics.Color;
import arc.graphics.gl.Shader;
import arc.math.geom.Vec3;

public class LightSource {
  public final Vec3 position = new Vec3();

  public final Color color = new Color();
  public float radius = 500;
  public float intensity = 1;
  public float attenuation = 3.8f;

  public void update(){}

  public void apply(Shader shader, int off){
    shader.setUniformf("u_light[" + off + "].position", position);
    shader.setUniformf("u_light[" + off + "].color", color);
    shader.setUniformf("u_light[" + off + "].radius", radius);
    shader.setUniformf("u_light[" + off + "].attenuation", attenuation);
  }

  public void set(LightSource light) {
    position.set(light.position);
    color.set(light.color);
    radius = light.radius;
    intensity = light.intensity;
    attenuation = light.attenuation;
  }
}
