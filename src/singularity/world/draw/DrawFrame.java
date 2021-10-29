package singularity.world.draw;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.world.Block;
import singularity.world.blockComp.DrawableComp;

/**用于绘制逐帧动画的工具类*/
public class DrawFrame<Target extends DrawableComp> extends SglDrawBlock<Target>{
  /**存储帧贴图，数组一维为显示的层，第二维存储帧贴图*/
  public TextureRegion[][] frames;
  
  public DrawFrame(Block block){
    super(block);
  }
  
  /**帧控制器，重写该方法的返回值以控制某层的动画
   * @param index 层索引，为层的序数*/
  public int framesControl(int index, Target e){
    return 0;
  }
  
  /**帧控制器，重写该方法的返回值以控制某层的旋转
   * @param index 层索引，为层的序数*/
  public float rotationControl(int index, Target e){
    return 0f;
  }
  
  /**帧控制器，重写该方法的返回值以控制某层的透明度
   * @param index 层索引，为层的序数*/
  public float alphaControl(int index, Target e){
    return 1f;
  }
  
  @Override
  public TextureRegion[] icons(){
    TextureRegion[] icons = new TextureRegion[frames.length];
    for(int layer=0; layer < icons.length; layer++){
      icons[layer] = frames[layer][0];
    }
    return icons;
  }
  
  public class DrawFrameDrawer extends SglDrawBlockDrawer{
    public DrawFrameDrawer(Target entity){
      super(entity);
    }
  
    public int framesControl(int index){
      return DrawFrame.this.framesControl(index, entity);
    }
  
    public float rotationControl(int index){
      return DrawFrame.this.rotationControl(index, entity);
    }
  
    public float alphaControl(int index){
      return DrawFrame.this.alphaControl(index, entity);
    }
  
    @Override
    public void draw(){
      for(int layer=0; layer<frames.length; layer++){
        Draw.alpha(alphaControl(layer));
        Draw.rect(frames[layer][Math.min(frames[layer].length - 1, framesControl(layer))], entity.x(), entity.y(), rotationControl(layer));
      }
      Draw.blend();
    }
  }
}