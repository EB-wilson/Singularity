package singularity.ui;

import singularity.ui.dialogs.MainMenu;
import singularity.ui.dialogs.PublicInfoDialog;
import singularity.ui.dialogs.override.SglPlanetsDialog;

import static mindustry.Vars.ui;

public class SglUI{
  //ui相关
  /**主菜单*/
  public MainMenu mainMenu;
  public PublicInfoDialog publicInfo;
  
  public void init(){
    mainMenu = new MainMenu();
    publicInfo = new PublicInfoDialog();
    
    //override
    ui.planet = new SglPlanetsDialog();
  }
}
