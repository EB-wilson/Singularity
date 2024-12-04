package singularity.graphic.graphic3d;

import arc.graphics.Texture;

public class DrawRequests {
  public static class DrawRequest {
    public Texture texture, normalTexture, diffTexture, specTexture;
    public int verticesOffset, verticesSize;
  }

  public static class SortedDrawRequest extends DrawRequest {
    public boolean isAlpha;
    public float dst2;
    public Runnable runTask;
  }

}
