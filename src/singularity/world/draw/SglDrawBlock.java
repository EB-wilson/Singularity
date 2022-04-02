package singularity.world.draw;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.world.Block;
import singularity.world.components.DrawableComp;

public class SglDrawBlock<Target extends DrawableComp> extends SglDrawBase<Target>{

  public Block block;
  
  public SglDrawBlock(Block block){
    this.block = block;
  }
  
  public TextureRegion region;

  @Override
  public void load(){
    loadType();
    
    region = Core.atlas.find(block.name);
  }
  
  @Override
  public TextureRegion[] icons(){
    return new TextureRegion[]{region};
  }
  
  public class SglDrawBlockDrawer extends SglBaseDrawer<Target>{
    public SglDrawBlockDrawer(Target entity){
      super(entity);
    }
  
    @Override
    public void doDraw(){
      if(drawDef == null){
        draw();
      }
      else drawDef.get(entity);
      
      Draw.reset();
    }
  
    public void draw(){
      Draw.rect(region, entity.x(), entity.y());
    }
  }
}