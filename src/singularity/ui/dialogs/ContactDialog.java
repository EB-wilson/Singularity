package singularity.ui.dialogs;

import singularity.Sgl;
import singularity.Singularity;
import arc.Core;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;

public class ContactDialog extends BaseDialog{
  private final String[][] author = new String[][]{
    new String[]{"qq", Sgl.qq},
    new String[]{"telegram", Sgl.telegram},
    new String[]{"facebook", Sgl.facebook},
  };
  
  private final String[][] groups = new String[][]{
    new String[]{"qq1", Sgl.qqGroup1},
    new String[]{"qq2", Sgl.qqGroup2},
    new String[]{"telegram", Sgl.telegramGroup},
  };
  
  public ContactDialog(){
    super(Core.bundle.get("dialog.contact.title"));
    build();
  }
  
  public void build(){
    cont.clear();
  
    Table buttons = new Table(Tex.button);
    cont.add(Core.bundle.get("dialog.contact.text"));
    cont.row();
    for(String[] s: author){
      String str = s[0];
      String uri = s[1];
      ImageButton button = new ImageButton(Singularity.getModAtlas("logos_" + str));
      button.resizeImage(50);
      button.clicked(() -> new OpenUriDialog(str, uri, Core.bundle.get("dialog.contact.authorBy_" + str)).showDialog());
      buttons.add(button).size(80, 70).padLeft((str.equals("qq"))? 0: 20);
    }
    
    BaseDialog groupsDialog = new BaseDialog(Core.bundle.get("misc.group")){{
      Table groupBoard = new Table(Tex.button);
      this.cont.table(t -> {
        t.add(Core.bundle.get("dialog.contact.groups"));
        t.row();
        for(String[] s: groups){
          String str = s[0];
          String uri = s[1];
          ImageButton button = new ImageButton(Singularity.getModAtlas("logos_" + (str.equals("qq1") || str.equals("qq2") ? "qq": str)));
          button.resizeImage(50);
          button.clicked(() -> new OpenUriDialog(str, uri, Core.bundle.get("dialog.contact.groupBy_" + str)).showDialog());
          groupBoard.add(button).size(80, 70).padLeft((str.equals("qq1"))? 0: 20);
        }
        t.add(groupBoard);
        t.row();
        t.button(Core.bundle.get("misc.back"), this::hide).size(120, 70);
      });
    }};
    
    ImageButton groupsButton = new ImageButton(Singularity.getModAtlas("groups"));
    groupsButton.resizeImage(50);
    groupsButton.clicked(groupsDialog::show);
    buttons.add(groupsButton).size(80, 70).padLeft(20);
    
    cont.add(buttons).size(400, 200);
    cont.row();
    cont.button(Core.bundle.get("misc.back"), this::hide).size(100, 60);
  }
}
