package singularity.world.draw;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import singularity.world.blocks.product.NormalCrafter;

public class SglDrawSmelter<Target extends NormalCrafter.NormalCrafterBuild> extends DrawFactory<Target>{
  public Color flameColor = Color.valueOf("ffc999");
  public TextureRegion top;
  public float lightRadius = 60f, lightAlpha = 0.65f, lightSinScl = 10f, lightSinMag = 5;
  public float flameRadius = 3f, flameRadiusIn = 1.9f, flameRadiusScl = 5f, flameRadiusMag = 2f, flameRadiusInMag = 1f;
  
  public SglDrawSmelter(Block block, Color flameColor){
    super(block);
    this.flameColor = flameColor;
  }
  
  public SglDrawSmelter(Block block){
    super(block);
  }
  
  @Override
  public void load(){
    super.load();
    top = Core.atlas.find(block.name + "_top");
    block.clipSize = Math.max(block.clipSize, (lightRadius + lightSinMag) * 2f * block.size);
  }
  
  @Override
  public TextureRegion[] icons(){
    return new TextureRegion[]{region};
  }
  
  public class SglDrawSmelterDrawer extends DrawFactoryDrawer{
    public SglDrawSmelterDrawer(Target entity){
      super(entity);
    }
  
    @Override
    public void draw(){
      Draw.rect(region, entity.x(), entity.y(), block.rotate ? entity.rotation()*90 : 0);
    
      if(entity.warmup > 0f && flameColor.a > 0.001f){
        float g = 0.3f;
        float r = 0.06f;
        float cr = Mathf.random(0.1f);
      
        Draw.z(Layer.block + 0.01f);
      
        Draw.alpha(((1f - g) + Mathf.absin(Time.time, 8f, g) + Mathf.random(r) - r) * entity.warmup);
      
        Draw.tint(flameColor);
        Fill.circle(entity.x(), entity.y(), flameRadius + Mathf.absin(Time.time, flameRadiusScl, flameRadiusMag) + cr);
        Draw.color(1f, 1f, 1f, entity.warmup);
        Draw.rect(top, entity.x(), entity.y());
        Fill.circle(entity.x(), entity.y(), flameRadiusIn + Mathf.absin(Time.time, flameRadiusScl, flameRadiusInMag) + cr);
      
        Draw.color();
      }
    }
  
    @Override
    public void drawLight(){
      Drawf.light(entity.team, entity.x(), entity.y(), (lightRadius + Mathf.absin(lightSinScl, lightSinMag)) * entity.warmup * block.size, flameColor, lightAlpha);
    }
  }
}
