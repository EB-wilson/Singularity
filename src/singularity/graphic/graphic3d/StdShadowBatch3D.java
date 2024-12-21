package singularity.graphic.graphic3d;

import arc.files.Fi;
import arc.graphics.*;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.GLOnlyTextureData;
import arc.graphics.gl.Shader;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import arc.util.Log;
import singularity.graphic.SglShaders;

public class StdShadowBatch3D extends StandardBatch3D {
  private final Vec3 tmpVec = new Vec3();

  protected class ShadowedLightSource extends LightSource {
    private final Mat3D view = new Mat3D();
    private final Mat3D proj = new Mat3D();

    public final Mat3D[] lightSpaceProj = {new Mat3D(), new Mat3D(), new Mat3D(), new Mat3D(), new Mat3D(), new Mat3D()};
    public final FrameBuffer[] shadowBuffers = new FrameBuffer[6];
    public final Cubemap shadowCube;

    public ShadowedLightSource(int shadowSize) {
      GLOnlyTextureData data = new GLOnlyTextureData(shadowSize, shadowSize, 0,
          Pixmap.Format.rgba8888.glFormat, Pixmap.Format.rgba8888.glFormat, Pixmap.Format.rgba8888.glType);
      shadowCube = new Cubemap(data, data, data, data, data, data);

      for (int i = 0; i < 6; i++) {
        Cubemap.CubemapSide side = Cubemap.CubemapSide.values()[i];
        //可能不是很健壮的做法，但是，有效
        shadowBuffers[i] = new FrameBuffer(Pixmap.Format.rgba8888, shadowSize, shadowSize, true){
          @Override
          public void bind() {
            super.bind();
            Gl.framebufferTexture2D(Gl.framebuffer, GL20.GL_COLOR_ATTACHMENT0,
                side.glEnum, shadowCube.getTextureObjectHandle(), 0);
          }
        };
      }
    }

    @Override
    public void update() {
      for (int cubeFace = 0; cubeFace < 6; cubeFace++) {
        Cubemap.CubemapSide side = Cubemap.CubemapSide.values()[cubeFace];
        view.setToLookAt(position, tmpVec.set(position).add(side.direction), side.up);
        proj.setToProjection(1, radius, 90, 1).mul(view);

        lightSpaceProj[cubeFace].set(proj);
      }
    }

    @Override
    public void apply(Shader shader, int off) {
      super.apply(shader, off);

      //shadowBuffer.getTexture().bind(5 + off);
      shadowCube.bind(5 + off);
      shader.setUniformi("u_shadowCube" + off, 5 + off);
    }

    public void resetBuffer() {
      for (FrameBuffer frameBuffer : shadowBuffers) {
        frameBuffer.begin(Color.clear);
        frameBuffer.end();
      }
      //shadowBuffer.begin(Color.clear);
      //while (shadowBuffer.nextSide()){
      //  Gl.clear(Gl.depthBufferBit | Gl.colorBufferBit);
      //}
      //shadowBuffer.end();
    }
  }

  protected final int shadowSize;
  protected boolean shadowing = false;
  protected float shadowRadius = 250;
  public float shadowBias = 1.35f;

  protected Shader shadowShader;
  protected ShadowedLightSource currentLight;
  protected int currFace;

  public StdShadowBatch3D(int max, int shadowSize) {
    this(max, 4, shadowSize);
  }

  public StdShadowBatch3D(int max, int maxLights, int shadowSize) {
    this(max, maxLights, Gl.triangles, shadowSize);
  }

  public StdShadowBatch3D(int max, int maxLights, int primitiveType, int shadowSize) {
    super(max, maxLights, primitiveType);
    this.shadowSize = shadowSize;

    for (int i = 0; i < lights.length; i++) {
      lights[i] = new ShadowedLightSource(shadowSize);
    }
  }

  @Override
  protected void setSort(boolean sort) {
    throw new UnsupportedOperationException("Shadowing batch only works on sort mode");
  }

  @Override
  protected void setupShaders(int maxLights) {
    class MultiLightShader extends Shader{
      public MultiLightShader(Fi vertexShader, Fi fragmentShader) {
        super(vertexShader, fragmentShader);
      }

      @Override
      protected String preprocess(String source, boolean fragment) {
        source = "#define LIGHTS " + maxLights + "\n" + source;

        String preprocess = super.preprocess(source, fragment);
        Log.info(preprocess);
        return preprocess;
      }
    }

    shadowShader = new Shader(
        SglShaders.internalShaderDir.child("3d").child("standard_shadowmap.vert"),
        SglShaders.internalShaderDir.child("3d").child("standard_shadowmap.frag")
    );
    baseShader = new MultiLightShader(
        SglShaders.internalShaderDir.child("3d").child("standard_base_shadow.vert"),
        SglShaders.internalShaderDir.child("3d").child("standard_base_shadow.frag")
    ){
      @Override
      protected String preprocess(String source, boolean fragment) {
        StringBuilder cubeUniforms = new StringBuilder();

        //for (int i = 0; i < u_activeLights; i++) {
        //  if (i < u_activeLights){
        //    LightSource l = u_light[i];
        //    float shadow = 1.0 - shadow(l, u_shadowCube[i]);
        //    lightedColor += shadow * calculateLighting(l, color.rgb);
        //  }
        //}
        StringBuilder calcLightShadow = new StringBuilder();
        for (int i = 0; i < lights.length; i++) {
          cubeUniforms.append("uniform samplerCube u_shadowCube").append(i).append(";\n");
          calcLightShadow.append("if(").append(i).append(" < u_activeLights){");
          calcLightShadow.append("LightSource l").append(i).append(" = u_light[").append(i).append("];\n");
          calcLightShadow.append("float shadow").append(i).append(" = 1.0 - shadow(l").append(i).append(", u_shadowCube").append(i).append(");\n");
          calcLightShadow.append("lightedColor += shadow").append(i).append(" * calculateLighting(l").append(i).append(", color.rgb);");
          calcLightShadow.append("}");
        }

        return super.preprocess(
            source.replace("//$samplpers$", cubeUniforms)
                .replace("//$calculateShadowLight$", calcLightShadow),
            fragment
        );
      }
    };
    //normalShader = new MultiLightShader(
    //    SglShaders.internalShaderDir.child("3d").child("standard_norm_shadow.vert"),
    //    SglShaders.internalShaderDir.child("3d").child("standard_norm_shadow.frag")
    //);
    //standardShader = new MultiLightShader(
    //    SglShaders.internalShaderDir.child("3d").child("standard_shadow.vert"),
    //    SglShaders.internalShaderDir.child("3d").child("standard_shadow.frag")
    //);
  }

  @Override
  protected void applyShader(Shader shader) {
    super.applyShader(shader);
    shader.setUniformf("u_shadowBias", shadowBias);
    shader.setUniformf("u_shadowRadius", shadowRadius);
  }

  @Override
  public void begin(boolean cullFace) {
    super.begin(cullFace);

    for (LightSource light : lights) {
      ((ShadowedLightSource) light).resetBuffer();
    }
  }

  @Override
  public void flush() {
    if (shadowing) {
      Mesh mesh = this.mesh;
      //calling buffer() marks it as dirty, so it gets reuploaded upon render
      mesh.getVerticesBuffer();

      buffer.position(0);
      buffer.limit(vertexIdx);

      Shader shader = shadowShader;

      for (int i = 0; i < activeLights; i++) {
        ShadowedLightSource light = (ShadowedLightSource) lights[i];
        currentLight = light;

        FrameBuffer[] shadowBuffers = light.shadowBuffers;
        for (int face = 0, len = shadowBuffers.length; face < len; face++) {
          FrameBuffer shadowBuffer = shadowBuffers[face];
          currFace = face;

          shader.bind();
          shader.apply();
          shader.setUniformf("u_lightPos", light.position);
          shader.setUniformf("u_shadowRadius", shadowRadius);
          shader.setUniformMatrix4("u_transform", transform.val);
          shader.setUniformMatrix4("u_projLightSpace", light.lightSpaceProj[currFace].val);

          shadowBuffer.begin();
          mesh.render(shader, primitiveType);
          for (int l = 0; l < noSortReqCount; l++) {
            DrawRequests.DrawRequest req = noSortRequests[l];
            if (req.mesh != null) req.mesh.render(shader, primitiveType);
          }
          for (int l = 0; l < sortedReqCount; l++) {
            DrawRequests.SortedDrawRequest req = sortedRequests[l];
            if (req.mesh != null) req.mesh.render(shader, primitiveType);
          }
          shadowBuffer.end();
        }
      }

      buffer.limit(buffer.capacity());
      buffer.position(0);

      vertexIdx = 0;
    }
    else super.flush();
  }

  @Override
  protected void flushRequests() {
    if(!flushing && (noSortReqCount > 0 || sortedReqCount > 0)) {
      flushing = true;
      sortRequests();

      boolean lastAlpha = isAlpha;
      boolean lastPreTransEnabled = enablePreTransform;

      enablePreTransform = false;

      DrawRequests.DrawRequest[] r = noSortRequests; DrawRequests.SortedDrawRequest[] sr = sortedRequests;
      int num = noSortReqCount, snum = sortedReqCount;

      shadowing = true;
      putRequests(num, r, snum, sr);
      flush();

      Gl.clear(Gl.depthBufferBit);
      shadowing = false;
      putRequests(num, r, snum, sr);

      isAlpha = lastAlpha;
      enablePreTransform = lastPreTransEnabled;

      flushing = false;
      noSortReqCount = 0;
      sortedReqCount = 0;
      noSortReqVertIdx = 0;
      sortedReqVertIdx = 0;
    }
  }

  private void putRequests(int num, DrawRequests.DrawRequest[] r, int snum, DrawRequests.SortedDrawRequest[] sr) {
    boolean lastPreTrnEnabled = enablePreTransform;
    boolean lastAlpha = isAlpha;
    enablePreTransform = false;

    isAlpha = false;
    for (int i = 0; i < num; i++) {
      DrawRequests.DrawRequest req = r[i];

      if (req.mesh != null) {
        putMesh(
            req.texture, req.normalTexture, req.specTexture, false, req.mesh
        );
      }
      else {
        putVertices(
            req.texture, req.normalTexture, req.specTexture,
            noSortReqVertices, req.verticesOffset, req.verticesSize
        );
      }
    }

    isAlpha = true;
    for (int i = 0; i < snum; i++) {
      DrawRequests.SortedDrawRequest req = sr[i];

      if (req.mesh != null) {
        putMesh(
            req.texture, req.normalTexture, req.specTexture, true, req.mesh
        );
      }
      else {
        putVertices(
            req.texture, req.normalTexture, req.specTexture,
            noSortReqVertices, req.verticesOffset, req.verticesSize
        );
      }
    }

    enablePreTransform = lastPreTrnEnabled;
    isAlpha = lastAlpha;
  }
}
