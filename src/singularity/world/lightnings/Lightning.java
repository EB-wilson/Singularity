package singularity.world.lightnings;

import arc.func.FloatFloatf;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.graphics.Drawf;
import singularity.world.lightnings.generator.LightningGenerator;

import java.util.LinkedList;

/**
 * 单条闪电的存储容器，保存了闪电的起始时间还有闪电的顶点信息
 * 此类实例大量，应当复用
 */
public class Lightning implements Pool.Poolable{
  public final LinkedList<LightningVertex> vertices = new LinkedList<>();
  public float lifeTime;
  public float startTime;
  public float clipSize;
  public float width;

  public FloatFloatf lerp;

  public float speed;
  public float time;
  public float counter, lengthMargin;

  public static Lightning create(LightningGenerator generator, float width, float lifeTime, FloatFloatf lerp, float time){
    return create(generator, width, lifeTime, lerp, time, 0);
  }

  public static Lightning create(LightningGenerator generator, float width, float lifeTime, FloatFloatf lerp, float time, float speed){
    Lightning result = Pools.obtain(Lightning.class, Lightning::new);
    result.width = width;
    result.speed = speed;
    result.time = time;
    result.startTime = Time.time;
    result.lifeTime = lifeTime;
    result.lerp = lerp;

    generator.setCurrentGen(result);
    for(LightningVertex vertex: generator){
      result.vertices.addLast(vertex);
    }
    result.clipSize = generator.clipSize();

    return result;
  }
  
  private Lightning(){}
  
  public void draw(){
    LightningVertex last = null;
    float increase, hstroke, len;

    float lerp = this.lerp.get((Time.time - startTime)/lifeTime);

    for(LightningVertex vertex: vertices){
      if(last != null){
        if(!last.valid) break;

        if(!vertex.valid && !Vars.state.isPaused()){
          if(speed > 0){
            float distance = Mathf.len(vertex.x - last.x, vertex.y - last.y);
            if(lengthMargin >= distance){
              last.progress = 1;
              lengthMargin -= distance;
            }

            if(last.progress >= 1){
              last.progress = 1;
              vertex.valid = true;
            }

            if(!vertex.valid){
              increase = speed*Time.delta/distance;
              float delta = Math.min(increase, 1 - last.progress);
              last.progress += delta;
              lengthMargin += (increase - delta)*distance;
            }
          }
          else{
            if(time == 0) last.progress = 1;
            if(counter >= 1){
              last.progress = 1;
              counter--;
            }

            if(last.progress >= 1){
              last.progress = 1;
              vertex.valid = true;
            }

            if(!vertex.valid && time > 0){
              increase = vertices.size()/time*Time.delta;
              float delta = Math.min(increase, 1 - last.progress);
              counter += increase - delta;
              last.progress += delta;
            }
          }
        }

        hstroke = width/2f*lerp;
        Tmp.v1.set(Tmp.v2.set(vertex.x - last.x, vertex.y - last.y)).rotate90(1).setLength(hstroke);
        len = Tmp.v2.len();
        Tmp.v2.setLength(len*last.progress);

        if(last.isStart){
          Fill.tri(
              last.x + Tmp.v2.x + Tmp.v1.x,
              last.y + Tmp.v2.y + Tmp.v1.y,
              last.x + Tmp.v2.x - Tmp.v1.x,
              last.y + Tmp.v2.y - Tmp.v1.y,
              last.x,
              last.y
          );
        }
        else{
          if(vertex.isEnd){
            Fill.tri(
                last.x + Tmp.v1.x,
                last.y + Tmp.v1.y,
                last.x - Tmp.v1.x,
                last.y - Tmp.v1.y,
                last.x + Tmp.v2.x,
                last.y + Tmp.v2.y
            );
          }
          else{
            Lines.stroke(width*lerp);
            Lines.line(last.x, last.y, last.x + Tmp.v2.x, last.y + Tmp.v2.y);
          }
        }
        Drawf.light(Team.derelict,last.x, last.y, vertex.x, vertex.y, hstroke*4.5f, Draw.getColor(), 0.7f*lerp);
        if(vertex.valid) vertex.draw();
      }

      last = vertex;
    }
  }

  @Override
  public void reset(){
    for(LightningVertex vertex: vertices){
      Pools.free(vertex);
    }
    vertices.clear();
    counter = 0;
    width = 0;
    speed = 0;
    time = 0;
    lifeTime = 0;
    lerp = null;
    lengthMargin = 0;
    startTime = 0;
    clipSize = 0;
  }
}
