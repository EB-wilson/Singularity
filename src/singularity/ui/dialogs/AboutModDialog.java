package singularity.ui.dialogs;

import singularity.Sgl;
import singularity.Singularity;
import arc.Core;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;

import static singularity.ui.SglUI.*;

public class AboutModDialog extends BaseDialog {
  public AboutModDialog(String title) {
    super(title);
  }
  
  public void build(){
    OpenUriDialog modDevelopsDialog = new OpenUriDialog(Core.bundle.get("groups.qq.develops"), Sgl.modDevelopGroup);
    OpenUriDialog githubProjectDialog = new OpenUriDialog(Core.bundle.get("github.mod.source"), Sgl.githubProject);
  
    cont.clear();
    
    Table board = new Table(Tex.button);
    board.defaults().margin(0).left().grow();
    
    ImageButton infoIcon = new ImageButton(Singularity.getModAtlas("icon_info"));
    Table infoButton = new Table(Tex.button);
    infoButton.left().add(infoIcon).size(80, 80).padLeft(0);
    infoButton.button(Core.bundle.get("dialog.aboutMod.modInfo"), () -> {
      BaseDialog info = new BaseDialog("Mod " + Core.bundle.get("misc.info"));
      info.cont.table(t -> {
        Table table = new Table();
        table.defaults().left().margin(0).height(50).grow().padTop(5);
        table.add("Mod " + Core.bundle.get("misc.name") + ":");
        table.add(Vars.mods.locateMod("singularity").meta.displayName);
        table.row();
        table.add("Mod " + Core.bundle.get("misc.author") + ":");
        table.add(Vars.mods.locateMod("singularity").meta.author);
        table.button(Core.bundle.get("misc.contact"), Sgl.ui.contact::show);
        table.row();
        table.add("Mod " + Core.bundle.get("misc.version") + ":");
        table.add(Vars.mods.locateMod("singularity").meta.version);
        table.button(Core.bundle.get("infos.checkUpdate"), () -> {});
        table.row();
        table.add(Core.bundle.get("infos.releaseDate") + ":");
        table.add(Core.bundle.get("mod.updateDate"));
        table.row();
        table.add(Core.bundle.get("groups.qq.develops") + ":");
        table.add();
        table.button(Core.bundle.get("misc.join"), modDevelopsDialog::showDialog);
        t.add(table).width(700).growY();
        t.row();
        t.button(Core.bundle.get("misc.back"), info::hide).width(80).padTop(20);
      });
      info.show();
    }).grow().height(80);
    board.add(infoButton).size(650, 80).padTop(5);
  
    board.row();
    
    ImageButton githubIcon = new ImageButton(Singularity.getModAtlas("icon_github"));
    Table githubButton = new Table(Tex.button);
    githubButton.left().add(githubIcon).size(80, 80).padLeft(0);
    githubButton.button(Core.bundle.get("dialog.aboutMod.githubProject"), githubProjectDialog::showDialog).grow().height(80);
    board.add(githubButton).size(650, 80);
  
    board.row();
    
    ImageButton otherModIcon = new ImageButton(Singularity.getModAtlas("icon_other_mod"));
    Table otherModButton = new Table(Tex.button);
    otherModButton.left().add(otherModIcon).size(80, 80).padLeft(0);
    otherModButton.button(Core.bundle.get("dialog.aboutMod.otherMod"), () -> {}).grow().height(80);
    board.add(otherModButton).size(650, 80);
  
    board.row();
    
    ImageButton thanksIcon = new ImageButton(Singularity.getModAtlas("icon_thanks"));
    Table thanksButton = new Table(Tex.button);
    thanksButton.left().add(thanksIcon).size(80, 80).padLeft(0);
    thanksButton.button(Core.bundle.get("dialog.aboutMod.thanks"), () -> {}).grow().height(80);
    board.add(thanksButton).size(650, 80);
  
    board.row();
  
    board.button(Core.bundle.get("misc.back"), this::hide).width(80).center().padTop(20);
    cont.add(board);
  }
}
