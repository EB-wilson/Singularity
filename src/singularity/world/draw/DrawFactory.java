package singularity.world.draw;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.world.Block;
import singularity.Singularity;
import singularity.world.blocks.product.NormalCrafter.NormalCrafterBuild;

public class DrawFactory extends SglDrawBlock{
  public TextureRegion rotator, top, bottom, liquid;
  
  public float rotationScl;

  public float progress(Building entity){
    if(!(entity instanceof NormalCrafterBuild)) return 0f;
    return ((NormalCrafterBuild)entity).progress;
  }
  
  public float totalProgress(Building entity){
    if(!(entity instanceof NormalCrafterBuild)) return 0f;
    return ((NormalCrafterBuild)entity).totalProgress;
  }
  
  public float warmup(Building entity){
    if(!(entity instanceof NormalCrafterBuild)) return 0f;
    return ((NormalCrafterBuild)entity).warmup;
  }

  @Override
  public void load(Block block){
    super.load(block);
    rotator = Core.atlas.has(block.name + "_rotator")? Core.atlas.find(block.name + "_rotator"): null;
    top = Core.atlas.has(block.name + "_top")? Core.atlas.find(block.name + "_top"): null;
    bottom = Core.atlas.has(block.name + "_bottom")? Core.atlas.find(block.name + "_bottom"): Singularity.getModAtlas("bottom_" + block.size);
    liquid = Core.atlas.has(block.name + "_liquid")? Core.atlas.find(block.name + "_liquid"): null;
  }
  
  @Override
  public void draw(Building entity){
    Draw.rect(bottom, entity.x, entity.y);
    Draw.rect(region, entity.x, entity.y, entity.block().rotate ? entity.rotdeg() : 0);
    if(rotator != null) Drawf.spinSprite(rotator, entity.x, entity.y, totalProgress(entity)*rotationScl);
    if(top != null) Draw.rect(top, entity.x, entity.y);
    Draw.blend();
  }
  
  @Override
  public TextureRegion[] icons(Block block){
    return top != null? new TextureRegion[]{
      bottom,
      region,
      top
    }: new TextureRegion[]{
      bottom,
      region
    };
  }
}