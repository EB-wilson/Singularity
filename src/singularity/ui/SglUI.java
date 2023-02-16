package singularity.ui;

import mindustry.Vars;
import mindustry.gen.Unit;
import singularity.Sgl;
import singularity.ui.dialogs.*;
import singularity.ui.fragments.EntityInfoFrag;
import singularity.ui.fragments.UnitHealthDisplay;
import singularity.ui.fragments.UnitStatusDisplay;
import singularity.ui.fragments.override.SglMenuFrag;

public class SglUI{
  //ui相关
  public EntityInfoFrag entityInfoFrag;

  /**主菜单*/
  public MainMenu mainMenu;
  public AboutModDialog aboutDialog;
  public PublicInfoDialog publicInfo;
  public ContributorsDialog contributors;

  public DistNetMonitorDialog bufferStat;

  public void init(){
    entityInfoFrag = new EntityInfoFrag();
    entityInfoFrag.displayMatcher.put(new UnitHealthDisplay<>(), e -> e instanceof Unit);
    entityInfoFrag.displayMatcher.put(new UnitStatusDisplay<>(), e -> e instanceof Unit);

    mainMenu = new MainMenu();
    aboutDialog = new AboutModDialog();
    publicInfo = new PublicInfoDialog();
    contributors = new ContributorsDialog();
    bufferStat = new DistNetMonitorDialog();

    entityInfoFrag.build(Vars.ui.hudGroup);
    mainMenu.build();
    aboutDialog.build();
    contributors.build();

    if(!Sgl.config.disableModMainMenu){
      Vars.ui.menufrag = new SglMenuFrag();
      Vars.ui.menufrag.build(Vars.ui.menuGroup);
    }
  }
}
