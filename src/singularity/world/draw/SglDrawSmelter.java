package singularity.world.draw;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.Block;

public class SglDrawSmelter extends DrawFactory{
  public Color flameColor = Color.valueOf("ffc999");
  public TextureRegion top;
  public float lightRadius = 60f, lightAlpha = 0.65f, lightSinScl = 10f, lightSinMag = 5;
  public float flameRadius = 3f, flameRadiusIn = 1.9f, flameRadiusScl = 5f, flameRadiusMag = 2f, flameRadiusInMag = 1f;
  
  public SglDrawSmelter(){
  }
  
  public SglDrawSmelter(Color flameColor){
    this.flameColor = flameColor;
  }
  
  @Override
  public void load(Block block){
    top = Core.atlas.find(block.name + "_top");
    block.clipSize = Math.max(block.clipSize, (lightRadius + lightSinMag) * 2f * block.size);
  }
  
  @Override
  public void draw(Building build){
    Draw.rect(build.block.region, build.x, build.y, build.block.rotate ? build.rotdeg() : 0);
    
    if(warmup(build) > 0f && flameColor.a > 0.001f){
      float g = 0.3f;
      float r = 0.06f;
      float cr = Mathf.random(0.1f);
      
      Draw.z(Layer.block + 0.01f);
      
      Draw.alpha(((1f - g) + Mathf.absin(Time.time, 8f, g) + Mathf.random(r) - r) * warmup(build));
      
      Draw.tint(flameColor);
      Fill.circle(build.x, build.y, flameRadius + Mathf.absin(Time.time, flameRadiusScl, flameRadiusMag) + cr);
      Draw.color(1f, 1f, 1f, warmup(build));
      Draw.rect(top, build.x, build.y);
      Fill.circle(build.x, build.y, flameRadiusIn + Mathf.absin(Time.time, flameRadiusScl, flameRadiusInMag) + cr);
      
      Draw.color();
    }
  }
  
  @Override
  public void drawLight(Building build){
    Drawf.light(build.team, build.x, build.y, (lightRadius + Mathf.absin(lightSinScl, lightSinMag)) * warmup(build) * build.block.size, flameColor, lightAlpha);
  }
  
  @Override
  public TextureRegion[] icons(Block block){
    return new TextureRegion[]{region};
  }
}
