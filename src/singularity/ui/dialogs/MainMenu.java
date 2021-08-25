package singularity.ui.dialogs;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;
import singularity.Sgl;

import static singularity.ui.SglUI.*;

public class MainMenu extends BaseDialog {
  WorldOutlookDialog worldOutlook = new WorldOutlookDialog(Core.bundle.get("dialog.worldOutlook.title"));
  AboutModDialog aboutMod = new AboutModDialog(Core.bundle.get("dialog.aboutMod.title"));
  
  public MainMenu() {
    super(Core.bundle.get("dialog.mainMenu.title"));
    build();
  }

  public void build() {
    aboutMod.build();
    cont.clear();

    Table table = new Table();
    table.add(Core.bundle.get("dialog.mainMenu.text"));
    table.row();
    Table button = new Table(Tex.button);
    Table layout1 = new Table();
    layout1.top().button(Core.bundle.get("dialog.mainMenu.worldOutlook"), this::show1).size(215, 90);
    layout1.top().button(Core.bundle.get("dialog.mainMenu.dataBase"), this::show2).size(215, 90);
    layout1.top().button(Core.bundle.get("dialog.mainMenu.setting"), this::show3).size(215, 90);
    layout1.row();
    layout1.add("--------------------");
    layout1.image(Core.atlas.find("singularity-mod_icon")).size(215, 80);
    layout1.add("--------------------");
    layout1.row();
    layout1.button(Core.bundle.get("dialog.mainMenu.aboutMod"), this::show4).size(215, 90);
    layout1.button(Core.bundle.get("dialog.mainMenu.contact"), this::show5).size(215, 90);
    layout1.button(Core.bundle.get("dialog.mainMenu.contribute"), this::show6).size(215, 90);
    button.add(layout1).size(650, 270);
    button.row();
    button.add("[gray]" + Core.bundle.get("dialog.mainMenu.tip_1")).padTop(30).padLeft(20);
    table.add(button).size(650, 350);
    table.row();
    table.add("[red]" + Core.bundle.get("dialog.mainMenu.warning")).left();
    table.row();
    table.button(Core.bundle.get("misc.close"), this::hide).size(100, 60);

    cont.add(table);
  }

  private void show1(){
    worldOutlook.showDialog();
  }

  private void show2(){

  }

  private void show3(){

  }

  private void show4(){
    aboutMod.show();
  }

  private void show5(){
    Sgl.ui.contact.show();
  }

  private void show6(){

  }
}
