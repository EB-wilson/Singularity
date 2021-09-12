package singularity.world.draw;

import mindustry.graphics.Drawf;
import singularity.world.blocks.drills.BaseDrill.BaseDrillBuild;
import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.Block;

public class DrawDrill extends SglDrawBlock{
  public TextureRegion rotator, top, liquid, rim;

  public float rotation(Building entity){
    if(!(entity instanceof BaseDrillBuild)) return 0f;
    return ((BaseDrillBuild)entity).rotatorAngle*((BaseDrillBuild)entity).block().rotatorSpeed;
  }
  
  public Color heatColor(Building entity){
    if(!(entity instanceof BaseDrillBuild)) return Color.black;
    return ((BaseDrillBuild)entity).block().heatColor;
  }
  
  public float warmup(Building entity){
    if(!(entity instanceof BaseDrillBuild)) return 0f;
    return ((BaseDrillBuild)entity).warmup;
  }

  @Override
  public void load(Block block){
    super.load(block);
    rotator = Core.atlas.find(block.name + "_rotator");
    top = Core.atlas.has(block.name + "_top")? Core.atlas.find(block.name + "_top"): null;
    liquid = Core.atlas.has(block.name + "_liquid")? Core.atlas.find(block.name + "_liquid"): null;
    rim = Core.atlas.has(block.name + "_rim")? Core.atlas.find(block.name + "_rim"): null;
  }
  
  @Override
    public void draw(Building entity){
      Draw.rect(region, entity.x, entity.y);
      
      if(rim != null){
        Draw.color(heatColor(entity));
        Draw.alpha(warmup(entity) * 0.6f * (1f - 0.3f + Mathf.absin(Time.time, 3f, 0.3f)));
        Draw.blend(Blending.additive);
        Draw.rect(rim, entity.x, entity.y);
        Draw.blend();
        Draw.color();
      }
      
      if(liquid != null){
        Draw.color(entity.liquids.current().color);
        Draw.rect(liquid, entity.x, entity.y);
        Draw.blend();
        Draw.color();
      }
  
      Drawf.spinSprite(rotator, entity.x, entity.y, rotation(entity));
      Draw.blend();
      
      if(top != null){
        Draw.rect(top, entity.x, entity.y);
      }
    }
  
  @Override
  public TextureRegion[] icons(Block block){
    return top != null? new TextureRegion[]{
      region,
      rotator,
      top
    }: new TextureRegion[]{
      region,
      rotator
    };
  }
}