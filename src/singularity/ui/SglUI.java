package singularity.ui;

import arc.scene.Group;
import mindustry.Vars;
import singularity.Sgl;
import singularity.ui.dialogs.*;
import singularity.ui.fragments.SecondaryConfigureFragment;
import singularity.ui.fragments.override.SglMenuFrag;
import universecore.util.handler.FieldHandler;

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
    
    Group overlay = FieldHandler.getValueTemp(Vars.control.input, "group");
    secConfig.build(overlay);

    if(!Sgl.config.disableModMainMenu){
      Vars.ui.menufrag = new SglMenuFrag();
      Vars.ui.menufrag.build(Vars.ui.menuGroup);
    }
  }
}
