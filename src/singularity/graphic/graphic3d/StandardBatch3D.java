package singularity.graphic.graphic3d;

import arc.Core;
import arc.files.Fi;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.g3d.Camera3D;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.Shader;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import arc.util.Log;
import singularity.graphic.SglShaders;

public class StandardBatch3D {
  protected static final Vec3 temp1 = new Vec3(), temp2 = new Vec3(), temp3 = new Vec3(), temp4 = new Vec3(), temp5 = new Vec3();

  protected static final int
      coordOff = 3,
      coordNormOff = 5,
      coordDiffOff = 7,
      coordSpecOff = 9,
      colorOff = 11,
      normOff = 12,
      tangOff = 15,
      sTangOff = 18;

  protected final int depthBufferHandle;
  protected final FrameBuffer buffer;

  protected final Mesh mesh;
  protected final int maxVertices;
  protected final int vertexSize;
  protected final int primitiveType;

  protected boolean isAlpha = false;
  protected boolean enablePreTransform = false;
  protected final Mat3D preTransform = new Mat3D().idt(), trn = new Mat3D();
  protected final Mat3D transform = new Mat3D().idt();

  protected final Camera3D camera = new Camera3D();
  protected final float[] vertices;

  protected final Vec3 directionalLightDir = new Vec3(0, -1, 0);
  protected final Color directionalLightColor = new Color();

  protected final LightSource[] lights;
  protected int activeLights = 0;

  protected final Color ambientColor = new Color();

  protected Texture lastTexture;
  protected Texture lastNormTexture;
  protected Texture lastDiffTexture;
  protected Texture lastSpecTexture;

  protected Shader baseShader;
  protected Shader normalShader;
  protected Shader standardShader;

  protected int vertexIdx;
  protected boolean isDrawing;

  public StandardBatch3D(int maxVertices){
    this(maxVertices, 4, Gl.triangles);
  }

  public StandardBatch3D(int maxVertices, int maxLights){
    this(maxVertices, maxLights, Gl.triangles);
  }

  public StandardBatch3D(int maxVertices, int maxLights, int primitiveType){
    this.lights = new LightSource[maxLights];
    this.maxVertices = maxVertices;
    this.primitiveType = primitiveType;

    setupLights();
    setupShaders(maxLights);

    buffer = new FrameBuffer(Pixmap.Format.rgba8888, Core.graphics.getWidth(), Core.graphics.getHeight(), true);
    depthBufferHandle = Gl.genBuffer();
    Gl.bindRenderbuffer(Gl.renderbuffer, depthBufferHandle);
    Gl.renderbufferStorage(Gl.renderbuffer, GL20.GL_DEPTH_COMPONENT, Core.graphics.getWidth(), Core.graphics.getHeight());
    Gl.bindRenderbuffer(Gl.renderbuffer, 0);

    VertexAttribute[] attribs = buildVertexAttributes();
    mesh = new Mesh(false, maxVertices, 0, attribs);

    vertices = new float[maxVertices * (mesh.vertexSize / 4)];
    vertexSize = mesh.vertexSize / 4;
  }

  protected void setupLights(){
    for (int i = 0; i < lights.length; i++) {
      lights[i] = new LightSource();
    }
  }

  protected VertexAttribute[] buildVertexAttributes(){
    Seq<VertexAttribute> attribs = Seq.with(
        VertexAttribute.position3,
        new VertexAttribute(2, Shader.texcoordAttribute + "0"),
        new VertexAttribute(2, Shader.texcoordAttribute + "1"),
        new VertexAttribute(2, Shader.texcoordAttribute + "2"),
        new VertexAttribute(2, Shader.texcoordAttribute + "3"),
        VertexAttribute.color,
        VertexAttribute.normal,
        new VertexAttribute(3, "a_subTangent"),
        new VertexAttribute(3, "a_tangent")
    );

    return attribs.toArray(VertexAttribute.class);
  }

  protected void setupShaders(int maxLights){
    class MultiLightShader extends Shader{
      public MultiLightShader(Fi vertexShader, Fi fragmentShader) {
        super(vertexShader, fragmentShader);
      }

      @Override
      protected String preprocess(String source, boolean fragment) {
        source = "#define LIGHTS " + maxLights + "\n" + source;

        return super.preprocess(source, fragment);
      }
    }

    baseShader = new MultiLightShader(
        SglShaders.internalShaderDir.child("3d").child("standard_base.vert"),
        SglShaders.internalShaderDir.child("3d").child("standard_base.frag")
    );
    normalShader = new MultiLightShader(
        SglShaders.internalShaderDir.child("3d").child("standard_norm.vert"),
        SglShaders.internalShaderDir.child("3d").child("standard_norm.frag")
    );
    standardShader = new MultiLightShader(
        SglShaders.internalShaderDir.child("3d").child("standard.vert"),
        SglShaders.internalShaderDir.child("3d").child("standard.frag")
    );
  }

  protected void applyShader(Shader shader, boolean hasNormalTex, boolean hasStandards){
    shader.bind();
    shader.apply();

    lastTexture.bind(0);
    shader.setUniformi("u_texture", 0);

    if (hasNormalTex) {
      lastNormTexture.bind(1);
      shader.setUniformi("u_normalTex", 1);
    }
    if (hasNormalTex && hasStandards) {
      lastDiffTexture.bind(2);
      shader.setUniformi("u_diffuseTex", 2);
      lastSpecTexture.bind(3);
      shader.setUniformi("u_specularTex", 3);
    }

    shader.setUniformMatrix4("u_proj", camera.projection.val);
    shader.setUniformMatrix4("u_view", camera.view.val);
    shader.setUniformMatrix4("u_transform", transform.val);

    shader.setUniformf("u_cameraPos", camera.position);
    shader.setUniformf("u_lightDir", directionalLightDir);
    shader.setUniformf("u_lightColor", directionalLightColor);
    shader.setUniformf("u_ambientColor", ambientColor);

    shader.setUniformi("u_activeLights", activeLights);
    for (int i = 0; i < lights.length; i++) {
      lights[i].apply(shader, i);
    }
  }

  public void preTransform(Mat3D transform){
    preTransform.set(transform);
  }
  public Mat3D getPreTransform(){
    return preTransform;
  }
  public void resetPreTransform(){
    preTransform.idt();
    enablePreTransform = false;
  }
  public void enablePreTransform(){
    enablePreTransform = true;
  }

  public Camera3D getCamera() {
    return camera;
  }

  public void setCamera(Camera3D camera) {
    flush();
    this.camera.width = camera.width;
    this.camera.height = camera.height;
    this.camera.perspective = camera.perspective;
    this.camera.position.set(camera.position);
    this.camera.direction.set(camera.direction);
    this.camera.up.set(camera.up);
    this.camera.update();
  }

  public void setShaders(Shader base, Shader normText, Shader standard){
    flush();
    this.baseShader = base;
    this.normalShader = normText;
    this.standardShader = standard;
  }

  public void setBaseShader(Shader baseShader) {
    flush();
    this.baseShader = baseShader;
  }
  public void setNormalShader(Shader normalShader) {
    flush();
    this.normalShader = normalShader;
  }
  public void setStandardShader(Shader standardShader) {
    flush();
    this.standardShader = standardShader;
  }
  public Shader getBaseShader(){ return baseShader; }
  public Shader getNormalShader(){ return normalShader; }
  public Shader getStandardShader(){ return standardShader; }

  public void setTransform(Mat3D trans){
    flush();
    transform.set(trans);
  }

  public Mat3D getTransform(){
    return transform;
  }

  public Vec3 getDirLight(){
    return directionalLightDir;
  }

  public void setDirLight(Vec3 pos){
    flush();
    directionalLightDir.set(pos);
  }

  public void setDirLight(float x, float y, float z){
    flush();
    directionalLightDir.set(x, y, z);
  }

  public void setDirLightColor(Color color){
    flush();
    directionalLightColor.set(color);
  }

  public void setDirLightColor(Color color, float intensity){
    flush();
    directionalLightColor.set(color);
    directionalLightColor.a(intensity);
  }

  public Color getDirLightColor(){
    return directionalLightColor;
  }

  public LightSource getLight(int index){
    return lights[index];
  }

  public void resetLights(){
    flush();
    activeLights = 0;
  }

  public void activeLights(int num){
    flush();
    activeLights = num;
  }

  public void activeLights(LightSource... lights){
    flush();
    for (int i = 0; i < lights.length; i++) {
      this.lights[i].set(lights[i]);
    }
    activeLights = lights.length;
  }

  public LightSource nextLight(){
    flush();
    return lights[activeLights++];
  }

  public LightSource nextLight(Vec3 lightPos, Color color){
    LightSource source = nextLight();
    source.position.set(lightPos);
    source.color.set(color);
    source.update();
    return source;
  }

  public LightSource nextLight(float x, float y, float z, Color color) {
    LightSource source = nextLight();
    source.position.set(x, y, z);
    source.color.set(color);
    source.update();
    return source;
  }

  public LightSource nextLight(LightSource light){
    LightSource source = nextLight();
    source.set(light);
    source.update();
    return source;
  }

  public void updateLights() {
    for (LightSource light : lights) {
      light.update();
    }
  }

  public void setAmbientColor(Color color, float strength){
    flush();
    ambientColor.set(color);
    ambientColor.a(strength);
  }

  public Color getAmbientColor() {
    return ambientColor;
  }

  public void setAlpha(boolean isAlpha){
    this.isAlpha = isAlpha;
  }

  public void switchTexture(Texture texture){
    if (lastTexture != texture) {
      flush();
      lastTexture = texture;
    }
  }

  public void switchTexture(Texture texture, Texture normTexture){
    if (lastTexture != texture || lastNormTexture != normTexture) {
      flush();
      lastTexture = texture;
      lastNormTexture = normTexture;
    }
  }

  public void switchTexture(Texture texture, Texture normTexture, Texture diffTexture, Texture specTexture){
    if (lastTexture != texture || lastNormTexture != normTexture || lastDiffTexture != diffTexture || lastSpecTexture != specTexture) {
      flush();
      lastTexture = texture;
      lastNormTexture = normTexture;
      lastDiffTexture = diffTexture;
      lastSpecTexture = specTexture;
    }
  }

  public void vertices(Texture texture, float[] vertices, int offset, int counts){
    vertices(texture, null, null, null, vertices, offset, counts);
  }

  public void vertices(Texture texture, Texture normTexture, float[] vertices, int offset, int counts){
    vertices(texture, normTexture, null, null, vertices, offset, counts);
  }

  public void vertices(
      Texture texture, Texture normTexture, Texture diffTexture, Texture specTexture,
      float[] vertices, int offset, int counts
  ){
    switchTexture(texture, normTexture, diffTexture, specTexture);

    if (vertexIdx + counts > this.vertices.length) flush();
    float[] vert = this.vertices;
    int idx = vertexIdx;

    if (enablePreTransform) {
      Mat3D trn = this.trn.set(preTransform).toNormalMatrix();
      for (int i = 0; i < counts; i += vertexSize) {
        int offVert = idx + i;
        int offSource = offset + i;
        preTrn(vert, vertices, offVert, offSource, preTransform, trn);
      }
    }
    else {
      System.arraycopy(vertices, offset, vert, vertexIdx, counts);
    }

    vertexIdx += counts;
  }

  protected static void preTrn(float[] vertices, float[] source, int idx, int off, Mat3D preTan, Mat3D trn) {
    //position
    temp1.set(source[off], source[off + 1], source[off + 2]);
    Mat3D.prj(temp1, preTan);
    vertices[idx] = temp1.x; vertices[idx + 1] = temp1.y; vertices[idx + 2] = temp1.z;

    //coords
    //base
    vertices[idx + 3] = source[off + 3];
    vertices[idx + 4] = source[off + 4];
    //norm
    vertices[idx + 5] = source[off + 5];
    vertices[idx + 6] = source[off + 6];
    //diff
    vertices[idx + 7] = source[off + 7];
    vertices[idx + 8] = source[off + 8];
    //spec
    vertices[idx + 9] = source[off + 9];
    vertices[idx + 10] = source[off + 10];

    //color
    vertices[idx + 11] = source[off + 11];

    //normal
    temp1.set(source[off + 12], source[off + 13], source[off + 14]);
    Mat3D.rot(temp1, trn);
    vertices[idx + 12] = temp1.x; vertices[idx + 13] = temp1.y; vertices[idx + 14] = temp1.z;
    //tangent
    temp1.set(source[off + 15], source[off + 16], source[off + 17]);
    Mat3D.rot(temp1, trn);
    vertices[idx + 15] = temp1.x; vertices[idx + 16] = temp1.y; vertices[idx + 17] = temp1.z;
    //subTangent
    temp1.set(source[off + 18], source[off + 19], source[off + 20]);
    Mat3D.rot(temp1, trn);
    vertices[idx + 18] = temp1.x; vertices[idx + 19] = temp1.y; vertices[idx + 20] = temp1.z;
  }

  public void tri(
      Texture texture,
      float x1, float y1, float z1, float u1, float v1,
      float x2, float y2, float z2, float u2, float v2,
      float x3, float y3, float z3, float u3, float v3,
      Color color
  ){
    tri(
        texture, null, null, null,
        x1, y1, z1, u1, v1, 0, 0, 0, 0, 0, 0,
        x2, y2, z2, u2, v2, 0, 0, 0, 0, 0, 0,
        x3, y3, z3, u3, v3, 0, 0, 0, 0, 0, 0,
        color
    );
  }

  public void tri(
      Texture texture, Texture textureNorm,
      float x1, float y1, float z1, float u1, float v1, float un1, float vn1,
      float x2, float y2, float z2, float u2, float v2, float un2, float vn2,
      float x3, float y3, float z3, float u3, float v3, float un3, float vn3,
      Color color
  ){
    tri(
        texture, textureNorm, null, null,
        x1, y1, z1, u1, v1, un1, vn1, 0, 0, 0, 0,
        x2, y2, z2, u2, v2, un2, vn2, 0, 0, 0, 0,
        x3, y3, z3, u3, v3, un3, vn3, 0, 0, 0, 0,
        color
    );
  }

  @SuppressWarnings("DuplicatedCode")
  public void tri(
      Texture texture, Texture textureNorm, Texture textureDiff, Texture textureSpec,
      float x1, float y1, float z1, float u1, float v1, float un1, float vn1, float ud1, float vd1, float us1, float vs1,
      float x2, float y2, float z2, float u2, float v2, float un2, float vn2, float ud2, float vd2, float us2, float vs2,
      float x3, float y3, float z3, float u3, float v3, float un3, float vn3, float ud3, float vd3, float us3, float vs3,
      Color color
  ){
    if (enablePreTransform) {
      temp1.set(x1, y1, z1);
      Mat3D.prj(temp1, preTransform);
      x1 = temp1.x; y1 = temp1.y; z1 = temp1.z;
      temp1.set(x2, y2, z2);
      Mat3D.prj(temp1, preTransform);
      x2 = temp1.x; y2 = temp1.y; z2 = temp1.z;
      temp1.set(x3, y3, z3);
      Mat3D.prj(temp1, preTransform);
      x3 = temp1.x; y3 = temp1.y; z3 = temp1.z;
    }

    float t1 = u2 - u1;
    float b1 = v2 - v1;
    float t2 = u3 - u1;
    float b2 = v3 - v1;

    float ex1 = x2 - x1, ey1 = y2 - y1, ez1 = z2 - z1;
    float ex2 = x3 - x1, ey2 = y3 - y1, ez2 = z3 - z1;

    float k = t1 * b2 - b1 * t2;

    float normalX = ey1 * ez2 - ez1 * ey2;
    float normalY = ez1 * ex2 - ex1 * ez2;
    float normalZ = ex1 * ey2 - ey1 * ex2;

    float tangX = k * (b2 * ex1 - b1 * ex2);
    float tangY = k * (b2 * ey1 - b1 * ey2);
    float tangZ = k * (b2 * ez1 - b1 * ez2);

    float subTangX = k * (t2 * ex1 - t1 * ex2);
    float subTangY = k * (t2 * ey1 - t1 * ey2);
    float subTangZ = k * (t2 * ez1 - t1 * ez2);

    float ln = 1.0f / (float)Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
    normalX *= ln; normalY *= ln; normalZ *= ln;
    float lt = 1.0f / (float)Math.sqrt(tangX * tangX + tangY * tangY + tangZ * tangZ);
    tangX *= lt; tangY *= lt; tangZ *= lt;
    float ls = 1.0f / (float)Math.sqrt(subTangX * subTangX + subTangY * subTangY + subTangZ * subTangZ);
    subTangX *= ls; subTangY *= ls; subTangZ *= ls;

    if (vertexIdx + vertexSize*3 >= vertices.length) flush();
    switchTexture(texture, textureNorm, textureDiff, textureSpec);

    float[] vertices = this.vertices;
    int ind = vertexIdx;
    vertexIdx += vertexSize*3;

    float colorBit = color.toFloatBits();

    vertices[ind] = x1; vertices[ind+1] = y1; vertices[ind+2] = z1;
    vertices[ind + coordOff] = u1; vertices[ind + coordOff + 1] = v1;
    vertices[ind + coordNormOff] = un1; vertices[ind + coordNormOff + 1] = vn1;
    vertices[ind + coordDiffOff] = ud1; vertices[ind + coordDiffOff + 1] = vd1;
    vertices[ind + coordSpecOff] = us1; vertices[ind + coordSpecOff + 1] = vs1;
    vertices[ind + colorOff] = colorBit;
    vertices[ind + normOff] = normalX; vertices[ind + normOff + 1] = normalY; vertices[ind + normOff + 2] = normalZ;
    vertices[ind + tangOff] = tangX; vertices[ind + tangOff + 1] = tangY; vertices[ind + tangOff + 2] = tangZ;
    vertices[ind + sTangOff] = subTangX; vertices[ind + sTangOff + 1] = subTangY; vertices[ind + sTangOff + 2] = subTangZ;
    ind += vertexSize;

    vertices[ind] = x2; vertices[ind+1] = y2; vertices[ind+2] = z2;
    vertices[ind + coordOff] = u2; vertices[ind + coordOff + 1] = v2;
    vertices[ind + coordNormOff] = un2; vertices[ind + coordNormOff + 1] = vn2;
    vertices[ind + coordDiffOff] = ud2; vertices[ind + coordDiffOff + 1] = vd2;
    vertices[ind + coordSpecOff] = us2; vertices[ind + coordSpecOff + 1] = vs2;
    vertices[ind + colorOff] = colorBit;
    vertices[ind + normOff] = normalX; vertices[ind + normOff + 1] = normalY; vertices[ind + normOff + 2] = normalZ;
    vertices[ind + tangOff] = tangX; vertices[ind + tangOff + 1] = tangY; vertices[ind + tangOff + 2] = tangZ;
    vertices[ind + sTangOff] = subTangX; vertices[ind + sTangOff + 1] = subTangY; vertices[ind + sTangOff + 2] = subTangZ;
    ind += vertexSize;

    vertices[ind] = x3; vertices[ind+1] = y3; vertices[ind+2] = z3;
    vertices[ind + coordOff] = u3; vertices[ind + coordOff + 1] = v3;
    vertices[ind + coordNormOff] = un3; vertices[ind + coordNormOff + 1] = vn3;
    vertices[ind + coordDiffOff] = ud3; vertices[ind + coordDiffOff + 1] = vd3;
    vertices[ind + coordSpecOff] = us3; vertices[ind + coordSpecOff + 1] = vs3;
    vertices[ind + colorOff] = colorBit;
    vertices[ind + normOff] = normalX; vertices[ind + normOff + 1] = normalY; vertices[ind + normOff + 2] = normalZ;
    vertices[ind + tangOff] = tangX; vertices[ind + tangOff + 1] = tangY; vertices[ind + tangOff + 2] = tangZ;
    vertices[ind + sTangOff] = subTangX; vertices[ind + sTangOff + 1] = subTangY; vertices[ind + sTangOff + 2] = subTangZ;
  }

  public void rect(
      TextureRegion region,
      float x1, float y1, float z1,
      float x2, float y2, float z2,
      float x3, float y3, float z3,
      float x4, float y4, float z4,
      Color color
  ){
    tri(
        region.texture, null, null, null,
        x1, y1, z1, region.u, region.v2, 0, 0, 0, 0, 0, 0,
        x2, y2, z2, region.u2, region.v2, 0, 0, 0, 0, 0, 0,
        x3, y3, z3, region.u2, region.v, 0, 0, 0, 0, 0, 0,
        color
    );
    tri(
        region.texture, null, null, null,
        x3, y3, z3, region.u2, region.v, 0, 0, 0, 0, 0, 0,
        x4, y4, z4, region.u, region.v, 0, 0, 0, 0, 0, 0,
        x1, y1, z1, region.u, region.v2, 0, 0, 0, 0, 0, 0,
        color
    );
  }

  public void rect(
      TextureRegion region, TextureRegion normRegion,
      float x1, float y1, float z1,
      float x2, float y2, float z2,
      float x3, float y3, float z3,
      float x4, float y4, float z4,
      Color color
  ){
    tri(
        region.texture, normRegion.texture, null, null,
        x1, y1, z1, region.u, region.v2, normRegion.u, normRegion.v2, 0, 0, 0, 0,
        x2, y2, z2, region.u2, region.v2, normRegion.u2, normRegion.v2, 0, 0, 0, 0,
        x3, y3, z3, region.u2, region.v, normRegion.u2, normRegion.v, 0, 0, 0, 0,
        color
    );
    tri(
        region.texture, normRegion.texture, null, null,
        x3, y3, z3, region.u2, region.v, normRegion.u2, normRegion.v, 0, 0, 0, 0,
        x4, y4, z4, region.u, region.v, normRegion.u, normRegion.v, 0, 0, 0, 0,
        x1, y1, z1, region.u, region.v2, normRegion.u, normRegion.v2, 0, 0, 0, 0,
        color
    );
  }

  public void rect(
      TextureRegion region, TextureRegion normRegion, TextureRegion diffRegion, TextureRegion specRegion,
      float x1, float y1, float z1,
      float x2, float y2, float z2,
      float x3, float y3, float z3,
      float x4, float y4, float z4,
      Color color
  ){
    tri(
        region.texture, normRegion.texture, diffRegion.texture, specRegion.texture,
        x1, y1, z1, region.u, region.v2, normRegion.u, normRegion.v2, diffRegion.u, diffRegion.v2, specRegion.u, specRegion.v2,
        x2, y2, z2, region.u2, region.v2, normRegion.u2, normRegion.v2, diffRegion.u2, diffRegion.v2, specRegion.u2, specRegion.v2,
        x3, y3, z3, region.u2, region.v, normRegion.u2, normRegion.v, diffRegion.u2, diffRegion.v, specRegion.u2, specRegion.v,
        color
    );
    tri(
        region.texture, normRegion.texture, diffRegion.texture, specRegion.texture,
        x3, y3, z3, region.u2, region.v, normRegion.u2, normRegion.v, diffRegion.u2, diffRegion.v, specRegion.u2, specRegion.v,
        x4, y4, z4, region.u, region.v, normRegion.u, normRegion.v, diffRegion.u, diffRegion.v, specRegion.u, specRegion.v,
        x1, y1, z1, region.u, region.v2, normRegion.u, normRegion.v2, diffRegion.u, diffRegion.v2, specRegion.u, specRegion.v2,
        color
    );
  }

  public void begin(){
    begin(true);
  }

  public void begin(boolean cullFace){
    if (isDrawing) throw new RuntimeException("Batch already started");

    updateLights();

    Blending.normal.apply();

    //Gl.framebufferRenderbuffer(Gl.framebuffer, Gl.depthAttachment, Gl.renderbuffer, depthBufferHandle);
    buffer.begin(Color.clear);

    Gl.enable(Gl.depthTest);
    Gl.depthMask(true);
    Gl.clear(Gl.depthBufferBit);

    Gl.depthFunc(Gl.less);

    if (cullFace) {
      Gl.enable(Gl.cullFace);
      Gl.cullFace(Gl.back);
    }

    isDrawing = true;
  }

  public void end(){
    if (!isDrawing) throw new RuntimeException("Batch not started");

    flush();

    buffer.end();
    buffer.blit(SglShaders.simpleScreen);
    //Gl.framebufferRenderbuffer(Gl.framebuffer, Gl.depthAttachment, Gl.renderbuffer, 0);

    Gl.disable(Gl.cullFace);
    Gl.disable(Gl.depthTest);

    isDrawing = false;
  }

  public boolean flushMesh(){
    if(vertexIdx < vertexSize*3) {
      vertexIdx = 0;
      return false;
    }

    Mesh mesh = this.mesh;
    mesh.setVertices(vertices, 0, vertexIdx);
    vertexIdx = 0;

    return true;
  }

  public void flush(){
    if (!isDrawing) throw new RuntimeException("Batch not started");

    if (!flushMesh()) return;

    Shader shader = getCurrShader();
    applyShader(
        shader, lastNormTexture != null,
        lastDiffTexture != null && lastSpecTexture != null
    );

    if (isAlpha) Gl.depthMask(false);

    mesh.render(shader, primitiveType);

    if (isAlpha) Gl.depthMask(true);

    lastTexture = null;
    lastNormTexture = null;
    lastDiffTexture = null;
    lastSpecTexture = null;

    Gl.activeTexture(Gl.texture0);
  }

  protected Shader getCurrShader() {
    if (lastNormTexture != null && lastDiffTexture != null && lastSpecTexture != null) return standardShader;
    else if (lastNormTexture != null) return normalShader;
    else return baseShader;
  }

  public int getMaxVertices(){
    return maxVertices;
  }

  public void dispose(){
    baseShader.dispose();
    normalShader.dispose();
    standardShader.dispose();
    mesh.dispose();
  }
}
