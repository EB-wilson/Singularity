package singularity.world.draw;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Rand;
import arc.util.Time;
import mindustry.graphics.Drawf;
import mindustry.world.Block;
import singularity.world.blocks.product.NormalCrafter;

public class SglDrawCultivator<T extends NormalCrafter.NormalCrafterBuild> extends SglDrawBlock<T>{
  public Color plantColor = Color.valueOf("5541b1");
  public Color plantColorLight = Color.valueOf("7457ce");
  public Color bottomColor = Color.valueOf("474747");
  
  public int bubbles = 12, sides = 8;
  public float strokeMin = 0.2f, spread = 3f, timeScl = 70f;
  public float recurrence = 6f, radius = 3f;
  
  public TextureRegion liquid;
  public TextureRegion top;
  
  public SglDrawCultivator(Block block){
    super(block);
  }
  
  @Override
  public void load(){
    super.load();
    liquid = Core.atlas.find(block.name + "_liquid");
    top = Core.atlas.find(block.name + "_top");
  }
  
  @Override
  public TextureRegion[] icons(){
    return new TextureRegion[]{block.region, top};
  }
  
  public class SglDrawCultivatorDrawer extends SglDrawBlockDrawer{
    protected final Rand rand = new Rand();
    
    public SglDrawCultivatorDrawer(T entity){
      super(entity);
    }
  
    @Override
    public void draw(){
      rand.setSeed(entity.pos());
      
      Draw.rect(region, entity.x, entity.y);
      Drawf.liquid(liquid, entity.x, entity.y, entity.warmup, plantColor);
    
      Draw.color(bottomColor, plantColorLight, entity.warmup);
      
      for(int i = 0; i < bubbles; i++){
        float x = rand.range(spread), y = rand.range(spread);
        float life = 1f - ((Time.time / timeScl + rand.random(recurrence)) % recurrence);
      
        if(life > 0){
          Lines.stroke(entity.warmup * (life + strokeMin));
          Lines.poly(entity.x + x, entity.y + y, sides, (1f - life) * radius);
        }
      }
    
      Draw.color();
      Draw.rect(top, entity.x, entity.y);
    }
  }
}
