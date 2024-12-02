package singularity.graphic.graphic3d;

import arc.graphics.Color;
import arc.graphics.Texture;
import arc.math.geom.Vec3;

public class DrawRequest {
  public boolean isTriangle, isAlpha;
  public float
      x1, y1, z1,
      x2, y2, z2,
      x3, y3, z3;
  public float
      u1, v1,
      u2, v2,
      u3, v3;
  public float
      un1, vn1,
      un2, vn2,
      un3, vn3;
  public float
      ud1, vd1,
      ud2, vd2,
      ud3, vd3;
  public float
      us1, vs1,
      us2, vs2,
      us3, vs3;
  public Color color;

  public Texture texture, normalTexture, diffTexture, specTexture;
  public float[] vertices;
  public int vertexSize;

  public float dst;

  public void calcDst(float x, float y, float z) {
    float max = 0;

    float tx, ty, tz;
    if (isTriangle) {
      tx = x1 - x; ty = y1 - y; tz = z1 - z;
      max = Math.max(max, tx * tx + ty * ty + tz * tz);
      tx = x2 - x; ty = y2 - y; tz = z2 - z;
      max = Math.max(max, tx * tx + ty * ty + tz * tz);
      tx = x3 - x; ty = y3 - y; tz = z3 - z;
      max = Math.max(max, tx * tx + ty * ty + tz * tz);
    }
    else {
      for (int i = 0; i < vertices.length; i+=vertexSize) {
        tx = vertices[i] - x; ty = vertices[i + 1] - y; tz = vertices[i + 2] - z;
        max = Math.max(max, tx * tx + ty * ty + tz * tz);
      }
    }

    this.dst = max;
  }
}
