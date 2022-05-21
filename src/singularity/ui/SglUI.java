package singularity.ui;

import arc.scene.Group;
import mindustry.Vars;
import singularity.ui.dialogs.*;
import singularity.ui.dialogs.override.SglPlanetsDialog;
import singularity.ui.fragments.SecondaryConfigureFragment;
import universecore.util.handler.FieldHandler;

import static mindustry.Vars.ui;

public class SglUI{
  //ui相关
  /**主菜单*/
  public MainMenu mainMenu;
  public AboutModDialog aboutDialog;
  public PublicInfoDialog publicInfo;
  public ContributorsDialog contributors;
  
  public SecondaryConfigureFragment secConfig;
  public DistNetMonitorDialog bufferStat;

  public void init(){
    mainMenu = new MainMenu();
    aboutDialog = new AboutModDialog();
    publicInfo = new PublicInfoDialog();
    contributors = new ContributorsDialog();
    bufferStat = new DistNetMonitorDialog();
    
    secConfig = new SecondaryConfigureFragment();
  
    mainMenu.build();
    aboutDialog.build();
    contributors.build();
    bufferStat.build();
    
    Group overlay = FieldHandler.getValue(Vars.control.input.frag, "group");
    secConfig.build(overlay);
    
    //override
    if(!Vars.net.server()) ui.planet = new SglPlanetsDialog();
  }
}
