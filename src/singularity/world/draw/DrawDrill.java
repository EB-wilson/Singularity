package singularity.world.draw;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.graphics.Drawf;
import mindustry.world.Block;
import singularity.world.blocks.drills.BaseDrill;

public class DrawDrill<Target extends BaseDrill.BaseDrillBuild> extends SglDrawBlock<Target>{
  public TextureRegion rotator, top, liquid, rim;
  public Color heatColor, liquidColor;
  
  public DrawDrill(Block block){
    super(block);
    heatColor = ((BaseDrill)block).heatColor;
    liquidColor = ((BaseDrill)block).liquidColor;
  }

  @Override
  public void load(){
    super.load();
    rotator = Core.atlas.find(block.name + "_rotator");
    top = Core.atlas.has(block.name + "_top")? Core.atlas.find(block.name + "_top"): null;
    liquid = Core.atlas.has(block.name + "_liquid")? Core.atlas.find(block.name + "_liquid"): null;
    rim = Core.atlas.has(block.name + "_rim")? Core.atlas.find(block.name + "_rim"): null;
  }
  
  @Override
  public TextureRegion[] icons(){
    return top != null? new TextureRegion[]{
      region,
      rotator,
      top
    }: new TextureRegion[]{
      region,
      rotator
    };
  }
  
  public class DrawDrillDrawer extends SglDrawBlockDrawer{
    public DrawDrillDrawer(Target entity){
      super(entity);
    }
  
    @Override
    public void draw(){
      Draw.rect(region, entity.x(), entity.y());
    
      if(rim != null){
        Draw.color(heatColor);
        Draw.alpha(entity.warmup * 0.6f * (1f - 0.3f + Mathf.absin(Time.time, 3f, 0.3f)));
        Draw.blend(Blending.additive);
        Draw.rect(rim, entity.x(), entity.y());
        Draw.blend();
        Draw.color();
      }
    
      if(liquid != null){
        Draw.color(liquidColor);
        Draw.rect(liquid, entity.x(), entity.y());
        Draw.blend();
        Draw.color();
      }
    
      Drawf.spinSprite(rotator, entity.x(), entity.y(), entity.rotatorAngle*entity.block().rotatorSpeed);
      Draw.blend();
    
      if(top != null){
        Draw.rect(top, entity.x(), entity.y());
      }
    }
  }
}