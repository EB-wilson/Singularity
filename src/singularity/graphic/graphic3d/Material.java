package singularity.graphic.graphic3d;

import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.Shader;
import arc.math.geom.Mat3D;
import arc.struct.Seq;

public abstract class Material implements Comparable<Material> {
  private static final Mat3D matrix = new Mat3D();


  public float priority = 0;

  protected Pass[] passes;
  protected FrameBuffer gBuffer;
  protected Shader baseShader;
  protected boolean drawingObjects = false;

  public Material(){
    init();
  }

  public void init(){
    passes = buildPasses();
    baseShader = buildShader();
    gBuffer = buildBuffer();
  }

  protected abstract Pass[] buildPasses();
  protected abstract Shader buildShader();
  protected abstract FrameBuffer buildBuffer();
  protected abstract void applyShader(Shader shader);

  public void reset() {
    gBuffer.begin(Color.clear);
    gBuffer.end();
  }

  public void begin(){
    if (drawingObjects) return;
    drawingObjects = true;

    gBuffer.begin();
  }

  public void drawObject(RendererObject object){
    if (!drawingObjects) throw new IllegalStateException("Material passing not started");

    Shader shader = baseShader;
    applyShader(shader);
    shader.setUniformMatrix4("u_transform", object.getTransform(matrix).val);

    object.mesh().render(shader, object.verticesPrimitive(), object.meshOffset(), object.meshCount());
  }

  public void drawObject(RendererObject object, Mat3D parentTrn){
    if (!drawingObjects) throw new IllegalStateException("Material passing not started");

    Shader shader = baseShader;
    applyShader(shader);
    shader.setUniformMatrix4("u_transform", parentTrn == null ?
        object.getTransform(matrix).val :
        object.getTransform(matrix).mul(parentTrn).val
    );

    object.mesh().render(shader, object.verticesPrimitive(), object.meshOffset(), object.meshCount());
  }

  public void end(){
    if (!drawingObjects) return;
    drawingObjects = false;

    gBuffer.end();
  }

  public void passing(){
    Seq<Texture> lastTextures = gBuffer.getTextureAttachments();
    for (Pass pass : passes){
      lastTextures = pass.pass(lastTextures);
    }
  }

  public void blit(Shader shader){
    passes[passes.length - 1].gBuffer.blit(shader);
  }

  @Override
  public int compareTo(Material o) {
    return Float.compare(priority, o.priority);
  }

  public static abstract class Pass {
    protected FrameBuffer gBuffer;
    protected Shader baseShader;

    protected abstract Shader buildShader();
    protected abstract FrameBuffer buildBuffer();
    protected abstract void applyShader(Shader shader, Seq<Texture> lastTextures);

    public void init(){
      baseShader = buildShader();
      gBuffer = buildBuffer();
    }

    public Seq<Texture> pass(Seq<Texture> lastTextures){
      gBuffer.begin(Color.clear);
      applyShader(baseShader, lastTextures);
      Draw.blit(baseShader);
      gBuffer.end();

      return gBuffer.getTextureAttachments();
    }
  }
}
