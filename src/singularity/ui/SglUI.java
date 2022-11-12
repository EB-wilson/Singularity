package singularity.ui;

import mindustry.Vars;
import singularity.Sgl;
import singularity.ui.dialogs.*;
import singularity.ui.fragments.override.SglMenuFrag;

public class SglUI{
  //ui相关
  /**主菜单*/
  public MainMenu mainMenu;
  public AboutModDialog aboutDialog;
  public PublicInfoDialog publicInfo;
  public ContributorsDialog contributors;

  public DistNetMonitorDialog bufferStat;

  public void init(){
    mainMenu = new MainMenu();
    aboutDialog = new AboutModDialog();
    publicInfo = new PublicInfoDialog();
    contributors = new ContributorsDialog();
    bufferStat = new DistNetMonitorDialog();
  
    mainMenu.build();
    aboutDialog.build();
    contributors.build();

    if(!Sgl.config.disableModMainMenu){
      Vars.ui.menufrag = new SglMenuFrag();
      Vars.ui.menufrag.build(Vars.ui.menuGroup);
    }
  }
}
