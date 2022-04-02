package singularity.world.lightnings;

import arc.func.Cons;
import arc.func.FloatFloatf;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import singularity.world.lightnings.generator.LightningGenerator;

import java.util.Iterator;
import java.util.LinkedList;

/**闪电容器，使用一个闪电生成器产生闪电，由容器进行处理和绘制，通常用于一类闪电用同一个容器存储*/
public class LightningContainer{
  /**当前创建闪电使用的闪电生成器，可随时变更，但通常来说不应该在不同线程里面调用同一个生成器，这是危险的*/
  public LightningGenerator generator;

  /**闪电从产生到完全出现需要的时间，这会平摊给每一段闪电，如果时间小于1/fps则每一段闪电固定使用1帧的时间间隔，fps为当前帧率
   * 但如果这个值为0,那么闪电会立即出现*/
  public float time = 0;
  /**闪电的扩散速度，小于或等于0时默认使用time提供的路径扩散计算方式，否则使用给出的速度来处理闪电的扩散（单位：/tick）*/
  public float speed = 0;
  /**闪电的存在时间*/
  public float lifeTime = 30;
  /**闪电每一段宽度的随机区间*/
  public float minWidth = 2.5f, maxWidth = 4.5f;
  /**闪电的衰减变换器，传入的数值为闪电的存在时间进度*/
  public FloatFloatf lerp = f -> 1 - f;

  /**闪电分支创建时调用的回调函数，一般用于定义闪电的分支子容器属性*/
  public Cons<Lightning> branchCreated;

  protected float clipSize;

  protected final LinkedList<Lightning> lightnings = new LinkedList<>();

  /**使用当前的闪电生成器在容器中创建一道新的闪电*/
  public void create(){
    generator.branched(branchCreated);
    lightnings.add(Lightning.create(
        generator,
        Mathf.random(minWidth, maxWidth),
        lifeTime,
        lerp,
        time,
        speed
    ));
  }

  /**绘制容器，这会将容器中保存的所有闪电进行绘制，并更新容器与闪电状态*/
  public void draw(){
    Iterator<Lightning> itr = lightnings.iterator();
    Lightning lightning;
    float progress;
    while(itr.hasNext()){
      lightning = itr.next();
      clipSize = Math.max(clipSize, lightning.clipSize);

      progress = (Time.time - lightning.startTime)/lifeTime;
      if(progress > 1){
        itr.remove();
        Pools.free(lightning);
        clipSize = 0;
        continue;
      }

      lightning.draw();
    }
  }

  public float clipSize(){
    return clipSize;
  }

  /**闪电分支容器，用于绘制分支闪电，会递归绘制所有的子分支*/
  public static class PoolLightningContainer extends LightningContainer implements Pool.Poolable{
    public static PoolLightningContainer create(LightningGenerator generator, float lifeTime, float minWidth, float maxWidth){
      PoolLightningContainer result = Pools.obtain(PoolLightningContainer.class, PoolLightningContainer::new);
      result.generator = generator;
      result.lifeTime = lifeTime;
      result.minWidth = minWidth;
      result.maxWidth = maxWidth;

      return result;
    }

    @Override
    public void reset(){
      time = 0;
      generator = null;
      lifeTime = 0;
      clipSize = 0;
      maxWidth = 0;
      minWidth = 0;
      lerp = f -> 1 - f;
      branchCreated = null;

      for(Lightning lightning: lightnings){
        Pools.free(lightning);
      }
      lightnings.clear();
    }
  }
}
