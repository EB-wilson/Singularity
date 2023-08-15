package singularity.ui.dialogs;

import arc.Core;
import arc.func.Boolc;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Dialog;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.core.UI;
import mindustry.entities.Units;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.type.PayloadStack;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.graphic.SglDrawConst;
import singularity.ui.SglStyles;
import singularity.world.blocks.product.SglUnitFactory;
import singularity.world.consumers.SglConsumeEnergy;
import singularity.world.consumers.SglConsumeType;
import singularity.world.consumers.SglConsumers;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.*;
import universecore.world.producers.BaseProducers;
import universecore.world.producers.ProduceType;

import java.text.Collator;
import java.util.Comparator;

import static mindustry.Vars.ui;

public class UnitFactoryCfgDialog extends BaseDialog {
  SglUnitFactory.SglUnitFactoryBuild currConfig;

  Table taskQueue, status, sideButton;
  Table commandCfg, commandCfgTab;
  boolean commandConfiguring;
  Vec2 configuringPos = new Vec2();
  SglUnitFactory.SglUnitFactoryBuild.BuildTask configCmdTask, curr;

  int pri;
  boolean fold;

  Dialog makeTask = new BaseDialog(Core.bundle.get("dialog.unitFactor.makeTask")) {
    static class Sorter{
      public final String name;
      public final Comparator<UnitType> sort;
      public final Prov<Drawable> icon;

      Sorter(String name, Comparator<UnitType> sort, Prov<Drawable> icon) {
        this.name = name;
        this.sort = sort;
        this.icon = icon;
      }
    }

    private final static Collator compare = Collator.getInstance(Core.bundle.getLocale());
    private final static Seq<Sorter> sorts = Seq.with(
        new Sorter("default", (t1, t2) -> 0, () -> Icon.list),
        new Sorter("name", (t1, t2) -> compare.compare(t2.localizedName, t1.localizedName), () -> Icon.edit),
        new Sorter("size", (t1, t2) -> Float.compare(t1.hitSize, t2.hitSize), () -> Icon.resize),
        new Sorter("health", (t1, t2) -> Float.compare(t1.health, t2.health), () -> Icon.add),
        new Sorter( "cost", (t1, t2) -> Float.compare(t1.getBuildTime(), t2.getBuildTime()), () -> Icon.hammer),
        new Sorter("strength", (t1, t2) -> Float.compare(t1.estimateDps(), t2.estimateDps()), () -> Icon.power)
    );

    private static final Seq<BaseProducers> tmpList = new Seq<>();

    String searching = "";
    boolean showLocking, reverse;
    Runnable rebuild;
    int sort = 0;

    {
      addCloseButton();

      cont.table(t -> {
        t.top().table(top -> {
          top.image(Icon.zoom).size(40);
          top.field("", tex -> {
            searching = tex;
            rebuild.run();
          }).growX();
          top.button(b -> b.image().size(38).scaling(Scaling.fit).update(i -> i.setDrawable(showLocking? Icon.lock: Icon.lockOpen)), Styles.clearNonei, () -> {
            showLocking = !showLocking;
            rebuild.run();
          }).size(50);
          top.add("").update(l -> l.setText(Core.bundle.format("dialog.unitFactor.sort", Core.bundle.get("dialog.unitFactor.sort_" + sorts.get(sort).name))));
          top.button(b -> b.image().size(38).scaling(Scaling.fit).update(i -> i.setDrawable(sorts.get(sort).icon.get())), Styles.clearNonei, () -> {
            sort = (sort + 1)%sorts.size;
            rebuild.run();
          }).size(50);
          top.button(b -> b.image().size(38).scaling(Scaling.fit).update(i -> i.setDrawable(reverse? Icon.up: Icon.down)), Styles.clearNonei, () -> {
            reverse = !reverse;
            rebuild.run();
          }).size(50);
          top.add("").update(l -> l.setText(Core.bundle.get(reverse? "dialog.unitFactor.reverse": "dialog.unitFactor.order"))).color(Pal.accent);
        }).growX().padLeft(120).padRight(120).fillY();

        t.row();
        t.pane(list -> rebuild = () -> {
          list.clearChildren();
          list.defaults().growX().fillY().pad(4);

          if (currConfig == null) return;

          tmpList.clear();
          tmpList.add(currConfig.block().producers().select(e ->
              e.cons.selectable.get() == BaseConsumers.Visibility.usable
              || (showLocking && e.cons.selectable.get() == BaseConsumers.Visibility.unusable)
          ));

          Comparator<UnitType> s = sorts.get(sort).sort;
          Comparator<UnitType> fs = reverse? s: (a, b) -> s.compare(b, a);
          tmpList.sort((a, b) -> {
            PayloadStack payloadsA = a.get(ProduceType.payload) == null ? null : a.get(ProduceType.payload).payloads[0];
            PayloadStack payloadsB = b.get(ProduceType.payload) == null ? null : b.get(ProduceType.payload).payloads[0];

            if (payloadsA == null || payloadsB == null) return 0;

            UnitType uA = (UnitType) payloadsA.item;
            UnitType uB = (UnitType) payloadsB.item;

            return fs.compare(uA, uB);
          });

          for (int i = 0; i < tmpList.size; i++) {
            BaseProducers prod = tmpList.get(i);
            BaseConsumers cons = prod.cons;

            PayloadStack[] payloads = prod.get(ProduceType.payload) == null ? null : prod.get(ProduceType.payload).payloads;
            if (payloads != null && payloads.length == 1 && payloads[0].item instanceof UnitType unit) {
              if (searching.trim().length() != 0 && !(unit.name.contains(searching.trim()) || unit.localizedName.contains(searching.trim())))
                continue;

              int index = currConfig.block().consumers.indexOf(cons);
              Button button = new Button() {
                {
                  setStyle(Styles.grayt);

                  clicked(() -> {
                    new BaseDialog("", SglStyles.transparentBack) {
                      int amount = 1;
                      Table tip;

                      {
                        addCloseButton();

                        Runnable rebuild = () -> {
                          cont.clearChildren();
                          cont.table(SglDrawConst.grayUI, t -> {
                            t.left().defaults().left().growX().pad(4);

                            t.top().table(top -> {
                              top.top().defaults().left();
                              top.image(unit.fullIcon).size(325).scaling(Scaling.fit).pad(6);

                              if (Core.graphics.isPortrait()) top.row();

                              top.table(info -> {
                                info.top().defaults().left().growX().pad(4);
                                info.add(unit.localizedName).color(Pal.accent);
                                info.row();
                                info.add(unit.description).wrap().color(Pal.lightishGray);
                                info.row();
                                info.table(req -> {
                                  req.left().defaults().left().fill().pad(4);
                                  req.add(Stat.buildCost.localized() + ":");
                                  ConsumeItems<?> ci = cons.get(ConsumeType.item);
                                  ConsumePower<?> cp = cons.get(ConsumeType.power);
                                  SglConsumeEnergy<?> cn = cons.get(SglConsumeType.energy);

                                  if (ci != null) {
                                    req.row();
                                    req.table(li -> {
                                      li.left().defaults().left().size(84f, 48);

                                      int i = 0;
                                      for (ItemStack stack : ci.consItems) {
                                        li.table(it -> {
                                          it.left().defaults().left();
                                          it.image(stack.item.fullIcon).scaling(Scaling.fit);
                                          it.add(UI.formatAmount(stack.amount)).padLeft(3);
                                        });
                                        i++;
                                        if (i%4 == 0) li.row();
                                      }
                                    });
                                  }

                                  for (BaseConsume<? extends ConsumerBuildComp> consume : cons.all()) {
                                    if (consume == ci || consume == cp || consume == cn) continue;
                                    req.row();
                                    req.table(ln -> {
                                      ln.left().defaults().left();
                                      consume.buildIcons(ln);
                                    });
                                  }

                                  if (cp != null) {
                                    req.row();
                                    req.add(Stat.powerUse.localized() + ": " + cp.usage*60 + StatUnit.perSecond.localized());
                                  }

                                  if (cn != null) {
                                    req.row();
                                    req.add(SglStat.consumeEnergy.localized() + ": " + cn.usage*60 + SglStatUnit.neutronFluxSecond.localized());
                                  }
                                });
                                info.row();
                                info.add(Stat.buildTime.localized() + ": " + timeFormat(cons.craftTime)).color(Color.gray);
                              }).grow().pad(4);
                            });

                            t.row();
                            t.image().color(Color.lightGray).height(4).pad(0).padTop(4).padBottom(4).growX();
                            t.row();
                            t.table(am -> {
                              am.defaults().padLeft(5).padRight(5);
                              am.button(Icon.up, Styles.cleari, () -> amount++).size(48).disabled(i -> amount >= Units.getCap(currConfig.team));
                              am.button(Icon.down, Styles.cleari, () -> amount--).size(48).disabled(i -> amount <= 1);
                              am.add("").update(l -> l.setText(Core.bundle.format("dialog.unitFactor.createAmount", amount))).growX();
                            });
                            t.row();
                            t.table(button -> {
                              button.defaults().height(48).pad(5);
                              button.button(Core.bundle.get("misc.details"), Icon.info, Styles.grayt, 32, () -> {
                                Vars.ui.content.show(unit);
                              }).growX();
                              button.button(Core.bundle.get("misc.add"), Icon.add, Styles.grayt, 32, () -> {
                                if (currConfig.taskCount() >= currConfig.block().maxTasks) {
                                  tip.clearActions();
                                  tip.actions(
                                      Actions.alpha(1, 0.3f),
                                      Actions.delay(1.5f),
                                      Actions.alpha(0, 0.8f)
                                  );
                                } else {
                                  currConfig.configure(IntSeq.with(1, unit.id, amount, index));
                                  rebuild(currConfig);

                                  hide();
                                }
                              }).growX();
                            });
                          }).fill();
                          cont.row();
                          tip = cont.table(SglDrawConst.grayUI, t -> t.add(Core.bundle.get("dialog.unitFactor.addFaid")).color(Color.red)).margin(6).fill().get();
                          tip.color.a = 0;
                        };

                        resized(rebuild);
                        shown(rebuild);
                      }
                    }.show();
                  });

                  left().defaults().left();
                  image(unit.uiIcon).size(80).scaling(Scaling.fit).pad(5);
                  table(inf -> {
                    inf.defaults().fill().left().pad(4);
                    inf.add(unit.localizedName).color(Pal.accent);
                    inf.row();
                    inf.table(req -> {
                      req.left().defaults().left().padRight(2);
                      req.add(Stat.buildCost.localized() + ":").padRight(4);
                      for (BaseConsume<? extends ConsumerBuildComp> consume : cons.all()) {
                        consume.buildIcons(req);
                      }
                    });
                    inf.row();
                    inf.add(Stat.buildTime.localized() + ": " + timeFormat(cons.craftTime)).color(Color.gray);
                  });
                }

                @Override
                public void draw() {
                  super.draw();

                  if (cons.selectable.get() == BaseConsumers.Visibility.unusable) {
                    Draw.color(Pal.darkerGray);
                    Draw.alpha(0.8f*parentAlpha);

                    Fill.rect(x + width/2, y + height/2, width, height);

                    Draw.color(Color.lightGray, parentAlpha);
                    Icon.lock.draw(x + width/2 - 16, y + height/2 + 8, 32, 32);

                    Draw.color(Color.gray, parentAlpha);

                    Fonts.outline.draw(Core.bundle.get("dialog.unitFactor.unresearch"), x + width/2, y + height/2 - 8, Tmp.c1.set(Pal.lightishGray).a(parentAlpha), 1, false, Align.center);
                  }
                }
              };
              list.add(button).disabled(b -> cons.selectable.get() != BaseConsumers.Visibility.usable);

              if (Core.graphics.isPortrait() || i%2 == 1) list.row();
            }
          }
        }).padLeft(60).padRight(60).growX().fillY().top();
      }).grow();

      resized(rebuild);

      shown(rebuild);
    }
  };

  public UnitFactoryCfgDialog() {
    super(Core.bundle.get("dialog.unitFactor.title"));

    Runnable rebuildButtons = () -> {
      buttons.clearChildren();
      buttons.defaults().reset();

      boolean portrait = Core.graphics.isPortrait();
      if (portrait) {
        buttons.defaults().size(480, 64).pad(4);
      } else buttons.defaults().size(210f, 64f).pad(3);

      Table ta = portrait ? buttons.table(t -> t.defaults().grow()).get() : buttons;
      ta.button("@back", Icon.left, Styles.grayt, this::hide).margin(6);
      addCloseListener();
      ta.button(Core.bundle.get("misc.add"), Icon.add, Styles.grayt, () -> {
        makeTask.show();
      }).margin(6);

      if (portrait) {
        buttons.row();
        ta = buttons.table(t -> t.defaults().grow()).get();
      }

      Cons<Table> play = tab -> tab.button("", new TextureRegionDrawable(Icon.play) {
        @Override
        public void draw(float x, float y, float width, float height) {
          if (currConfig != null && currConfig.activity) {
            Icon.cancel.draw(x, y, width, height);
          } else super.draw(x, y, width, height);
        }

        @Override
        public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
          if (currConfig != null && currConfig.activity) {
            Icon.cancel.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
          } else super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        }
      }, Styles.grayt, () -> {
        if (currConfig == null) return;
        currConfig.configure(IntSeq.with(5, currConfig.activity ? -1 : 1));
      }).update(t -> {
        if (currConfig == null) return;
        t.setText(Core.bundle.get(currConfig.activity ? "misc.stop" : "misc.execute"));
      }).margin(6);

      if (!portrait) play.get(ta);

      ta.button(Core.bundle.get("misc.import"), Icon.download, Styles.grayt, () -> {
        new BaseDialog("") {{
          setStyle(SglStyles.transparentBack);

          cont.table(SglDrawConst.grayUI, t -> {
            t.defaults().size(320, 58);
            t.add(Core.bundle.get("dialog.unitFactor.imports")).padBottom(12).center().labelAlign(Align.center);
            t.row();
            Boolc load = b -> {
              hide();
              try {
                currConfig.deserializeTask(Core.app.getClipboardText().replace("\r\n", "\n"), b);
                rebuild(currConfig);
              } catch (Throwable e) {
                ui.showException(e);
              }
            };
            t.button(Core.bundle.get("infos.override"), Icon.download, Styles.flatt, () -> load.get(false)).margin(6);
            t.row();
            t.button(Core.bundle.get("infos.append"), Icon.downOpen, Styles.flatt, () -> load.get(true)).margin(6);
            t.row();
            t.button(Core.bundle.get("misc.cancel"), Icon.cancel, Styles.flatt, this::hide).margin(6);
          }).fill().margin(8);
        }}.show();
      }).margin(6);

      ta.button(Core.bundle.get("misc.export"), Icon.upload, Styles.grayt, () -> {
        String str = currConfig.serializeTasks();
        Core.app.setClipboardText(str);
        Vars.ui.showInfoFade(Core.bundle.get("dialog.unitFactor.exported"), 3);
      }).margin(6);

      if (portrait) {
        buttons.row();
        play.get(buttons);
      }
    };

    resized(rebuildButtons);
    shown(rebuildButtons);
  }

  public void build() {
    ui.hudGroup.fill(t -> {
      t.add(new Element(){
        @Override
        public void draw() {
          if (!commandConfiguring) return;
          Vec2 viewPos = Core.camera.project(configuringPos.x, configuringPos.y);
          Draw.color(Pal.darkerGray);
          Lines.stroke(12);
          Lines.square(viewPos.x, viewPos.y, Scl.scl(28), 45);
          Draw.color(Pal.accent);
          Lines.stroke(6);
          Lines.square(viewPos.x, viewPos.y, Scl.scl(28), 45);

          float lerp = (Time.time%120f)/120f;
          Lines.stroke(6*(1 - lerp));
          Lines.square(viewPos.x, viewPos.y, Scl.scl(28 + 85*lerp), 45);
          super.draw();
        }
      });

      Table buttons = new Table(SglDrawConst.grayUI, tab -> {
        tab.table(cmds -> commandCfgTab = cmds).pad(5).fill();
        tab.button(Icon.ok, Styles.clearNonei, () -> {
          currConfig.configure(IntSeq.with(9, currConfig.indexOfTask(configCmdTask),
              (int) (configuringPos.x*1000), (int) (configuringPos.y*1000),
              UnitCommand.all.indexOf(configCmdTask.command)
          ));
          t.visible = false;
          configCmdTask = null;
          commandConfiguring = false;
          configuringPos.set(Float.MIN_VALUE, Float.MIN_VALUE);
          show();
        }).size(50).pad(5);
        tab.setTransform(true);
      });

      t.add(buttons);

      commandCfg = t;

      t.visible(() -> commandConfiguring && Vars.state.isGame());
      t.touchable = Touchable.enabled;

      Vec2 touchedPos = new Vec2();
      float[] time = {0};
      t.addListener(new InputListener(){
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
          Element hit = t.hit(x, y, true);
          if (Core.input.useKeyboard() || hit != null && hit != t) return false;

          float wx, wy;
          Vec2 v = Core.camera.unproject(x, y);
          wx = v.x;
          wy = v.y;

          touchedPos.set(wx, wy);
          time[0] = Time.time;

          return false;
        }
      });
      t.update(() -> {
        if (Core.input.useKeyboard()) return;

        Vec2 viewPos = Core.camera.project(configuringPos.x, configuringPos.y);
        buttons.setPosition(viewPos.x, viewPos.y - 38, Align.top);

        if (configCmdTask != null && !Core.input.isTouched() && Time.time - time[0] <= 30){
          configCmdTask.targetPos = new Vec2(touchedPos);
          configuringPos.set(touchedPos);

          time[0] = 0;
        }
      });
    });

    Runnable rebuildLayout = () -> {
      cont.clearChildren();
      cont.table(root -> {
        root.table(SglDrawConst.grayUI, pa -> {
          pa.top().pane(list -> taskQueue = list).growX().fillY().top();
        }).grow();

        if (Core.graphics.isPortrait()) root.row();

        Cell<Table> cell = root.table(SglDrawConst.grayUI, side -> {
          Table t = side;
          if (Core.graphics.isPortrait()) {
            side = new Table();
            Collapser coll = new Collapser(side, true);
            coll.setDuration(0.6f);

            t.add(coll).fillY().growX();
            t.row();
            t.button(Icon.up, Styles.clearNonei, 32, () -> {
              coll.setCollapsed(!coll.isCollapsed(), true);
            }).growX().height(40).update(i -> i.getStyle().imageUp = coll.isCollapsed() ? Icon.upOpen : Icon.downOpen);
          }
          side.top().pane(Styles.noBarPane, info -> status = info).grow().top().pad(4).get().setScrollingDisabledX(true);
          side = t;

          side.row();
          side.image().color(Pal.lightishGray).growX().pad(0).padBottom(4).height(4);
          side.row();
          side.table(buttons -> sideButton = buttons).growX().fillY().top().pad(4);
        });

        if (Core.graphics.isPortrait()) {
          cell.growX().fillY().padTop(6);
        } else cell.width(280).padLeft(6).growY();
      }).grow().padLeft(Core.graphics.isPortrait() ? 20 : 80).padRight(Core.graphics.isPortrait() ? 20 : 80);

      cont.row();
      cont.image().color(Color.darkGray).height(4).growX().pad(-1).padTop(3).padBottom(3);

      if (currConfig != null) rebuild(currConfig);
    };

    resized(true, rebuildLayout);
    shown(rebuildLayout);
  }

  private void rebuildCmds(SglUnitFactory.SglUnitFactoryBuild.BuildTask task){
    if (task != null) {
      commandCfgTab.clearChildren();
      for (UnitCommand command : task.buildUnit.commands) {
        commandCfgTab.button(Icon.icons.get(command.icon, Icon.cancel), Styles.clearNoneTogglei, () -> {
          task.command = command;
        }).checked(i -> task.command == command).size(50f).tooltip(command.localized());
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void rebuild(SglUnitFactory.SglUnitFactoryBuild factory) {
    taskQueue.clear();

    currConfig = factory;
    curr = factory.getCurrentTask();

    taskQueue.defaults().growX().left().pad(4);

    taskQueue.add(Core.bundle.get("dialog.unitFactor.executing")).color(Pal.accent);
    taskQueue.row();
    taskQueue.image().color(Pal.accent).height(3).pad(0).padBottom(4);
    taskQueue.row();

    buildTaskItem(taskQueue, factory.getCurrentTask(), true);

    taskQueue.add(Core.bundle.get("dialog.unitFactor.queue")).color(Pal.accent);
    taskQueue.row();
    taskQueue.image().color(Pal.accent).height(3).pad(0).padBottom(4);
    taskQueue.row();

    if (factory.getCurrentTask() != null) {
      for (SglUnitFactory.SglUnitFactoryBuild.BuildTask task : factory.getCurrentTask()) {
        if (task == factory.getCurrentTask()) continue;

        buildTaskItem(taskQueue, task, false);
      }
    }

    status.clearChildren();
    status.top().defaults().left().growX().pad(4).fillY();
    status.add("").update(l -> l.setText("> " + Core.bundle.format("dialog.unitFactor.status", factory.statusText())));
    status.row();
    status.add("").update(l -> l.setText(Core.bundle.format("dialog.unitFactor.taskRemaining", factory.taskCount(), factory.block().maxTasks)));
    status.row();
    status.add("").update(l -> {
      float time = 0;

      if (factory.getCurrentTask() != null && factory.getCurrentTask().factoryIndex != -1) {
        SglConsumers cons = (SglConsumers) factory.block().consumers.get(factory.getCurrentTask().factoryIndex);
        time += factory.getCurrentTask().queueAmount*cons.craftTime - (factory.progress() + factory.buildCount())*cons.craftTime;

        for (SglUnitFactory.SglUnitFactoryBuild.BuildTask task : factory.getCurrentTask()) {
          if (task == factory.getCurrentTask()) continue;

          cons = (SglConsumers) factory.block().consumers.get(task.factoryIndex);
          time += cons.craftTime*task.queueAmount;
        }
      }

      l.setText(Core.bundle.format("dialog.unitFactor.timeRemaining", factory.queueMode ? Core.bundle.get("misc.loop") : timeFormat(time/Math.max(factory.workEfficiency(), 0.00001f))));
    });
    status.row();
    status.add("").update(l -> l.setText(Core.bundle.format("dialog.unitFactor.matrixNetLinking", Core.bundle.get(
        factory.distributor.network.getCore() == null ? "infos.offline" :
        factory.distributor.network.netValid() ? "infos.connected" : "infos.netInvalid"
    ))));
    status.row();
    pri = factory.priority;
    status.table(t -> {
      t.defaults().left();
      t.add(Core.bundle.get("misc.priority"));
      t.field(String.valueOf(pri), TextField.TextFieldFilter.digitsOnly, num -> pri = Integer.parseInt(num)).growX();
    });
    status.row();
    status.button(Core.bundle.get("misc.sure"), Icon.ok, Styles.grayt, () -> factory.configure(IntSeq.with(2, pri))).height(38).growX()
        .disabled(b -> pri == factory.priority).update(b -> {
          if (pri != factory.priority) {
            if (fold) {
              b.clearActions();
              b.actions(Actions.alpha(1, 0.5f));
              fold = false;
            }
          } else {
            if (!fold) {
              b.clearActions();
              b.actions(Actions.alpha(0, 0.5f));
              fold = true;
            }
          }
        }).margin(6).get().color.a = 0;
    if (factory.block().hasPower) {
      status.row();
      status.add("").update(l -> {
        float pow = factory.power.graph.getPowerBalance()*60;
        l.setText(Core.bundle.format("dialog.unitFactor.power",
            UI.formatAmount((long) factory.power.graph.getBatteryStored()),
            UI.formatAmount((long) factory.power.graph.getBatteryCapacity()),
            (pow > 0 ? "[accent]+" : "[red]-") + UI.formatAmount((long) pow),
            UI.formatAmount((long) (factory.consumer == null || factory.consumer.current == null ? 0 : factory.consumer.getPowerUsage()*60))
        ));
      });
    }
    if (factory.block().hasEnergy) {
      status.row();
      status.add("").update(l -> {
        SglConsumeEnergy cons = factory.consumer.current == null ? null : factory.consumer.current.get(SglConsumeType.energy);
        l.setText(Core.bundle.format("dialog.unitFactor.energy", cons == null ? "0": Strings.autoFixed(cons.usage*cons.multiple(factory)*60, 1)));
      });
      status.row();
      status.add(new Bar(
          () -> Core.bundle.format("fragment.bars.nuclearContain", factory.getEnergy()),
          () -> SglDrawConst.matrixNetDark,
          () -> factory.getEnergy()/factory.block().energyCapacity()
      )).height(24);
    }
    status.row();
    status.add(new Bar(
        () -> Core.bundle.format("bar.efficiency", Mathf.round(factory.workEfficiency()*100)),
        () -> Pal.lightOrange,
        factory::workEfficiency
    )).height(24);
    status.row();
    status.add(Core.bundle.get("infos.storage"));
    status.row();
    status.table(req -> {
      Interval timer = new Interval();
      Runnable rebuild = () -> {
        req.clearChildren();
        req.left().defaults().size(85f, 48).left();
        int[] i = {0};
        int mod = Core.graphics.isPortrait() ? 5 : 3;
        factory.items.each((item, a) -> {
          req.table(it -> {
            it.left().defaults().left();
            it.image(item.fullIcon).scaling(Scaling.fit);
            it.add("").padLeft(3).update(l -> l.setText(UI.formatAmount(factory.items.get(item))));
          });
          i[0]++;
          if (i[0]%mod == 0) req.row();
        });
      };

      req.update(() -> {
        if (timer.get(30)) rebuild.run();
      });
    }).fillY();

    sideButton.clearChildren();
    sideButton.left().defaults().left().growX().pad(4);
    sideButton.button(Core.bundle.get("misc.clear"), Icon.trash, Styles.grayt, () -> {
      float delays = 0;
      for (Element child: taskQueue.getChildren()) {
        if (child instanceof Table) {
          child.clearActions();
          child.actions(
              Actions.delay(delays),
              Actions.parallel(
                  Actions.moveBy(-180, 0, 0.6f, Interp.pow2Out),
                  Actions.alpha(0, 0.5f)
              )
          );
          delays += 0.1f;
        }
      }
      Time.run(delays*60 + 36f, () -> {
        factory.configure(IntSeq.with(0));
        rebuild(factory);
      });
    }).margin(6).disabled(b -> factory.getCurrentTask() == null);
    sideButton.row();
    sideButton.check("", factory.queueMode, b -> factory.configure(IntSeq.with(3, b ? 1 : -1)))
        .update(l -> l.setText(Core.bundle.get(factory.queueMode ? "dialog.unitFactor.queueMode" : "dialog.unitFactor.stackMode"))).get().left();
    sideButton.row();
    sideButton.check(Core.bundle.get("dialog.unitFactor.skipBlocked"), factory.skipBlockedTask, b -> factory.configure(IntSeq.with(4, b ? 1 : -1))).get().left();
  }

  public void show(SglUnitFactory.SglUnitFactoryBuild factory) {
    rebuild(factory);
    show();
  }

  private void buildTaskItem(Table table, SglUnitFactory.SglUnitFactoryBuild.BuildTask task, boolean executing) {
    Table[] mark = new Table[1];
    mark[0] = table.table(SglDrawConst.grayUI, ta -> {
      ta.defaults().left().pad(6);
      if (task == null) {
        ta.add("no task executing!").center().height(80);
        return;
      }
      SglConsumers cons = (SglConsumers) currConfig.block().consumers.get(task.factoryIndex);

      ta.image(task.buildUnit.uiIcon).size(80).scaling(Scaling.fit);
      ta.table(tab -> {
        tab.table(inf -> {
          inf.add(task.buildUnit.localizedName).color(Pal.accent).left();
          inf.add("").update(t -> t.setText(Core.bundle.format("dialog.unitFactor.movePos",
              task.targetPos == null? "--": Mathf.round(task.targetPos.x),
              task.targetPos == null? "--": Mathf.round(task.targetPos.y)
          ))).color(Color.gray).left().growX().padLeft(4);
          inf.table(top -> {
            if (executing) {
              top.add("").left().update(l -> l.setText(Core.bundle.format("dialog.unitFactor.executed", currConfig.buildCount(), task.queueAmount)));
            } else top.add(Core.bundle.format("dialog.unitFactor.createAmount", task.queueAmount)).left();

            top.table(button -> {
              button.defaults().size(42);
              button.button(Icon.upOpen, Styles.clearNonei, 28, () -> {
                currConfig.configure(IntSeq.with(7, currConfig.indexOfTask(task)));
                rebuild(currConfig);
              }).disabled(b -> task.pre == null || (currConfig.activity && task.pre == currConfig.getCurrentTask()));
              button.button(Icon.downOpen, Styles.clearNonei, 28, () -> {
                currConfig.configure(IntSeq.with(8, currConfig.indexOfTask(task)));
                rebuild(currConfig);
              }).disabled(b -> task.next == null || (currConfig.activity && task == currConfig.getCurrentTask()));
              button.button(Icon.settings, Styles.clearNonei, 28, () -> {
                hide();
                configuringPos.set(currConfig.x, currConfig.y);
                configCmdTask = task;
                commandConfiguring = true;
                commandCfg.visible = true;

                rebuildCmds(task);
              });
              button.button(Icon.cancel, Styles.clearNonei, 28, () -> {
                if (executing) {
                  currConfig.configure(IntSeq.with(6, currConfig.indexOfTask(task)));
                } else {
                  mark[0].clearActions();
                  mark[0].actions(
                      Actions.parallel(
                          Actions.moveBy(-180, 0, 0.6f, Interp.pow2Out),
                          Actions.alpha(0, 0.5f)
                      ),
                      Actions.run(() -> {
                        currConfig.removeTask(task);
                        rebuild(currConfig);
                      })
                  );
                }
              });
            }).padLeft(4).padTop(-4).padRight(-4);
          }).right().fillX();
        }).growX();

        tab.row();

        tab.table(req -> {
          req.table(r -> {
            r.defaults().left();
            req.add(Stat.buildCost.localized() + ":").padRight(4);
            for (BaseConsume<? extends ConsumerBuildComp> consume : cons.all()) {
              consume.buildIcons(req);
            }
          }).left().growX();

          req.add(Core.bundle.get("misc.command") + ":");
          req.image().update(i -> i.setDrawable(Icon.icons.get(task.command.icon, Icon.cancel))).scaling(Scaling.fit).size(38);
          req.add("").update(l -> l.setText(task.command.localized())).color(Color.gray).padRight(4);

          if (executing) {
            req.add("").right().update(l -> l.setText(timeFormat((task.queueAmount*cons.craftTime - cons.craftTime*(currConfig.buildCount() + currConfig.progress()))/Math.max(currConfig.workEfficiency(), 0.00001f))));
          } else req.add(timeFormat(cons.craftTime*task.queueAmount)).right();
        }).left().growX();

        tab.row();

        tab.add(new Bar(
            executing ? () -> Core.bundle.format("bar.numprogress", Strings.autoFixed(currConfig.progress()*100, 2)) : () -> Core.bundle.get("infos.waiting"),
            () -> Pal.powerBar,
            executing ? currConfig::progress : () -> 0
        )).height(25).growX();
      }).growX().padBottom(4).padTop(4);
    }).fillY().growX().get();
    table.row();

    if (executing) {
      mark[0].update(() -> {
        if (currConfig.getCurrentTask() != curr) {
          curr = currConfig.getCurrentTask();

          mark[0].clearActions();
          mark[0].actions(
              Actions.parallel(
                  Actions.moveBy(-180, 0, 0.6f, Interp.pow2Out),
                  Actions.alpha(0, 0.5f)
              ),
              Actions.run(() -> rebuild(currConfig))
          );
        }
      });
    }
  }

  public static String timeFormat(float ticks) {
    int hor = (int) (ticks/216000);
    if (hor > 99) return "xx:xx:xx";

    ticks %= 216000;
    int min = (int) (ticks/3600);
    ticks %= 3600;
    int sec = (int) (ticks/60);

    StringBuilder builder = new StringBuilder();
    if (hor < 10) builder.append("0");
    builder.append(hor).append(":");
    if (min < 10) builder.append("0");
    builder.append(min).append(":");
    if (sec < 10) builder.append("0");
    builder.append(sec);

    return builder.toString();
  }
}
