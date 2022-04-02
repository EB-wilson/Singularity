package singularity.world.lightnings.generator;

import arc.func.Cons;
import arc.func.Func2;
import arc.math.Mathf;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import org.jetbrains.annotations.NotNull;
import singularity.world.lightnings.Lightning;
import singularity.world.lightnings.LightningVertex;

import java.util.Iterator;

/**闪电生成器基类，同时实现了Iterator和Iterable接口，可以使用for-each循环形式逐个产生顶点，每一次获取迭代器都将返回生成器自身，并重置迭代状态
 * 注意，任何在迭代器运作外的时机变更生成器属性，都会直接影响迭代产生的顶点分布情况，而生成器是可复用的，每次迭代都会产生互不相关的一组顶点
 * <p>警告：这个方法不是线程安全的，任何时候要避免在一次迭代结束前再次获取迭代器*/
public abstract class LightningGenerator implements Iterable<LightningVertex>, Iterator<LightningVertex>{
  public float originX, originY;

  /**顶点基准间距最小值*/
  public float minInterval = 8;
  /**顶点基准位置最大值*/
  public float maxInterval = 16;

  /**闪电顶点离散程度，越高则顶点偏移越远*/
  public float maxSpread = 12.25f;

  /**产生分支的几率（每一个顶点）*/
  public float branchChance;
  /**最小分支强度*/
  public float minBranchStrength = 0.3f;
  /**最大分支强度*/
  public float maxBranchStrength = 0.8f;
  /**分支创建器，传入分支所在的顶点以及分支的强度，需要返回一个闪电生成器，注意，任何生成器对象都可以被传入，请不要new创建生成器*/
  public Func2<LightningVertex, Float, LightningGenerator> branchMaker;

  public Cons<Lightning> branchCreated;

  protected Lightning curr;

  protected LightningVertex last;

  public static final Pool<LightningVertex> vertexPool;

  static {
    Pools.set(LightningVertex.class, vertexPool = new Pool<>(8192, 65536){
      @Override
      protected LightningVertex newObject(){
        return new LightningVertex();
      }
    });
  }

  public void setCurrentGen(Lightning curr){
    this.curr = curr;
  }

  public void branched(Cons<Lightning> branchCreated){
    this.branchCreated = branchCreated;
  }

  /**用分支创建器对顶点创建一条分支闪电*/
  public void createBranch(LightningVertex vertex){
    float strength = Mathf.clamp(Mathf.random(minBranchStrength, maxBranchStrength));
    vertex.branchOther = Lightning.create(
        branchMaker.get(vertex, strength),
        curr.width*strength,
        curr.lifeTime,
        curr.lerp,
        curr.time,
        curr.speed
    );

    vertex.branchOther.vertices.getFirst().isStart = false;
    vertex.isBranch = true;

    if(branchCreated != null) branchCreated.get(vertex.branchOther);
  }

  /**此类同时实现了可迭代和迭代器接口，即可以进行for-each循环来逐个产生顶点，需要此方法返回对象本身，而此方法应当被重写以在调用时重置迭代器状态，这意味着这个方法不是线程安全的*/
  @NotNull
  @Override
  public Iterator<LightningVertex> iterator(){
    return this;
  }

  /**迭代器通过这个方法获取下一个顶点*/
  @Override
  public LightningVertex next(){
    LightningVertex vertex = Pools.obtain(LightningVertex.class, null);
    handleVertex(vertex);
    afterHandle(vertex);
    if(!vertex.isStart && !vertex.isEnd && branchChance > 0 && Mathf.chance(branchChance)){
      createBranch(vertex);
    }
    last = vertex;
    return vertex;
  }

  /**在顶点处理之后调用*/
  public void afterHandle(LightningVertex vertex){
    if(last == null) return;
    vertex.angle = Mathf.angle(vertex.x - last.x, vertex.y - last.y);
  }

  /**顶点处理，实现以为顶点分配属性，如坐标等*/
  protected abstract void handleVertex(LightningVertex vertex);

  /**返回当前闪电的裁剪大小，此大小应当能够完整绘制闪电*/
  public abstract float clipSize();
}
