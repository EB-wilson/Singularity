package singularity.ui.dialogs;

import arc.Core;
import arc.func.*;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.actions.Actions;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.TextButton;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import singularity.Sgl;
import universecore.util.Empties;

public class ModConfigDialog extends BaseDialog{
  Table settings;
  Table hover;
  OrderedMap<String, Seq<ConfigLayout>> entries = new OrderedMap<>();
  ObjectMap<String, Drawable> icons = new ObjectMap<>();

  String currCat;
  Table catTable;
  int currIndex;

  static int cfgCount = 0;

  public ModConfigDialog(){
    super("");
    titleTable.clear();

    addCloseButton();
    hidden(() -> Sgl.config.save());

    resized(this::rebuild);
    shown(this::rebuild);
  }

  public void rebuild(){
    clearHover();

    cont.clearChildren();
    cont.table(Tex.pane, main -> {
      main.table(cats -> {
        if(Scl.scl(entries.size*280) > Core.graphics.getWidth()*0.85f){
          Runnable rebuild = () -> {
            currCat = entries.orderedKeys().get(currIndex);

            catTable.clearActions();
            catTable.actions(
                Actions.alpha(0, 0.5f),
                Actions.run(() -> {
                  catTable.clearChildren();
                  catTable.image(icons.get(currCat, Core.atlas.drawable("settings_" + currCat))).size(38);
                  catTable.add(Core.bundle.get("settings.category." + currCat));
                }),
                Actions.alpha(1, 0.5f));
          };

          cats.button(Icon.leftOpen, Styles.clearNonei, () -> {
            currIndex = Mathf.mod(currIndex - 1, entries.size);
            rebuild.run();
            settings.clearActions();
            settings.actions(
                Actions.alpha(0, 0.5f),
                Actions.run(this::rebuildSettings),
                Actions.alpha(1, 0.5f)
            );
          }).size(60).padLeft(12);
          cats.table(Tex.underline, t -> catTable = t).height(60).growX().padLeft(4).padRight(4);
          cats.button(Icon.rightOpen, Styles.clearNonei, () -> {
            currIndex = Mathf.mod(currIndex + 1, entries.size);
            rebuild.run();
            settings.clearActions();
            settings.actions(
                Actions.alpha(0, 0.5f),
                Actions.run(this::rebuildSettings),
                Actions.alpha(1, 0.5f)
            );
          }).size(60).padRight(12);

          rebuild.run();
        }
        else{
          cats.defaults().height(60).growX().padLeft(2).padRight(2);
          for(String key: entries.keys()){
            cats.button(
                Core.bundle.get("settings.category." + key),
                icons.get(key, Core.atlas.drawable("settings_" + key)),
                new TextButton.TextButtonStyle(){{
                  font = Fonts.def;
                  fontColor = Color.white;
                  disabledFontColor = Color.lightGray;
                  down = Styles.flatOver;
                  checked = Styles.flatOver;
                  up = Tex.underline;
                  over = Tex.underlineOver;
                  disabled = Tex.underlineDisabled;
                }},
                () -> {
                  currCat = key;
                  settings.clearActions();
                  settings.actions(
                      Actions.alpha(0, 0.5f),
                      Actions.run(this::rebuildSettings),
                      Actions.alpha(1, 0.5f)
                  );
                }
            ).update(b -> b.setChecked(key.equals(currCat)));
          }
        }
      }).growX().fillY();
      main.row();
      main.image().color(Color.gray).height(4).growX().pad(-6).padTop(4).padBottom(4);
      main.row();
      main.top().pane(pane -> {
        pane.top().table(settings -> {
          settings.defaults().top().growX().height(50);
          this.settings = settings;
        }).growX().top();

        hover = new Table(Tex.pane);
        hover.visible = false;
        pane.addChild(hover);
      }).growX().fillY().top().get().setScrollingDisabledX(true);
    }).grow().pad(4).padLeft(12).padRight(12);

    rebuildSettings();
  }

  void rebuildSettings(){
    if(currCat == null){
      currCat = entries.orderedKeys().first();
    }

    settings.clearChildren();
    cfgCount = 0;
    for(ConfigLayout entry: entries.get(currCat)){
      cfgCount++;
      settings.table(((TextureRegionDrawable)Tex.whiteui).tint(Pal.darkestGray.cpy().a(cfgCount % 2)), ent -> {
        ent.setClip(false);
        ent.defaults().growY();
        entry.build(ent);
      });
      settings.row();
    }
  }

  public void addConfig(String category, ConfigLayout... config){
    entries.get(category, Seq::new).addAll(config);
    if(category.equals(currCat)) rebuildSettings();
  }

  public void addConfig(String category, Drawable icon, ConfigLayout... config){
    entries.get(category, Seq::new).addAll(config);
    icons.put(category, icon);
    if(category.equals(currCat)) rebuildSettings();
  }

  public void removeCfg(String category, String name){
    entries.get(category, Empties.nilSeq()).remove(e -> e.name.equals(name));
    if(category.equals(currCat)) rebuildSettings();
  }

  public void removeCat(String category){
    entries.remove(category);
    icons.remove(category);
  }

  public void clearHover(){
    if(hover == null) return;
    hover.clear();
    hover.visible = false;
  }

  public void setHover(Cons<Table> build){
    if(hover == null) return;

    clearHover();
    build.get(hover);
  }

  public static abstract class ConfigLayout{
    public final String name;

    public ConfigLayout(String name){
      this.name = name;
    }

    public abstract void build(Table table);
  }

  public static class ConfigSepLine extends ConfigLayout{
    String string;
    Color lineColor = Color.lightGray;

    public ConfigSepLine(String name, String str){
      super(name);
      this.string = str;
    }

    @Override
    public void build(Table table){
      table.left().add(string).fill().left().padBottom(0);
      table.row();
      table.image().color(lineColor).pad(-5).padBottom(4).height(4).growX();
    }
  }

  public static abstract class ConfigEntry extends ConfigLayout{
    public Prov<String> str;
    public Prov<String> tip;
    public Boolp disabled = () -> false;

    public ConfigEntry(String name){
      super(name);
      if(Core.bundle.has("settings.tip." + name)){
        tip = () -> Core.bundle.get("settings.tip." + name);
      }
    }

    @Override
    public void build(Table table){
      table.left().add(Core.bundle.get("settings.item." + name)).left().padLeft(4);
      table.right().table(t -> {
        t.setClip(false);
        t.right().defaults().right().padRight(0);
        if(str != null){
          t.add("").update(l -> {
            l.setText(str.get());
          });
        }
        buildCfg(t);
      }).growX().height(60).padRight(4);

      if(tip != null){
        table.addListener(new Tooltip(ta -> ta.add(tip.get()).update(l -> l.setText(tip.get()))));
      }
    }

    public abstract void buildCfg(Table table);
  }

  public static class ConfigButton extends ConfigEntry{
    Prov<Button> button;

    public ConfigButton(String name, Prov<Button> button){
      super(name);
      this.button = button;
    }

    @Override
    public void buildCfg(Table table){
      table.add(button.get()).width(180).growY().pad(4).get().setDisabled(disabled);
    }
  }

  public static class ConfigTable extends ConfigEntry{
    Cons<Table> table;
    Cons<Cell<Table>> handler;

    public ConfigTable(String name, Cons<Table> builder, Cons<Cell<Table>> handler){
      super(name);
      this.table = builder;
      this.handler = handler;
    }

    @Override
    public void buildCfg(Table table){
      handler.get(table.table(t -> {
        t.setClip(false);
        this.table.get(t);
      }));
    }
  }

  public static class ConfigCheck extends ConfigEntry{
    Boolp checked;
    Boolc click;

    public ConfigCheck(String name, Boolc click, Boolp checked){
      super(name);
      this.checked = checked;
      this.click = click;
    }

    @Override
    public void buildCfg(Table table){
      table.check("", checked.get(), click).update(c -> c.setChecked(checked.get())).get().setDisabled(disabled);
    }
  }

  public static class ConfigSlider extends ConfigEntry{
    Floatc slided;
    Floatp curr;
    float min, max, step;

    public ConfigSlider(String name, Floatc slided, Floatp curr, float min, float max, float step){
      super(name);
      this.slided = slided;
      this.curr = curr;
      this.min = min;
      this.max = max;
      this.step = step;
    }

    @Override
    public void buildCfg(Table table){
      if(str == null){
        table.add("").update(l -> {
          l.setText(Strings.autoFixed(curr.get(), 1));
        }).padRight(0);
      }
      table.slider(min, max, step, curr.get(), slided).width(360).padLeft(4).update(s -> {
        s.setValue(curr.get());
        s.setDisabled(disabled.get());
      });
    }
  }
}
