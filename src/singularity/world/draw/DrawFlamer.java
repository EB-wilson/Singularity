package singularity.world.draw;

import arc.func.Cons2;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Queue;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import singularity.world.components.DrawableComp;
import universecore.components.blockcomp.FactoryBuildComp;

public class DrawFlamer<Target extends Building & FactoryBuildComp & DrawableComp> extends DrawFactory<Target>{
  public Color flameColor = Pal.darkPyraFlame;
  public float alpha = 0.45f;
  public float flameSize = 5, minFlameSize = 3;
  public float cycle = 1;
  public float range = 8, minRange = 3.5f;
  public float density = 0.4f;
  
  public Cons2<Target, FlamePart> drawFlame = (entity, f) -> {
    Vec2 v = f.move();
    Fill.circle(entity.x + v.x, entity.y + v.y, f.size());
  };
  
  public DrawFlamer(Block block){
    super(block);
  }
  
  public class DrawFlamerDrawer extends DrawFactoryDrawer{
    private final Queue<FlamePart> flame = new Queue<>();
    
    public DrawFlamerDrawer(Target entity){
      super(entity);
    }
    
    public void drawFlame(){
      Draw.color(flameColor);
      if(Mathf.chanceDelta(density)){
        FlamePart f = new FlamePart();
        f.size = Mathf.random(minFlameSize, flameSize);
        Angles.randLenVectors(System.nanoTime(), 1, minRange, range, (x, y) -> f.move = new Vec2(x, y));
        flame.addLast(f);
      }
      while(true){
        if(flame.first().time >= cycle){
          flame.removeFirst();
        }else break;
      }
      
      Draw.alpha(alpha);
      for(FlamePart f : flame){
        f.time += Time.delta;
        drawFlame.get(entity, f);
      }
    }
  }
  
  private class FlamePart{
    Vec2 move;
    float size;
    float time;
    
    public float fin(){
      return time/cycle;
    }
    
    public Vec2 move(){
      return move.cpy().scl(time/cycle);
    }
    
    public float size(){
      return (float)Math.sin((time/cycle)*180)*size;
    }
  }
}
