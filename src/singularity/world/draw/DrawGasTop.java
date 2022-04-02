package singularity.world.draw;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.world.components.DrawableComp;
import singularity.world.components.GasBuildComp;

public class DrawGasTop<T extends Building & GasBuildComp & DrawableComp> extends SglDrawBlock<T>{
  TextureRegion top;
  
  public DrawGasTop(Block block){
    super(block);
  }
  
  @Override
  public void load(){
    super.load();
    top = Core.atlas.find(block.name + "_top");
  }
  
  public class DrawGasTopDrawer extends SglDrawBlockDrawer{
    public DrawGasTopDrawer(T entity){
      super(entity);
    }
  
    @Override
    public void draw(){
      Draw.rect(region, entity.x, entity.y);
      if(entity.gases() != null){
        Draw.color(entity.gases().color());
        Draw.alpha(entity.gases().getPressure()/entity.getGasBlock().maxGasPressure());
        Draw.rect(top, entity.x, entity.y);
      }
    }
  }
}
