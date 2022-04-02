package singularity.world.draw;

public class SglBaseDrawer<Target>{
  /**绘制时执行的目标对象*/
  protected Target entity;
  
  public SglBaseDrawer(Target entity){
    this.entity = entity;
  }
  
  /**draw渲染执行时调用此方法*/
  public void doDraw(){
  }
  
  /**draw主方法*/
  public void draw(){
  }
  
  /**渲染光照*/
  public void drawLight(){
  }
}
