package singularity.ui.fragments.override;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.WindowedMean;
import arc.math.geom.Vec3;
import arc.scene.Group;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Align;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.core.Version;
import mindustry.game.EventType;
import mindustry.game.Universe;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.graphics.g3d.PlanetRenderer;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.fragments.MenuFragment;
import singularity.Sgl;
import singularity.Singularity;
import singularity.core.UpdatePool;
import singularity.graphic.renders.SglPlanetRender;
import universecore.util.handler.FieldHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static mindustry.Vars.*;

public class SglMenuFrag extends MenuFragment{
  private final PlanetParams params = new PlanetParams();
  private final Vec3 camRight = new Vec3(1, 0, 0);
  
  private final PlanetRenderer renderer = new SglPlanetRender();
  
  private Universe universe = new Universe();
  private final Universe varsUniverse = Vars.universe;
  
  WindowedMean dxMean = new WindowedMean(10), dyMean = new WindowedMean(10);
  float lastDx, lastDy;
  boolean controlling, shown = true, inMenu, planetShown;
  
  @Override
  public void build(Group parent){
    params.planet = Planets.sun;
    params.uiAlpha = 0;
    params.camPos.set(10, 0, 15);
    
    parent.clear();
    Core.scene.root.removeChild(parent);
    
    WidgetGroup group = new WidgetGroup();
    group.setFillParent(true);
    group.touchable = Touchable.childrenOnly;
    group.visible(() -> state.isMenu());
    
    ui.menuGroup = group;
    Core.scene.add(group);
  
    UpdatePool.receive("menuUpdate", () -> {
      if(state.isMenu() != inMenu){
        Vars.universe = state.isMenu()? universe: varsUniverse;
        inMenu = state.isMenu();
      }
      
      if(state.isMenu() && !ui.planet.isShown()){
        for(int i = 0; i < 24; i++){
          universe.update();
        }
      }
      
      if(planetShown != ui.planet.isShown()){
        Vars.universe = ui.planet.isShown()? varsUniverse: universe;
        planetShown = ui.planet.isShown();
      }
    });
  
    group.fill((x, y, w, h) -> renderer.render(params));
  
    group.fill(t -> {
      t.touchable = Touchable.enabled;
      
      t.update(() -> {
        float dx = lastDx/9*Time.delta, dy = lastDy/9*Time.delta;
        if(!controlling){
          params.camDir.rotate(camRight, dy);
          params.camUp.rotate(camRight, dy);
  
          params.camDir.rotate(params.camUp, dx);
          camRight.rotate(params.camUp, dx);
          lastDx = Mathf.lerpDelta(lastDx, 0, 0.035f);
          lastDy = Mathf.lerpDelta(lastDy, 0, 0.035f);
        }
  
        
      });
      
      t.addListener(new InputListener(){
        @Override
        public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
          return super.scrolled(event, x, y, amountX, amountY);
        }
      });
      
      t.addCaptureListener(new ElementGestureListener(){
        @Override
        public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
          controlling = true;
          Tmp.v34.set(params.camPos);
          
          super.touchDown(event, x, y, pointer, button);
        }
  
        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
          controlling = false;
          lastDx = dxMean.rawMean();
          lastDy = dyMean.rawMean();
          
          super.touchUp(event, x, y, pointer, button);
        }
  
        @Override
        public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
          params.camDir.rotate(camRight, deltaY/9);
          params.camUp.rotate(camRight, deltaY/9);
  
          params.camDir.rotate(params.camUp, deltaX/9);
          camRight.rotate(params.camUp, deltaX/9);
          
          dxMean.add(deltaX);
          dyMean.add(deltaY);
          super.pan(event, x, y, deltaX, deltaY);
        }
  
        @Override
        public void zoom(InputEvent event, float initialDistance, float distance){
          params.camPos.set(
              Tmp.v34.cpy().add(
                  params.camDir.cpy().setLength((distance - initialDistance)/60).scl(distance > initialDistance? 1: -1)));
          
          super.zoom(event, initialDistance, distance);
        }
      });
    });
  
    group.fill(c -> {
      c.visibility = () -> shown;
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
      group.fill(c -> c.top().left().table(Tex.buttonEdge4, t -> {
        t.touchable = Touchable.enabled;
        t.image().update(i -> i.setDrawable(shown? Icon.eye: Icon.eyeOff));
        t.clicked(() -> shown = !shown);
      }).size(84, 45).name("shown"));
      group.fill(c -> c.bottom().left().button("", Styles.infot, ui.about::show).size(84, 45).name("info"));
      group.fill(c -> c.bottom().right().button("", Styles.discordt, ui.discord::show).size(84, 45).name("discord"));
    }else if(becontrol.active()){
      group.fill(c -> c.bottom().right().button("@be.check", Icon.refresh, () -> {
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
  
    String versionText = ((Version.build == -1) ? "[#fc8140aa]" : "[#ffffffba]") + Version.combined(),
        modVersionText = "UniverseCore:" + Sgl.libVersion + " Singularity:" + Sgl.modVersion;
    
    group.fill((x, y, w, h) -> {
      if(shown){
        TextureRegion logo = Singularity.getModAtlas("logo");
        float width = Core.graphics.getWidth(), height = Core.graphics.getHeight() - Core.scene.marginTop;
        float logoscl = Scl.scl(1);
        float logow = Math.min(logo.width*logoscl, Core.graphics.getWidth() - Scl.scl(20));
        float logoh = logow*(float) logo.height/logo.width;
  
        float fx = (int) (width/2f);
        float fy = (int) (height - 6 - logoh) + logoh/2 - (Core.graphics.isPortrait() ? Scl.scl(30f) : 0f);
  
        Draw.color();
        Draw.rect(logo, fx, fy, logow, logoh);
  
        Fonts.def.setColor(Color.white);
        GlyphLayout layout = Fonts.def.draw(modVersionText, fx, fy - logoh/2f, Align.center);
        Fonts.def.setColor(Color.white);
        Fonts.def.draw(versionText, fx, fy - logoh/2f - layout.height - 6f, Align.center);
      }
    });
  
    group.fill(t -> {
      t.visibility = () -> shown;
      //所以我选择在游戏logo上盖个透明的按钮(反正也能按)
      Image button = new Image(Singularity.getModAtlas("transparent"));
      button.clicked(Sgl.ui.mainMenu::show);
      t.top().add(button).size(940, 270);
    });
  }
}
