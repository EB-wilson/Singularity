package singularity.ui.dialogs;

import arc.Core;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.BaseDialog;
import singularity.Sgl;
import singularity.ui.SglStyles;

import static mindustry.Vars.ui;

public class AboutModDialog extends BaseDialog {
  BaseListDialog openUrl = new BaseListDialog(){{
    width = 490;
    itemBoardWidth = 180;
    height = 240;
    itemBoardHeight = 180;
    itemHeight = 45;
    
    build();
  }};
  
  Seq<BaseListDialog.ItemEntry> facebookPages = Seq.with(
      getEntry(Core.bundle.get("dialog.openUriDialog.facebook"), Sgl.facebook)
  );
  
  Seq<BaseListDialog.ItemEntry> telegramPages = Seq.with(
      getEntry(Core.bundle.get("dialog.openUriDialog.telegramPerson"), Sgl.telegram),
      getEntry(Core.bundle.get("dialog.openUriDialog.telegramGroup"), Sgl.telegramGroup)
  );
  
  Seq<BaseListDialog.ItemEntry> qqPages = Seq.with(
      getEntry(Core.bundle.get("dialog.openUriDialog.qqPerson"), Sgl.qq),
      getEntry(Core.bundle.get("dialog.openUriDialog.qqGroup1"), Sgl.qqGroup1),
      getEntry(Core.bundle.get("dialog.openUriDialog.qqGroup2"), Sgl.qqGroup2)
  );
  
  Cons<Seq<BaseListDialog.ItemEntry>> showUrl = items -> {
    openUrl.set(items);
    openUrl.rebuild();
    
    openUrl.show();
  };
  
  ButtonEntry[] modPages = new ButtonEntry[]{
      new ButtonEntry(Icon.githubSquare, t -> {
        t.add(Core.bundle.get("misc.github")).color(Pal.accent);
        t.row();
        t.add(Core.bundle.get("infos.openAddress"));
      }, () -> Pal.accent, () -> openUrl(Sgl.githubProject)),
      
      new ButtonEntry(Icon.discord, t -> {
        t.add(Core.bundle.get("misc.discord")).color(Pal.accent);
        t.row();
        t.add(Core.bundle.get("infos.discord"));
      }, () -> Color.valueOf("7289da"), () -> openUrl(Sgl.discord)),
  };
  
  ButtonEntry[] authorPages = new ButtonEntry[]{
      new ButtonEntry(Core.atlas.find(Sgl.modName + "-facebook"), t -> {
        t.add(Core.bundle.get("misc.facebook")).color(Pal.accent);
        t.row();
        t.add(Core.bundle.get("infos.facebook"));
      }, () -> Pal.items, () -> showUrl.get(facebookPages)),
      
      new ButtonEntry(Core.atlas.find(Sgl.modName + "-telegram"), t -> {
        t.add(Core.bundle.get("misc.telegram")).color(Pal.accent);
        t.row();
        t.add(Core.bundle.get("infos.telegram"));
      }, () -> Color.lightGray, () -> showUrl.get(telegramPages)),
      
      new ButtonEntry(Core.atlas.find(Sgl.modName + "-qq"), t -> {
        t.add(Core.bundle.get("misc.qq")).color(Pal.accent);
        t.row();
        t.add(Core.bundle.get("infos.qq"));
      }, () -> Pal.redderDust, () -> showUrl.get(qqPages)),
  };
  
  public AboutModDialog() {
    super(Core.bundle.get("dialog.aboutMod.title"));
    
    addCloseButton();
  }
  
  public void build(){
    cont.clearChildren();
    cont.defaults().width(710).fillY().top();
  
    cont.table(Tex.buttonTrans, t -> {
      t.defaults().left().padTop(5).growX().height(40);
      t.add(Core.bundle.get("mod.name")).color(Pal.accent);
      t.row();
      t.add(Core.bundle.get("misc.author")).color(Pal.accent);
      t.add(Core.bundle.get("mod.author"));
      t.row();
      t.add(Core.bundle.get("misc.version")).color(Pal.accent);
      t.add(Core.bundle.get("mod.version"));
      t.table(update -> {
        update.add(new Element(){
          @Override
          public void draw(){
            Draw.alpha(parentAlpha);
            Draw.color(Pal.accent);
            Fill.square(x + width/2, y + height/2, 8);
            Fill.square(x + width/2, y + height/2, 8, 45);
          }
        }).size(40);
        update.add(Core.bundle.get("infos.newestVersion"));
      });
      t.row();
      t.add(Core.bundle.get("infos.releaseDate")).color(Pal.accent);
      t.add(Core.bundle.get("mod.updateDate"));
      t.button(Core.bundle.get("infos.checkUpdate"), () -> {});
    }).width(580).fillY();
    
    cont.row();
    
    cont.table(t -> {
      t.defaults().growX().height(80).pad(0).padTop(10).margin(0);
      
      t.add(Core.bundle.get("infos.modPage")).color(Pal.accent).height(24);
      t.row();
      t.image().color(Pal.accent).width(740).height(4).pad(0).padTop(4);
      t.row();
      for(ButtonEntry item : modPages){
        t.table(Tex.underline, table -> {
          table.table(img -> {
            img.image().height(75).width(40f).update(i -> i.setColor(item.color.get()));
            img.row();
            img.image().height(5).width(40f).update(i -> i.setColor(item.color.get().cpy().mul(0.8f, 0.8f, 0.8f, 1f)));
          }).expandY();
  
          table.table(Tex.buttonEdge3, i -> i.image(item.region).size(32)).size(80);
          Table i = table.table().width(510).padLeft(10).get();
          i.defaults().growX().left();
          item.text.get(i);
  
          table.button(Icon.link, item.clicked).size(80).left().padLeft(12);
        }).width(710);
        
        t.row();
      }
      
      t.add(Core.bundle.get("infos.authorPage")).color(Pal.accent).height(24);
      t.row();
      t.image().color(Pal.accent).width(740).height(4).pad(0).padTop(4);
      t.row();
      for(ButtonEntry item : authorPages){
        t.button(table -> {
          table.table(img -> {
            img.image().height(75).width(40f).update(i -> i.setColor(item.color.get()));
            img.row();
            img.image().height(5).width(40f).update(i -> i.setColor(item.color.get().cpy().mul(0.8f, 0.8f, 0.8f, 1f)));
          }).expandY();
    
          table.table(Tex.buttonEdge3, i -> i.image(item.region).size(32)).size(80);
          Table i = table.table().width(590).padLeft(10).padRight(12).get();
          i.defaults().growX().left();
          item.text.get(i);
        }, SglStyles.underline, item.clicked).width(722);
        
        t.row();
      }
    });
  }
  
  private static void openUrl(String url){
    if(!Core.app.openURI(url)){
      ui.showErrorMessage("@linkfail");
      Core.app.setClipboardText(url);
    }
  }
  
  private BaseListDialog.ItemEntry getEntry(String title, String url){
    return new BaseListDialog.ItemEntry(t -> t.add(title), info -> {
      info.add(url).width(260).get().setWrap(true);
      openUrl.buttonTable.clearChildren();
      openUrl.buttonTable.table(t -> {
        t.button(Core.bundle.get("misc.copy"), () -> Core.app.setClipboardText(url));
        t.button(Core.bundle.get("misc.open"), () -> openUrl(url));
      });
    });
  }
  
  private static class ButtonEntry{
    TextureRegionDrawable region;
    Cons<Table> text;
    Prov<Color> color;
    
    Runnable clicked;
    
    public ButtonEntry(TextureRegionDrawable region, Cons<Table> text, Prov<Color> color, Runnable clicked){
      this.region = region;
      this.text = text;
      this.color = color;
      this.clicked = clicked;
    }
    
    public ButtonEntry(TextureRegion region, Cons<Table> text, Prov<Color> color, Runnable clicked){
      this(new TextureRegionDrawable(region), text, color, clicked);
    }
    
    public ButtonEntry(TextureRegionDrawable region, String text, Color color, Runnable clicked){
      this(region, t -> t.add(text), () -> color, clicked);
    }
    
    public ButtonEntry(TextureRegion region, String text, Color color, Runnable clicked){
      this(new TextureRegionDrawable(region), t -> t.add(text), () -> color, clicked);
    }
  }
}
