package singularity.graphic.graphic3d;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.math.geom.Mat3D;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class SortedBatch3D extends StandardBatch3D{
  private static final int processorThreads = Runtime.getRuntime().availableProcessors()*2;

  protected DrawRequest[] requests = new DrawRequest[4000];
  protected float[] dst2 = new float[4000];
  protected int requestCount = 0;

  {
    for (int i = 0; i < requests.length; i++) {
      requests[i] = new DrawRequest();
      requests[i].vertexSize = vertexSize;
      requests[i].vertices = new float[vertexSize*3];
    }
  }

  protected boolean multiThread = (Core.app.getVersion() >= 21 && !Core.app.isIOS()) || Core.app.isDesktop();
  protected boolean sort = true;
  protected boolean flushing = false;

  public SortedBatch3D(int maxVertices) {
    super(maxVertices);
  }

  public SortedBatch3D(int maxVertices, int primitiveType) {
    super(maxVertices, primitiveType);
  }

  public SortedBatch3D(int maxVertices, int maxLights, int primitiveType) {
    super(maxVertices, maxLights, primitiveType);
  }

  protected void setSort(boolean sort){
    if(this.sort != sort){
      flush();
    }
    this.sort = sort;
  }

  protected void expandRequests(){
    final DrawRequest[] requests = this.requests, newRequests = new DrawRequest[requests.length*7/4];
    System.arraycopy(requests, 0, newRequests, 0, Math.min(newRequests.length, requests.length));
    for(int i = requests.length; i < newRequests.length; i++){
      newRequests[i] = new DrawRequest();
    }
    this.requests = newRequests;
    this.dst2 = Arrays.copyOf(dst2, this.requests.length);
  }

  @Override
  public void vertices(
      Texture texture, Texture normTexture, Texture textureDiff, Texture textureSpec,
      float[] vertices, int offset, int counts) {
    if (sort && !flushing) {
      for (int n = 0; n < counts; n += vertexSize*3) {
        if(requestCount >= requests.length) expandRequests();
        DrawRequest req = requests[requestCount];
        float[] vert = req.vertices;

        int triVerts = vertexSize*3;
        if (enablePreTransform) {
          Mat3D trn = this.trn.set(preTransform).toNormalMatrix();
          for (int i = 0; i < triVerts; i += vertexSize) {
            int offSource = offset + n + i;
            preTrn(vert, vertices, i, offSource, preTransform, trn);
          }
        }
        else {
          System.arraycopy(vertices, offset, vert, 0, triVerts);
        }

        req.isTriangle = false;
        req.texture = texture;
        req.normalTexture = normTexture;
        req.calcDst(camera.position.x, camera.position.y, camera.position.z);
        dst2[requestCount] = req.dst;

        requestCount++;
      }
    }
    else super.vertices(texture, normTexture, textureDiff, textureSpec, vertices, offset, counts);
  }

  @Override
  public void tri(
      Texture texture, Texture textureNorm, Texture textureDiff, Texture textureSpec,
      float x1, float y1, float z1, float u1, float v1, float un1, float vn1, float ud1, float vd1, float us1, float vs1,
      float x2, float y2, float z2, float u2, float v2, float un2, float vn2, float ud2, float vd2, float us2, float vs2,
      float x3, float y3, float z3, float u3, float v3, float un3, float vn3, float ud3, float vd3, float us3, float vs3,
      Color color
  ) {
    if (sort && !flushing) {
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

      if(requestCount >= requests.length) expandRequests();
      DrawRequest req = requests[requestCount];
      req.color = color;
      req.x1 = x1; req.y1 = y1; req.z1 = z1; req.u1 = u1; req.v1 = v1;
      req.x2 = x2; req.y2 = y2; req.z2 = z2; req.u2 = u2; req.v2 = v2;
      req.x3 = x3; req.y3 = y3; req.z3 = z3; req.u3 = u3; req.v3 = v3;
      req.un1 = un1; req.vn1 = vn1; req.ud1 = ud1; req.vd1 = vd1; req.us1 = us1; req.vs1 = vs1;
      req.un2 = un2; req.vn2 = vn2; req.ud2 = ud2; req.vd2 = vd2; req.us2 = us2; req.vs2 = vs2;
      req.un3 = un3; req.vn3 = vn3; req.ud3 = ud3; req.vd3 = vd3; req.us3 = us3; req.vs3 = vs3;
      req.isAlpha = isAlpha;
      req.isTriangle = true;
      req.texture = texture;
      req.normalTexture = textureNorm;
      req.diffTexture = textureDiff;
      req.specTexture = textureSpec;
      req.calcDst(camera.position.x, camera.position.y, camera.position.z);

      dst2[requestCount] = req.dst;
      requestCount++;
    }
    else super.tri(
        texture, textureNorm, textureDiff, textureSpec,
        x1, y1, z1, u1, v1, un1, vn1, ud1, vd1, us1, vs1,
        x2, y2, z2, u2, v2, un2, vn2, ud2, vd2, us2, vs2,
        x3, y3, z3, u3, v3, un3, vn3, ud3, vd3, us3, vs3,
        color
    );
  }

  protected final void superFlush(){
    super.flush();
  }

  @Override
  public void flush() {
    flushRequests();
    superFlush();
  }

  protected void flushRequests(){
    if(!flushing && requestCount > 0) {
      flushing = true;
      sortRequests();

      boolean lastAlpha = isAlpha;
      boolean lastPreTransEnabled = enablePreTransform;

      enablePreTransform = false;

      DrawRequest[] r = requests;
      int num = requestCount;
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

      isAlpha = lastAlpha;
      enablePreTransform = lastPreTransEnabled;

      requestCount = 0;
      flushing = false;
    }
  }

  protected void sortRequests() {
    MergeSorter.init(requests.length);
    //if (multiThread) MergeSorter.sortMultiThreads(processorThreads, requests, dst2, 0, requestCount - 1);
    MergeSorter.sort(requests, dst2, 0, requestCount-1);
  }

  static class SortTask extends ForkJoinTask<Void> implements Pool.Poolable {
    int threads;
    DrawRequest[] requests;
    float[] dst2;
    int left;
    int right;

    @Override
    public void reset() {
      requests = null;
      dst2 = null;
      left = right = threads = 0;
    }

    @Override public Void getRawResult() {return null;}
    @Override protected void setRawResult(Void value) {}
    @Override
    protected boolean exec() {
      MergeSorter.sortMultiThreads(threads, requests, dst2, left, right);
      return true;
    }
  }

  static class MergeSorter {
    private static final ForkJoinPool commonPool = new ForkJoinPool();

    static float[] tmpDst2l;
    static float[] tmpDst2r;
    static DrawRequest[] tmpRequestsL;
    static DrawRequest[] tmpRequestsR;

    public static void init(int size){
      if(tmpDst2l == null || tmpDst2l.length < size) {
        tmpDst2l = new float[size];
        tmpDst2r = new float[size];
        tmpRequestsL = new DrawRequest[size];
        tmpRequestsR = new DrawRequest[size];
      }
    }

    public static void sortMultiThreads(int threads, DrawRequest[] requests, float[] dst2, int left, int right) {
      if (threads <= 1) sort(requests, dst2, left, right);
      else if (left < right) {
        int mid = (left + right) / 2;

        SortTask l = Pools.obtain(SortTask.class, SortTask::new);
        SortTask r = Pools.obtain(SortTask.class, SortTask::new);

        l.threads = r.threads = threads/2;
        l.dst2 = r.dst2 = dst2;
        l.requests = r.requests = requests;
        l.left = left; l.right = mid;
        r.left = mid + 1; r.right = right;

        ForkJoinTask<?> t1 = commonPool.submit(l);
        ForkJoinTask<?> t2 = commonPool.submit(r);

        t1.join();
        t2.join();

        Pools.free(l);
        Pools.free(r);

        merge(requests, dst2, left, mid, right);
      }
    }

    public static void sort(DrawRequest[] requests, float[] dst2, int left, int right) {
      if (left < right) {
        if (right - left <= 16) {
          insert(requests, dst2, left, right);
        }
        else {
          int mid = (left + right)/2;
          sort(requests, dst2, left, mid);
          sort(requests, dst2, mid + 1, right);
          merge(requests, dst2, left, mid, right);
        }
      }
    }

    private static void insert(DrawRequest[] requests, float[] dst2, int left, int right){
      for (int i = left + 1; i <= right; i++) {
        float key = dst2[i];
        DrawRequest req = requests[i];
        int j = i - 1;
        while (j >= left && dst2[j] < key) {
          dst2[j + 1] = dst2[j];
          requests[j + 1] = requests[j];
          j--;
        }
        dst2[j + 1] = key;
        requests[j + 1] = req;
      }
    }

    private static void merge(DrawRequest[] requests, float[] dst2, int left, int mid, int right) {
      int n1 = mid - left + 1;
      int n2 = right - mid;

      float[] tmpDl = tmpDst2l;
      DrawRequest[] tmpRl = tmpRequestsL;
      float[] tmpDr = tmpDst2r;
      DrawRequest[] tmpRr = tmpRequestsR;

      System.arraycopy(dst2, left, tmpDl, left, n1);
      System.arraycopy(requests, left, tmpRl, left, n1);
      System.arraycopy(dst2, mid + 1, tmpDr, mid + 1, n2);
      System.arraycopy(requests, mid + 1, tmpRr, mid + 1, n2);

      int i = 0, j = 0;
      int k = left;
      while (i < n1 && j < n2) {
        int ni = left + i;
        int nj = mid + 1 + j;
        if (tmpDl[ni] >= tmpDr[nj]) {
          dst2[k] = tmpDl[ni];
          requests[k] = tmpRl[ni];
          i++;
        } else {
          dst2[k] = tmpDr[nj];
          requests[k] = tmpRr[nj];
          j++;
        }
        k++;
      }

      while (i < n1) {
        int ni = left + i;
        dst2[k] = tmpDl[ni];
        requests[k] = tmpRl[ni];
        i++;
        k++;
      }

      while (j < n2) {
        int nj = mid + 1 + j;
        dst2[k] = tmpDr[nj];
        requests[k] = tmpRr[nj];
        j++;
        k++;
      }
    }
  }
}
