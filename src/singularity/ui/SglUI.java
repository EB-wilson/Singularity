package singularity.ui;

import singularity.ui.dialogs.ContactDialog;
import singularity.ui.dialogs.MainMenu;
import singularity.ui.dialogs.override.SglPlanetsDialog;

import static mindustry.Vars.ui;

public class SglUI{
  //ui相关
  /**[联系作者] 对话框*/
  public ContactDialog contact;
  /**主菜单*/
  public MainMenu mainMenu;
  
  public void init(){
    contact = new ContactDialog();
    mainMenu = new MainMenu();
    
    //override
    ui.planet = new SglPlanetsDialog();
  }
}
