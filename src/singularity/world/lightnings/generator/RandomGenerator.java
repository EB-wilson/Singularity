package singularity.world.lightnings.generator;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import org.jetbrains.annotations.NotNull;
import singularity.world.lightnings.LightningVertex;

import java.util.Iterator;

/**随机路径的闪电生成器，给出起点路径总长度生成随机闪电路径*/
public class RandomGenerator extends LightningGenerator{
  public float maxLength = 80;
  public float maxDeflect = 75;
  public float originAngle = Float.MIN_VALUE;

  float currLength;
  Vec2 curr = new Vec2();

  boolean first;
  float maxDistance;

  @Override
  public @NotNull Iterator<LightningVertex> iterator(){
    currLength = 0;
    maxDistance = 0;
    first = true;
    if(originAngle == Float.MIN_VALUE){
      curr.rnd(0.001f);
    }
    else{
      curr.set(0.001f, 0).setAngle(originAngle);
    }
    return this;
  }

  @Override
  protected void handleVertex(LightningVertex vertex){
    if(first){
      vertex.isStart = true;
      vertex.valid = true;
      first = false;
    }
    else{
      float distance = Mathf.random(minInterval, maxInterval);
      if(currLength + distance > maxLength){
        vertex.isEnd = true;
      }

      currLength += distance;
      Tmp.v1.setLength(distance).setAngle(curr.angle() + Mathf.random(-maxDeflect, maxDeflect));
      curr.add(Tmp.v1);
      maxDistance = Math.max(maxDistance, curr.len());
    }

    vertex.x = originX + curr.x;
    vertex.y = originY + curr.y;
  }

  @Override
  public float clipSize(){
    return maxDistance;
  }

  @Override
  public boolean hasNext(){
    return currLength < maxLength;
  }
}
