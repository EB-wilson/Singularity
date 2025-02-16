package singularity.graphic.graphic3d;

import arc.graphics.Mesh;
import universecore.annotations.Annotations;

@Annotations.ImplEntries
public class RendererObj implements RendererObject{
  private Mesh mesh;

  public Material material;
  public RendererObject parentObj;

  @Override
  public Material material() {
    return material;
  }

  @Override
  public Mesh mesh() {
    return mesh;
  }

  public void setMesh(Mesh mesh){
    material().shader.checkMesh(mesh);
    this.mesh = mesh;
  }

  @Override
  public RendererObject parent() {
    return parentObj;
  }

  @Override
  public void update() {}
}
