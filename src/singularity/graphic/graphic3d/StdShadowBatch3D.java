package singularity.graphic.graphic3d;

import arc.files.Fi;
import arc.graphics.*;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.FrameBufferCubemap;
import arc.graphics.gl.GLOnlyTextureData;
import arc.graphics.gl.Shader;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import arc.util.Log;
import arc.util.ScreenUtils;
import mindustry.gen.Tex;
import singularity.Sgl;
import singularity.graphic.SglShaders;

public class StdShadowBatch3D extends SortedBatch3D {
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
  protected float shadowRadius = 100;
  public float shadowBias = 2f;

  protected Shader shadowShader;
  protected ShadowedLightSource currentLight;
  protected int currFace;

  public StdShadowBatch3D(int maxVertices, int shadowSize) {
    this(maxVertices, Gl.triangles, shadowSize);
  }

  public StdShadowBatch3D(int maxVertices, int primitiveType, int shadowSize) {
    this(maxVertices, 4, primitiveType, shadowSize);
  }

  public StdShadowBatch3D(int maxVertices, int maxLights, int primitiveType, int shadowSize) {
    super(maxVertices, maxLights, primitiveType);
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
  protected void applyShader(Shader shader, boolean hasNormalTex, boolean hasStandards) {
    super.applyShader(shader, hasNormalTex, hasStandards);
    shader.setUniformf("u_shadowBias", shadowBias);
    shader.setUniformf("u_shadowRadius", shadowRadius);
  }

  @Override
  public void flush() {
    if (shadowing) {
      if (!flushMesh()) return;

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
          shadowBuffer.end();
        }
      }
    }
    else super.flush();
  }

  @Override
  public void begin(boolean cullFace) {
    super.begin(cullFace);

    for (LightSource light : lights) {
      ((ShadowedLightSource) light).resetBuffer();
    }
  }

  @Override
  protected void flushRequests() {
    if(!flushing && requestCount > 0) {
      flushing = true;
      sortRequests();

      boolean lastAlpha = isAlpha;
      boolean lastPreTransEnabled = enablePreTransform;

      enablePreTransform = false;

      DrawRequest[] r = requests;
      int num = requestCount;

      shadowing = true;
      putRequests(num, r);
      flush();

      Gl.clear(Gl.depthBufferBit);
      shadowing = false;
      putRequests(num, r);

      isAlpha = lastAlpha;
      enablePreTransform = lastPreTransEnabled;

      requestCount = 0;
      flushing = false;
    }
  }

  private void putRequests(int num, DrawRequest[] r) {
    for(int j = 0; j < num; j++){
      DrawRequest req = r[j];

      super.setAlpha(req.isAlpha);

      if(req.isTriangle){
        super.tri(
            req.texture, req.normalTexture, req.diffTexture, req.specTexture,
            req.x1, req.y1, req.z1, req.u1, req.v1, req.un1, req.vn1, req.ud1, req.vd1, req.us1, req.vs1,
            req.x2, req.y2, req.z2, req.u2, req.v2, req.un2, req.vn2, req.ud2, req.vd2, req.us2, req.vs2,
            req.x3, req.y3, req.z3, req.u3, req.v3, req.un3, req.vn3, req.ud3, req.vd3, req.us3, req.vs3,
            req.color
        );
      }else{
        super.vertices(
            req.texture, req.normalTexture,
            req.vertices, 0, vertexSize*3
        );
      }
    }
  }
}
