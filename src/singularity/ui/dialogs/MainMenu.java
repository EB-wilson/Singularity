package singularity.ui.dialogs;

import arc.Core;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.ui.Label;
import mindustry.content.Liquids;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.WarningBar;
import mindustry.ui.dialogs.BaseDialog;
import singularity.Sgl;
import singularity.Singularity;
import singularity.ui.SglStyles;

public class MainMenu extends BaseDialog {
  protected boolean launch = true;
  
  ButtonEntry[] buttonEntries = new ButtonEntry[]{
      new ButtonEntry(Singularity.getModAtlas("icon_start"), () -> Core.bundle.get(launch? "misc.startGame": "misc.backToGame"), () -> Color.white, () -> {hide(); launch = false;}),
      new ButtonEntry(Singularity.getModAtlas("icon_database"), Core.bundle.get("misc.modDatabase"), Pal.accent, () -> {}),
      new ButtonEntry(Singularity.getModAtlas("icon_configure"), Core.bundle.get("misc.modConfigure"), Color.lightGray, () -> {}),
      new ButtonEntry(Singularity.getModAtlas("icon_publicInfo"), () -> Core.bundle.get("misc.publicInfo"), () -> Liquids.cryofluid.color, () -> {
        Sgl.ui.publicInfo.show();
      }),
      new ButtonEntry(Singularity.getModAtlas("icon_about"), Core.bundle.get("misc.aboutMod"), Color.violet, () -> Sgl.ui.aboutDialog.show()),
      new ButtonEntry(Singularity.getModAtlas("icon_contribute"), Core.bundle.get("misc.contribute"), Color.yellow, () -> {}),
  };
  
  public MainMenu() {
    super(Core.bundle.get("dialog.mainMenu.title"));
  }
  
  public void build() {
    cont.top().table(main -> {
      main.image(Singularity.getModAtlas("launch_logo")).size(256, 128).padTop(30);
      main.row();
      main.image().color(Pal.accent).growX().height(3).pad(0).padTop(4).padBottom(4);
      main.row();
      main.add(new WarningBar()).growX().height(16).color(Color.lightGray).padBottom(30);
      main.row();
      main.add("S  I  N  G  U  L  A  R  I  T  Y").get().setFontScale(2);
      main.row();
      main.add(new WarningBar()).growX().height(16).color(Color.lightGray).padTop(30);
      main.row();
      main.image().color(Pal.accent).growX().height(3).pad(0).padTop(4);
      main.row();
      main.table(menu -> {
        menu.defaults().pad(0).padTop(6).margin(0).width(680).height(80).top();
        for(ButtonEntry entry : buttonEntries){
          menu.button(b -> {
            b.table(Tex.buttonEdge3, i -> i.image(entry.region).size(64)).size(80);
            Label l = b.add("").width(510).padLeft(10).get();
            shown(() -> l.setText(entry.text.get()));
            
            b.add(new Element(){
              @Override
              public void draw(){
                Draw.color(entry.color.get().cpy().lerp(Color.black, 0.3f));
                Draw.alpha(parentAlpha);
                Fill.square(x + width/2, y + height/2 - 6, 12, 45);
                Draw.color(entry.color.get());
                Draw.alpha(parentAlpha);
                Fill.square(x + width/2, y + height/2, 12, 45);
              }
            }).size(80);
          }, SglStyles.underline, entry.clicked);
          menu.row();
        }
      }).width(680).fillY();
    }).growX().top().pad(0).margin(0);
    
    row();
    image().color(Color.white).growX().height(2).pad(0).padTop(4);
    row();
    table(t -> {
      t.add("Singularity OS v." + Core.bundle.get("mod.version")).left().padLeft(3);
      t.image().color(Color.white).growY().width(2).pad(0).margin(0).padLeft(4).colspan(4);
      t.add().growX();
      t.image().color(Color.white).width(2).growY().pad(0).margin(0).padRight(4).colspan(4);
      t.add("run with UniverseCore").right().padRight(3);
    }).growX();
  }
  
  private static class ButtonEntry{
    TextureRegion region;
    Prov<String> text;
    Prov<Color> color;
    
    Runnable clicked;
    
    public ButtonEntry(TextureRegion region, Prov<String> text, Prov<Color> color, Runnable clicked){
      this.region = region;
      this.text = text;
      this.color = color;
      this.clicked = clicked;
    }
    
    public ButtonEntry(TextureRegion region, String text, Color color, Runnable clicked){
      this(region, () -> text, () -> color, clicked);
    }
  }
}
