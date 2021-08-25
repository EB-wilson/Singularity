package singularity.ui;

import arc.Core;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.event.ClickListener;
import arc.scene.event.HandCursorListener;
import arc.scene.ui.Dialog;
import arc.scene.ui.Image;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.dialogs.DatabaseDialog;
import singularity.type.SglContentType;
import singularity.ui.dialogs.ContactDialog;
import singularity.ui.dialogs.MainMenu;
import singularity.ui.dialogs.override.SglDatabaseDialog;
import singularity.ui.dialogs.override.SglPlanetsDialog;
import universeCore.util.UncContentType;

import static mindustry.Vars.mobile;
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
    ui.database = new SglDatabaseDialog();
    ui.planet = new SglPlanetsDialog();
  }
}
