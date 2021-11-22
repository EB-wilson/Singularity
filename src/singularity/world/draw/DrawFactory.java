package singularity.world.draw;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import mindustry.graphics.Drawf;
import mindustry.world.Block;
import singularity.Singularity;
import singularity.world.blocks.product.NormalCrafter;

public class DrawFactory<Target extends NormalCrafter.NormalCrafterBuild> extends SglDrawBlock<Target>{
  public TextureRegion rotator, top, bottom, liquid;
  
  public float rotationScl;
  public boolean iconRotator = false;
  
  public DrawFactory(Block block){
    super(block);
  }

  @Override
  public void load(){
    super.load();
    rotator = Core.atlas.find(block.name + "_rotator", Core.atlas.find(block.name + "-rotator", (TextureRegion)null));
    top = Core.atlas.find(block.name + "_top", Core.atlas.find(block.name + "-top", (TextureRegion) null));
    bottom = Core.atlas.find(block.name + "_bottom", Core.atlas.find(block.name + "-bottom", Singularity.getModAtlas("bottom_" + block.size)));
    liquid = Core.atlas.find(block.name + "_liquid", Core.atlas.find(block.name + "-liquid", (TextureRegion) null));
  }
  
  @Override
  public TextureRegion[] icons(){
    Seq<TextureRegion> result = new Seq<>();
    result.add(bottom);
    if(rotator != null && iconRotator) result.add(rotator);
    result.add(region);
    if(top != null) result.add(top);
    return result.toArray(TextureRegion.class);
  }
  
  public float liquidAlpha(Target entity){
    return entity.liquids.currentAmount()/entity.block.liquidCapacity;
  }
  
  public Color liquidColor(Target entity){
    return entity.liquids.current().color;
  }
  
  public class DrawFactoryDrawer extends SglDrawBlockDrawer{
    public DrawFactoryDrawer(Target entity){
      super(entity);
    }
  
    @Override
    public void draw(){
      Draw.rect(bottom, entity.x(), entity.y());
      Draw.rect(region, entity.x(), entity.y(), block.rotate ? entity.rotation()*90 : 0);
      if(liquid != null) Drawf.liquid(liquid, entity.x, entity.y,
          liquidAlpha(entity), liquidColor(entity), block.rotate ? entity.rotation()*90 : 0);
      if(rotator != null) Drawf.spinSprite(rotator, entity.x(), entity.y(), entity.totalProgress*rotationScl);
      if(top != null) Draw.rect(top, entity.x(), entity.y());
      Draw.blend();
    }
  }
}