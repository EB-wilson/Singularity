package singularity.world.draw;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.graphics.Drawf;
import mindustry.world.Block;
import singularity.Singularity;
import singularity.world.blocks.product.NormalCrafter;

public class DrawFactory<Target extends NormalCrafter.NormalCrafterBuild> extends SglDrawBlock<Target>{
  public TextureRegion rotator, top, bottom, liquid;
  
  public float rotationScl;
  
  public DrawFactory(Block block){
    super(block);
  }

  @Override
  public void load(){
    super.load();
    rotator = Core.atlas.has(block.name + "_rotator")? Core.atlas.find(block.name + "_rotator"): null;
    top = Core.atlas.has(block.name + "_top")? Core.atlas.find(block.name + "_top"): null;
    bottom = Core.atlas.has(block.name + "_bottom")? Core.atlas.find(block.name + "_bottom"): Singularity.getModAtlas("bottom_" + block.size);
    liquid = Core.atlas.has(block.name + "_liquid")? Core.atlas.find(block.name + "_liquid"): null;
  }
  
  @Override
  public TextureRegion[] icons(){
    return top != null? new TextureRegion[]{
      bottom,
      region,
      top
    }: new TextureRegion[]{
      bottom,
      region
    };
  }
  
  public class DrawFactoryDrawer extends SglDrawBlockDrawer{
    public DrawFactoryDrawer(Target entity){
      super(entity);
    }
  
    @Override
    public void draw(){
      Draw.rect(bottom, entity.x(), entity.y());
      Draw.rect(region, entity.x(), entity.y(), block.rotate ? entity.rotation()*90 : 0);
      if(rotator != null) Drawf.spinSprite(rotator, entity.x(), entity.y(), entity.totalProgress*rotationScl);
      if(top != null) Draw.rect(top, entity.x(), entity.y());
      Draw.blend();
    }
  }
}