package singularity.world.draw;

import arc.func.Cons;
import arc.func.Func;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import arc.util.Structs;
import singularity.world.blockComp.DrawableComp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class SglDrawBase<Target extends DrawableComp>{
  public Cons<Target> drawDef;
  
  public Func<Target, ? extends SglBaseDrawer<Target>> drawerType;
  
  @SuppressWarnings("unchecked")
  public SglBaseDrawer<Target> get(DrawableComp entity){
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
          Constructor<? extends SglBaseDrawer<Target>> cstr = (Constructor<? extends SglBaseDrawer<Target>>) type.getDeclaredConstructors()[0];
          drawerType = ent -> {
            try{
              return cstr.newInstance(this, ent);
            }catch(InstantiationException | IllegalAccessException | InvocationTargetException e){
              Log.err(e);
              return new SglBaseDrawer<>(ent);
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
}
