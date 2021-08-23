package singularity.world.draw;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.Building;
import mindustry.world.Block;

public class SglDrawBlock{
  public TextureRegion region;
  
  public void load(Block block){
    region = Core.atlas.find(block.name);
  }
  
  public void draw(Building entity){
    Draw.rect(region, entity.x, entity.y);
  }
  
  public TextureRegion[] icons(Block block){
    return new TextureRegion[]{region};
  }
  
  public void drawLight(Building entity){}
}