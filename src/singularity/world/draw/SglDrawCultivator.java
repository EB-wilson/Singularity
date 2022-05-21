package singularity.world.draw;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Rand;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.world.Block;
import singularity.world.components.DrawableComp;
import universecore.components.blockcomp.FactoryBuildComp;

public class SglDrawCultivator<T extends Building & FactoryBuildComp & DrawableComp> extends DrawFactory<T>{
  public Color plantColor = Color.valueOf("5541b1");
  public Color plantColorLight = Color.valueOf("7457ce");
  
  public int bubbles = 12, sides = 8;
  public float strokeMin = 0.2f, spread = 3f, timeScl = 70f;
  public float recurrence = 6f, radius = 3f;
  
  public SglDrawCultivator(Block block){
    super(block);
  }
  
  @Override
  public float liquidAlpha(T entity){
    return entity.warmup();
  }
  
  @Override
  public Color liquidColor(T entity){
    return plantColor;
  }
  
  public Color liquidColorLight(T entity){
    return plantColorLight;
  }
  
  public class SglDrawCultivatorDrawer extends DrawFactoryDrawer{
    protected final Rand rand = new Rand();
    
    public SglDrawCultivatorDrawer(T entity){
      super(entity);
    }
  
    @Override
    public void draw(){
      Draw.rect(bottom, entity.x, entity.y);
      drawLiquidRipple();
      Draw.rect(region, entity.x, entity.y);
      if(top != null) Draw.rect(top, entity.x, entity.y);
    }
    
    public void drawLiquidRipple(){
      rand.setSeed(entity.pos());
      Drawf.liquid(liquid, entity.x, entity.y, liquidAlpha(entity), liquidColor(entity));
  
      Draw.color(liquidColorLight(entity));
      Draw.alpha(entity.warmup());
      for(int i = 0; i < bubbles; i++){
        float x = rand.range(spread), y = rand.range(spread);
        float life = 1f - ((Time.time / timeScl + rand.random(recurrence)) % recurrence);
    
        if(life > 0){
          Lines.stroke(entity.warmup() * (life + strokeMin));
          Lines.poly(entity.x + x, entity.y + y, sides, (1f - life) * radius);
        }
      }
      Draw.color();
    }
  }
}
