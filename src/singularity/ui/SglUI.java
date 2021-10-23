package singularity.ui;

import singularity.ui.dialogs.AboutModDialog;
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
  
  public void init(){
    mainMenu = new MainMenu();
    aboutDialog = new AboutModDialog();
    publicInfo = new PublicInfoDialog();
  
    mainMenu.build();
    aboutDialog.build();
    
    //override
    ui.planet = new SglPlanetsDialog();
  }
}
