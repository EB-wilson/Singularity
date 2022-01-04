package singularity.ui;

import arc.scene.Group;
import mindustry.Vars;
import singularity.ui.dialogs.AboutModDialog;
import singularity.ui.dialogs.ContributorsDialog;
import singularity.ui.dialogs.MainMenu;
import singularity.ui.dialogs.PublicInfoDialog;
import singularity.ui.dialogs.override.SglPlanetsDialog;
import singularity.ui.fragments.override.SecondaryConfigureFragment;
import universeCore.util.handler.FieldHandler;

import static mindustry.Vars.ui;

public class SglUI{
  //ui相关
  /**主菜单*/
  public MainMenu mainMenu;
  public AboutModDialog aboutDialog;
  public PublicInfoDialog publicInfo;
  public ContributorsDialog contributors;
  
  public SecondaryConfigureFragment secConfig;
  
  public void init(){
    mainMenu = new MainMenu();
    aboutDialog = new AboutModDialog();
    publicInfo = new PublicInfoDialog();
    contributors = new ContributorsDialog();
    
    secConfig = new SecondaryConfigureFragment();
  
    mainMenu.build();
    aboutDialog.build();
    contributors.build();
    
    Group overlay = FieldHandler.getValue(Vars.control.input.frag, "group");
    secConfig.build(overlay);
    
    //override
    if(!Vars.net.server()) ui.planet = new SglPlanetsDialog();
  }
}
