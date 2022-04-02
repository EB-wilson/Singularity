package singularity.world.lightnings.generator;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import org.jetbrains.annotations.NotNull;
import singularity.world.lightnings.LightningVertex;

import java.util.Iterator;

/**点对点的闪电生成器，生成由指定起点到终点的闪电顶点*/
public class PointToPointGenerator extends LightningGenerator{
  public float targetX, targetY;

  float distance;

  Vec2 vector = new Vec2();
  float currentDistance;
  boolean first;

  @Override
  public @NotNull Iterator<LightningVertex> iterator(){
    currentDistance = 0;
    first = true;
    distance = vector.set(targetX - originX, targetY - originY).len();
    return super.iterator();
  }

  @Override
  public boolean hasNext(){
    return currentDistance < distance;
  }

  @Override
  protected void handleVertex(LightningVertex vertex){
    currentDistance += Mathf.random(minInterval, maxInterval);

    if(currentDistance < distance - minInterval){
      if(first){
        Tmp.v2.setZero();
      }
      else{
        float offset = Mathf.random(-maxSpread, maxSpread);

        vector.setLength(currentDistance);
        Tmp.v2.set(vector).add(Tmp.v1.set(vector).rotate90(1).setLength(offset).scl(offset < 0? -1: 1));
      }
    }
    else{
      vector.setLength(distance);
      Tmp.v2.set(vector);
      vertex.isEnd = true;
    }

    vertex.x = originX + Tmp.v2.x;
    vertex.y = originY + Tmp.v2.y;

    if(first){
      vertex.isStart = true;
      vertex.valid = true;
      first = false;
    }
  }

  @Override
  public float clipSize(){
    return distance;
  }
}
