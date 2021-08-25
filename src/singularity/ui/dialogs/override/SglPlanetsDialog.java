package singularity.ui.dialogs.override;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Strings;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.ui.dialogs.PlanetDialog;
import singularity.Sgl;
import singularity.ui.tables.GasValue;
import singularity.world.atmosphere.Atmosphere;

import java.util.concurrent.atomic.AtomicReference;

public class SglPlanetsDialog extends PlanetDialog{
  private static final Element updater = new Element();
  
  Runnable rebuildButtons = () -> {};
  Cons<Planet> rebuildSector = planet -> {};
  
  Sector currentSector;
  Sector lastSector;
  
  boolean fold = true;
  float alpha = 1;
  float x = 100;
  float y = 60;
  float minX = 100;
  float minY = 60;
  float maxX = 500;
  float maxY = 370;
  
  float sectBarWidth = 0;
  
  int state;
  
  Cell<Table> sectCe;
  
  @Override
  public SglPlanetsDialog show(){
    super.show();
    hidden(() -> {
      x = minX;
      y = minY;
      alpha = 1;
      fold = true;
    });
    
    defaults().pad(0).margin(0);
    row();
    image().color(Pal.accent).height(4).growX().padBottom(4);
    row();
    table(main -> {
      Table board = new Table(Tex.pane);
      board.update(() -> {
        if(lastSector != selected){
          if(selected == null || lastSector == null || lastSector.planet != selected.planet) rebuildSector.get(selected == null? null: selected.planet);
          lastSector = selected;
        }
      });
  
      Table sectorsBar = new Table();
      
      Table sectorList = new Table();
      sectorList.table(table -> {
        table.defaults().growX().height(46).pad(4).left().top();
        
      }).grow();
  
      Table defaultTable = new Table();
      defaultTable.button("unfold", () -> {
        fold = false;
        state = 1;
      }).grow();
  
      Table infoTable = new Table();
      infoTable.margin(4);
      infoTable.table(t -> {
        t.defaults().left().top().grow().padTop(4);
        t.add(Core.bundle.get("fragment.atmosphere.info"));
        t.row();
        
        rebuildSector = planet -> {
          t.clearChildren();
          if(planet != null){
            Atmosphere atmo = Sgl.atmospheres.getByPlanet(planet);
            t.add(planet.localizedName).color(Pal.accent).height(42).padLeft(10);
            t.row();
            t.add("> " + Core.bundle.format("fragment.atmosphere.basePressure", Strings.autoFixed(atmo.getBasePressure()*100, 2),
              Strings.autoFixed(atmo.getCurrPressure()*100, 2))).padLeft(10).padTop(5).padBottom(5);
            t.row();
            t.image().color(Pal.gray).colspan(3).height(4).growX().padBottom(0);
            t.row();
            t.add(Core.bundle.get("fragment.atmosphere.ingredients")).color(Pal.gray).padLeft(10);
            t.row();
            t.pane(table -> {
              table.defaults().pad(4);
              atmo.each((gas, amount) -> {
                table.add(new GasValue(gas, amount, true, false)).left().padLeft(0);
                table.add(Core.bundle.format("fragment.atmosphere.gasDelta", (atmo.getDelta(gas) > 0? "+": "") + atmo.getDelta(gas)*3600)).padLeft(60f);
                table.row();
              });
            }).height(160);
            t.row();
            
            t.table(buttons -> {
              rebuildButtons = () -> {
                sectCe = null;
                boolean[] trans = {false};
                float[] tableAlpha = {0};
                buttons.clearChildren();
                buttons.button(Core.bundle.get("fragment.atmosphere.sectors"), () -> {
                  sectCe = sectorsBar.table(Tex.pane).height(y);
                  sectCe.update(s -> {
                    s.add(updater);
                    s.removeChild(updater);
                    if(sectBarWidth < 179){
                      sectBarWidth = Mathf.lerp(sectBarWidth, 180, 0.15f);
                      sectCe.width(sectBarWidth);
                    }
                    else if(trans[0]){
                      if(tableAlpha[0] < 1){
                        tableAlpha[0] += 0.6;
                        sectorList.setColor(sectorList.color.a(tableAlpha[0]));
                      }
                    }
                    else{
                      s.add(sectorList).grow().top().margin(0);
                      trans[0] = true;
                    }
                    s.setColor(s.color.a(sectBarWidth/180));
                  });
                  buttons.clearChildren();
                  buttons.button(Core.bundle.get("fragment.atmosphere.sectorsClose"), () -> {
                    sectCe.get().clear();
                    sectCe.update(s -> {
                      s.add(updater);
                      s.removeChild(updater);
                      if(sectBarWidth > 1){
                        sectBarWidth = Mathf.lerp(sectBarWidth, 0, 0.15f);
                        sectCe.width(sectBarWidth);
                      }
                      else{
                        s.clear();
                        sectorsBar.clear();
                        rebuildButtons.run();
                      }
                      s.setColor(s.color.a(sectBarWidth/180));
                    });
                  }).growX().margin(0).padRight(4).padLeft(4).height(40);
                }).growX().margin(0).padRight(4).padLeft(4).height(40).get();
              };
              
              rebuildButtons.run();
            });
          }
          else{
            t.table(strs -> {
              strs.add(Core.bundle.get("fragment.atmosphere.unselectPlanet")).color(Pal.accent);
              strs.row();
              strs.add(Core.bundle.get("fragment.atmosphere.selectInfo")).padTop(5);
            }).padLeft(10).growX().fillY();
          }
        };
    
        rebuildSector.get(selected == null? null: selected.planet);
      }).grow();
      infoTable.row();
      infoTable.button("fold", () -> {
        if(sectCe != null) sectCe.update(s -> {
          s.add(updater);
          s.removeChild(updater);
          if(sectBarWidth > 1){
            sectBarWidth = Mathf.lerp(sectBarWidth, 0, 0.15f);
            sectCe.width(sectBarWidth);
          }
          else{
            s.clear();
            sectorsBar.clear();
            rebuildButtons.run();
          }
          s.setColor(s.color.a(sectBarWidth/180));
        });
        fold = true;
        state = 1;
      }).size(100, 42).center().bottom().padBottom(0).padTop(6);
  
      Table animateCont = new Table();
      animateCont.defaults().grow();
      animateCont.add(defaultTable).size(100, 60);
  
      Cell<Table> animateCell = board.add(animateCont);
      Cell<Table> boardCell = main.add(board).bottom();
      Cell<Table> sectorsCell = main.add(sectorsBar).padLeft(0).width(0);
  
      animateCell.update(table -> {
        if(state == 1){
          if(alpha > 0){
            alpha -= 0.06;
          }
          else{
            state = 2;
            table.clearChildren();
          }
        }
        else if(state == 2){
          table.add(updater);
          table.removeChild(updater);
          if(!fold){
            if(x < maxX) x = Mathf.lerp(x, maxX, 0.15f);
            if(y < maxY) y = Mathf.lerp(y, maxY, 0.15f);
            if(x >= maxX - 1 && y >= maxY - 1){
              state = 3;
              table.add(infoTable).size(x, y);
            }
          }
          else{
            if(x > minX) x = Mathf.lerp(x, minX, 0.15f);
            if(y > minY) y = Mathf.lerp(y, minY, 0.15f);
            if(x <= minX + 1 && y <= minY + 1){
              state = 3;
              table.add(defaultTable).size(x, y);
            }
          }
          sectorsCell.size(0, y);
        }
        else if(state == 3){
          if(alpha < 1){
            alpha += 0.06;
          }
          else state = 0;
        }
    
        defaultTable.setColor(defaultTable.color.a(alpha));
        infoTable.setColor(infoTable.color.a(alpha));
        boardCell.size(x, y);
      });
    });
    
    return this;
  }
}
