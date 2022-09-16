package singularity.world.draw;

import arc.func.Cons;
import arc.func.Func;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import arc.util.Structs;
import singularity.world.components.DrawableComp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class SglDrawBase<Target extends DrawableComp>{
  public Cons<Target> drawDef;
  
  public Func<Target, ? extends SglBaseDrawer> drawerType;
  
  @SuppressWarnings("unchecked")
  public SglBaseDrawer get(DrawableComp entity){
    return drawerType.get((Target) entity);
  }
  
  public void load(){
    loadType();
  }
  
  abstract public TextureRegion[] icons();
  
  @SuppressWarnings("unchecked")
  public void loadType(){
    try{
      Class<?> current = getClass();
      if(current.isAnonymousClass()){
        current = current.getSuperclass();
      }
      
      while(drawerType == null && SglDrawBase.class.isAssignableFrom(current)){
        Class<?> type = Structs.find(current.getDeclaredClasses(), t -> SglBaseDrawer.class.isAssignableFrom(t) && !t.isInterface());
        if(type != null){
          Constructor<? extends SglBaseDrawer> cstr = (Constructor<? extends SglBaseDrawer>) type.getDeclaredConstructors()[0];
          drawerType = ent -> {
            try{
              return cstr.newInstance(this, ent);
            }catch(InstantiationException | IllegalAccessException | InvocationTargetException e){
              Log.err(e);
              return new SglBaseDrawer(ent);
            }
          };
        }
        current = current.getSuperclass();
      }
    }catch(Throwable e){
      Log.err(e);
    }
    
    if(drawerType == null){
      drawerType = SglBaseDrawer::new;
    }
  }

  public class SglBaseDrawer{
    /**绘制时执行的目标对象*/
    protected Target entity;

    public SglBaseDrawer(Target entity){
      this.entity = entity;
    }

    /**draw渲染执行时调用此方法*/
    public void render(){
      if(drawDef == null){
        draw();
      }
      else drawDef.get(entity);

      Draw.reset();
    }

    /**draw主方法*/
    public void draw(){
    }

    /**渲染光照*/
    public void drawLight(){
    }
  }
}
