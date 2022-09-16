package singularity.world.draw;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import singularity.world.components.DrawableComp;
import universecore.components.blockcomp.FactoryBuildComp;

import static mindustry.Vars.tilesize;

public class SglDrawPlasma<T extends Building & FactoryBuildComp & DrawableComp> extends DrawFactory<T>{
  public TextureRegion[] plasmas;
  public Color plasma1 = Color.valueOf("ffd06b"), plasma2 = Color.valueOf("ff361b");
  public float lightRadius = 60f;
  public Color lightColor = Pal.lightFlame;
  public float lightAlpha = 0.65f;

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
      Draw.alpha((0.3f + Mathf.absin(Time.time, 2f + i * 2f, 0.3f + i * 0.05f)) * entity.workEfficiency());
      Draw.blend(Blending.additive);
      Draw.rect(plasmas[i], entity.x, entity.y, r, r, Time.time * (12 + i * 6f) * entity.workEfficiency());
      Draw.blend();
    }
    Draw.color();
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

    @Override
    public void drawLight(){
      Drawf.light(entity.x(), entity.y(), lightRadius * entity.workEfficiency() * block.size, lightColor, lightAlpha);
    }
  }
}
