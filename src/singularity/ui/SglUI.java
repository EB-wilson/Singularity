package singularity.ui;

import mindustry.Vars;
import singularity.ui.dialogs.AboutModDialog;
import singularity.ui.dialogs.ContributorsDialog;
import singularity.ui.dialogs.MainMenu;
import singularity.ui.dialogs.PublicInfoDialog;
import singularity.ui.dialogs.override.SglPlanetsDialog;

import static mindustry.Vars.ui;

public class SglUI{
  //ui相关
  /**主菜单*/
  public MainMenu mainMenu;
  public AboutModDialog aboutDialog;
  public PublicInfoDialog publicInfo;
  public ContributorsDialog contributors;
  
  public void init(){
    mainMenu = new MainMenu();
    aboutDialog = new AboutModDialog();
    publicInfo = new PublicInfoDialog();
    contributors = new ContributorsDialog();
  
    mainMenu.build();
    aboutDialog.build();
    contributors.build();
    
    //override
    if(!Vars.net.server()) ui.planet = new SglPlanetsDialog();
  }
}
