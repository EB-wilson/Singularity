package singularity.ui.dialogs.override;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.core.UI;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.PlanetDialog;
import singularity.Sgl;
import singularity.ui.tables.GasValue;
import singularity.world.atmosphere.Atmosphere;
import singularity.world.atmosphere.AtmosphereSector;
import universeCore.util.handler.MethodHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SglPlanetsDialog extends PlanetDialog{
  private static final Element updater = new Element();
  
  Runnable rebuildButtons = () -> {};
  Cons<String> search = field -> {};
  Cons<Sector> rebuildAnimateCont = sector -> {};
  Cons<Planet> rebuildSector = planet -> {};
  
  Sector lastSector;
  Sector currentSector;
  
  boolean fold = true;
  float alpha = 1;
  float x = 100;
  float y = 60;
  float minX = 100;
  float minY = 60;
  float maxX = 500;
  float maxY = 390;
  
  float maxSectorBarX = 232;
  
  float sectBarWidth = 0;
  
  int state;
  
  Cell<Table> sectCe;
  
  boolean[] sectState = {false, false, false};
  boolean lookTo;
  Sector sectLast;
  
  @Override
  public SglPlanetsDialog show(){
    super.show();
    hidden(() -> {
      sectBarWidth = 0;
      x = minX;
      y = minY;
      alpha = 1;
      fold = true;
      if(sectCe != null) sectCe.clearElement();
      sectState = new boolean[]{false, false, false};
      currentSector = null;
      sectLast = null;
    });
    
    tapped(() -> lookTo = false);
    
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
  
      Table animateCont = new Table();
      animateCont.defaults().grow();
  
      Table sectorsBar = new Table();
      
      Table sectorList = new Table();
      sectorList.table(table -> {
        table.defaults().growX().pad(0).padTop(4).left().top();
        table.add(Core.bundle.get("fragment.atmosphere.sectorsList")).height(30);
        table.row();
        table.button(t -> t.add("").grow().left().update(l -> l.setText(Core.bundle.get("misc.current") + ":" + (selected == null? "": selected.name()))), Styles.underlineb,
          () -> sectState[0] = true).update(b -> {
            b.setChecked(sectState[0]);
          }).height(40);
        table.row();
        table.image().color(Pal.gray).height(4).growX().padBottom(0).padTop(8);
        table.row();
        table.table(s -> {
          s.image(Icon.zoom).size(40);
          s.field("", te -> search.get(te)).width(172).get();
        }).height(40).pad(5);
        table.row();
        table.pane(pane -> {
          search = str -> {
            pane.clearChildren();
            Seq<Sector> all = planets.planet.sectors.select(Sector::hasBase);
            for(Sector sec : all){
              if(sec.hasBase() && (str.isEmpty() || sec.name().toLowerCase().contains(str.toLowerCase()))){
                pane.button(t -> {
                    t.add(sec.name()).width(160).left();
                    t.table(tip -> {
                      if(Sgl.atmospheres.getByPlanet(sec.planet).analyzed(sec)){
                        tip.image(Icon.ok).color(Color.green);
                      }
                      else{
                        tip.image(Icon.warningSmall).update(i -> {
                          i.color.set(Pal.accent).lerp(Pal.remove, Mathf.absin(Time.globalTime, 9f, 1f));
                        }).padRight(4f)
                          .get().addListener(new Tooltip(lab -> lab.background(Tex.buttonTrans).add(Core.bundle.get("fragment.atmosphere.unAnalyze"))));
                      }
                    }).right();
                  }, Styles.underlineb, () -> {
                  sectState[0] = false;
                  currentSector = sec;
                  selected = currentSector;
                }).update(b -> b.setChecked(currentSector == sec && !sectState[0])).height(40).growX();
                pane.row();
              }
            }
          };
          search.get("");
        }).grow();
        table.update(() -> {
          if(sectState[0]) currentSector = selected;
          if(currentSector != null && currentSector != sectLast){
            sectLast = currentSector;
  
            try{
              Method method = PlanetDialog.class.getDeclaredMethod("updateSelected");
              method.setAccessible(true);
              method.invoke(this);
            }catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
              Log.err(e);
            }
  
            lookTo = true;
            sectState[1] = true;
          }
          if(sectState[1]){
            if(!sectState[2] && alpha > 0){
              alpha -= 0.06;
            }
            else if(sectState[2]){
              if(alpha < 1){
                alpha += 0.06;
              }
              else{
                sectState[1] = false;
                sectState[2] = false;
              }
            }
            else{
              sectState[2] = true;
              rebuildAnimateCont.get(currentSector);
            }
          }
          if(lookTo) lookAt(currentSector, 0.1f);
        });
      });
  
      Table defaultTable = new Table();
      defaultTable.button(Core.bundle.get("fragment.atmosphere.infoButton"), () -> {
        fold = false;
        state = 1;
      }).grow();
  
      Table infoTable = new Table();
      infoTable.margin(4);
      infoTable.table(t -> {
        rebuildSector = planet -> {
          t.clearChildren();
          if(planet != null){
            Atmosphere atmo = Sgl.atmospheres.getByPlanet(planet);
            t.defaults().left().top().grow().padTop(4);
            t.add(Core.bundle.get("fragment.atmosphere.info")).padLeft(10).padTop(6);
            t.row();
            t.add(planet.localizedName).color(Pal.accent).padLeft(10).padBottom(16);
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
                table.add(new GasValue(gas, amount, true, false)).left().padLeft(0)
                  .get().addListener(new Tooltip(tip -> tip.background(Tex.buttonTrans).add(amount + "")));
                table.add(Core.bundle.format("fragment.atmosphere.gasAnalyzedDelta", (atmo.getAnalyzedDelta(gas) > 0? "+": "") + atmo.getAnalyzedDelta(gas)*3600)).padLeft(60f)
                  .get().addListener(new Tooltip(tip -> tip.background(Tex.buttonTrans).add((atmo.getAnalyzedDelta(gas) > 0? "+": "") + atmo.getAnalyzedDelta(gas)*60 + Core.bundle.get("misc.preSecond"))));
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
                    if(sectBarWidth < maxSectorBarX - 1){
                      sectBarWidth = Mathf.lerp(sectBarWidth, maxSectorBarX, 0.15f);
                      sectCe.width(sectBarWidth);
                    }
                    else if(trans[0]){
                      if(tableAlpha[0] < 1){
                        tableAlpha[0] += 0.06;
                        sectorList.setColor(sectorList.color.a(tableAlpha[0]));
                      }
                    }
                    else{
                      s.add(sectorList).grow().top().margin(4).color(sectorList.color.a(0)).colspan(3).width(sectBarWidth);
                      trans[0] = true;
                    }
                    s.setColor(s.color.a(sectBarWidth/maxSectorBarX));
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
                      s.setColor(s.color.a(sectBarWidth/maxSectorBarX));
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
      infoTable.button(Core.bundle.get("misc.fold"), () -> {
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
          s.setColor(s.color.a(sectBarWidth/maxSectorBarX));
        });
        fold = true;
        state = 1;
      }).size(100, 42).center().bottom().padBottom(0).padTop(6);
      
      rebuildAnimateCont = sector -> {
        animateCont.clearChildren();
        if(sector == null){
          animateCont.add(defaultTable).size(100, 60);
        }
        else{
          animateCont.table(t -> {
            Atmosphere atmo = Sgl.atmospheres.getByPlanet(sector.planet);
            AtmosphereSector atmoSect = atmo.getSector(sector);
            t.defaults().left().top().grow().padTop(4);
            t.add(Core.bundle.get("fragment.atmosphere.sectorInfo")).padLeft(10).padTop(6);
            t.row();
            t.add("[accent]" + sector.name() + "[gray](" + sector.planet.localizedName + ")").padLeft(10).padBottom(16);
            t.row();
            t.add("> " + Core.bundle.format("fragment.atmosphere.basePressure", Strings.autoFixed(atmo.getBasePressure()*100, 2),
              Strings.autoFixed(atmo.getCurrPressure()*100, 2))).padLeft(10).padTop(5).padBottom(5);
            t.row();
            t.image().color(Pal.gray).colspan(3).height(4).growX().padBottom(0);
            t.row();
            t.add(Core.bundle.get("fragment.atmosphere.baseDelta")).color(Pal.gray).padLeft(10);
            t.row();
            t.pane(table -> {
              table.defaults().pad(4);
              if(atmoSect.anyDisplay()){
                atmoSect.eachDisplay((gas, amount) -> {
                  table.add(new GasValue(gas, 0, true, false)).left().padLeft(0);
                  table.add((amount > 0? "[accent]+": "[]") + (amount*3600 > 1000 ? UI.formatAmount(((Number)(amount*3600)).longValue()) : Strings.autoFixed(amount*3600, 2)) + Core.bundle.get("misc.preMin")).padLeft(90f)
                    .get().addListener(new Tooltip(tip -> tip.background(Tex.buttonTrans).add(amount*60 + Core.bundle.get("misc.preSecond"))));
                  table.row();
                });
              }
              else{
                table.table(child -> {
                  child.add(Core.bundle.get("fragment.atmosphere.noneGasDelta")).color(Pal.accent);
                  child.row();
                  child.add(Core.bundle.get("fragment.atmosphere.deltaTestTip"));
                }).growX().fillY();
              }
            }).height(160);
            t.row();
  
            t.button(Core.bundle.get("fragment.atmosphere.sectorsClose1"), () -> {
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
                s.setColor(s.color.a(sectBarWidth/maxSectorBarX));
              });
              state = 1;
              fold = false;
              
              sectState = new boolean[]{false, false, false};
              currentSector = null;
              sectLast = null;
            }).growX().margin(0).padRight(8).padLeft(8).height(40);
            t.row();
            t.button(Core.bundle.get("misc.fold"), () -> {
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
                s.setColor(s.color.a(sectBarWidth/maxSectorBarX));
              });
              fold = true;
              state = 1;
  
              sectState = new boolean[]{false, false, false};
              currentSector = null;
              sectLast = null;
            }).size(100, 42).center().bottom().padBottom(0).padTop(6);
          }).size(x, y);
        }
      };
      rebuildAnimateCont.get(null);
  
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
    
        animateCont.setColor(defaultTable.color.a(alpha));
        boardCell.size(x, y);
      });
    });
    
    return this;
  }
}
