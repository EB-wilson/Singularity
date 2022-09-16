package singularity.world.draw;

import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import singularity.world.components.DrawableComp;

public class DrawerGroup<T extends DrawableComp> extends SglDrawBase<T>{
  public Seq<SglDrawBase<T>> draws = new Seq<>();
  public TextureRegion[] icon;

  public void setIcon(TextureRegion... iconLayers){
    icon = iconLayers;
  }

  @Override
  public void load(){
    super.load();
    for(SglDrawBase<T> draw: draws){
      draw.load();
    }
  }

  @Override
  public void loadType(){
    super.loadType();
    for(SglDrawBase<T> draw: draws){
      draw.loadType();
    }
  }

  @Override
  public TextureRegion[] icons(){
    return icon;
  }

  public DrawerGroup<T> addDrawer(SglDrawBase<T>... draw){
    draws.addAll(draw);
    return this;
  }

  public class DrawerGroupDrawer extends SglBaseDrawer{
    Seq<SglBaseDrawer> drawers = new Seq<>();

    public DrawerGroupDrawer(T entity){
      super(entity);

      for(SglDrawBase<T> draw: draws){
        drawers.add(draw.get(entity));
      }
    }

    @Override
    public void render(){
      for(SglDrawBase<T>.SglBaseDrawer drawer: drawers){
        drawer.render();
      }

      super.render();
    }
  }
}
