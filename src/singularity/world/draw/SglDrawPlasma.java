package singularity.world.draw;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.world.Block;
import singularity.world.blocks.product.NormalCrafter;

import static mindustry.Vars.tilesize;

public class SglDrawPlasma<T extends NormalCrafter.NormalCrafterBuild> extends DrawFactory<T>{
  public TextureRegion[] plasmas;
  public Color plasma1 = Color.valueOf("ffd06b"), plasma2 = Color.valueOf("ff361b");
  
  public SglDrawPlasma(Block block, int length){
    super(block);
    plasmas = new TextureRegion[length];
  }
  
  @Override
  public void load(){
    super.load();
    for(int i=0; i<plasmas.length; i++){
      plasmas[i] = Core.atlas.find(block.name + "_plasma_" + i);
    }
  }
  
  public void drawPlasma(T entity){
    for(int i = 0; i < plasmas.length; i++){
      float r = block.size * tilesize - 3f + Mathf.absin(Time.time, 2f + i * 1f, 5f - i * 0.5f);
      
      Draw.color(plasma1, plasma2, (float)i / plasmas.length);
      Draw.alpha((0.3f + Mathf.absin(Time.time, 2f + i * 2f, 0.3f + i * 0.05f)) * warmup(entity));
      Draw.blend(Blending.additive);
      Draw.rect(plasmas[i], entity.x, entity.y, r, r, Time.time * (12 + i * 6f) * warmup(entity));
      Draw.blend();
    }
    Draw.color();
  }
  
  public float warmup(T entity){
    return entity.warmup;
  }
  
  public class SglDrawPlasmaDrawer extends DrawFactoryDrawer{
    public SglDrawPlasmaDrawer(T entity){
      super(entity);
    }
  
    @Override
    public void draw(){
      Draw.rect(bottom, entity.x, entity.y);
      drawPlasma(entity);
      Draw.rect(region, entity.x, entity.y);
    }
  }
}
