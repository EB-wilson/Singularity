package singularity.ui.fragments.override;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Align;
import arc.util.Log;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.graphics.MenuRenderer;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.fragments.MenuFragment;
import singularity.Sgl;
import singularity.Singularity;
import universeCore.util.handler.FieldHandler;
import universeCore.util.handler.MethodHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static mindustry.Vars.*;

public class SglMenuFrag extends MenuFragment{
  @Override
  public void build(Group parent){
    MenuRenderer renderer = new MenuRenderer();
  
    Group group = new WidgetGroup();
    group.setFillParent(true);
    group.visible(() -> !ui.editor.isShown());
    parent.addChild(group);
  
    parent = group;
  
    parent.fill((x, y, w, h) -> renderer.render());
  
    parent.fill(c -> {
      FieldHandler.setValue(MenuFragment.class, "container", this, c);
      c.name = "menu container";
      
      try{
        Method met = mobile ? MenuFragment.class.getDeclaredMethod("buildMobile"): MenuFragment.class.getDeclaredMethod("buildDesktop");
        met.setAccessible(true);
        met.invoke(this);
        Events.on(EventType.ResizeEvent.class, event -> {
          try{
            met.invoke(this);
          }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
          }
        });
      }
      catch(InvocationTargetException | IllegalAccessException | NoSuchMethodException e){
        Log.err(e);
      }
    });
  
    //info icon
    if(mobile){
      parent.fill(c -> c.bottom().left().button("", Styles.infot, ui.about::show).size(84, 45).name("info"));
      parent.fill(c -> c.bottom().right().button("", Styles.discordt, ui.discord::show).size(84, 45).name("discord"));
    }else if(becontrol.active()){
      parent.fill(c -> c.bottom().right().button("@be.check", Icon.refresh, () -> {
        ui.loadfrag.show();
        becontrol.checkUpdate(result -> {
          ui.loadfrag.hide();
          if(!result){
            ui.showInfo("@be.noupdates");
          }
        });
      }).size(200, 60).name("becheck").update(t -> {
        t.getLabel().setColor(becontrol.isUpdateAvailable() ? Tmp.c1.set(Color.white).lerp(Pal.accent, Mathf.absin(5f, 1f)) : Color.white);
      }));
    }
  
    String versionText = ((Version.build == -1) ? "[#fc8140aa]" : "[#ffffffba]") + Version.combined();
    parent.fill((x, y, w, h) -> {
      TextureRegion logo = Singularity.getModAtlas("logo");
      float width = Core.graphics.getWidth(), height = Core.graphics.getHeight() - Core.scene.marginTop;
      float logoscl = Scl.scl(1);
      float logow = Math.min(logo.width * logoscl, Core.graphics.getWidth() - Scl.scl(20));
      float logoh = logow * (float)logo.height / logo.width;
    
      float fx = (int)(width / 2f);
      float fy = (int)(height - 6 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? Scl.scl(30f) : 0f);
    
      Draw.color();
      Draw.rect(logo, fx, fy, logow, logoh);
    
      Fonts.def.setColor(Color.white);
      Fonts.def.draw(versionText, fx, fy - logoh/2f, Align.center);
    });
  
    parent.fill(t -> {
      //所以我选择在游戏logo上盖个透明的按钮(反正也能按)
      Image button = new Image(Singularity.getModAtlas("transparent"));
      button.clicked(Sgl.ui.mainMenu::show);
      t.top().add(button).size(940, 270);
    });
  }
}
