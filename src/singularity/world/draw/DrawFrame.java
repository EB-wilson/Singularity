package singularity.world.draw;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.world.Block;

/**用于绘制逐帧动画的工具类*/
public class DrawFrame extends SglDrawBlock{
  /**存储帧贴图，数组一维为显示的层，第二维存储帧贴图*/
  public TextureRegion[][] frames;
  
  /**帧控制器，重写该方法的返回值以控制某层的动画
   * @param index 层索引，为层的序数*/
  public int framesControl(int index, Building e){
    return 0;
  }
  
  /**帧控制器，重写该方法的返回值以控制某层的旋转
   * @param index 层索引，为层的序数*/
  public float rotationControl(int index, Building e){
    return 0f;
  }
  
  /**帧控制器，重写该方法的返回值以控制某层的透明度
   * @param index 层索引，为层的序数*/
  public float alphaControl(int index, Building e){
    return 1;
  }
  
  @Override
  public void load(Block block){
  
  }
  
  @Override
  public void draw(Building entity){
    for(int layer=0; layer<frames.length; layer++){
      Draw.alpha(alphaControl(layer, entity));
      Draw.rect(frames[layer][Math.min(frames[layer].length - 1, framesControl(layer, entity))], entity.x, entity.y, rotationControl(layer, entity));
    }
    Draw.blend();
  }
  
  @Override
  public TextureRegion[] icons(Block block){
    TextureRegion[] icons = new TextureRegion[frames.length];
    for(int layer=0; layer < icons.length; layer++){
      icons[layer] = frames[layer][0];
    }
    return icons;
  }
}