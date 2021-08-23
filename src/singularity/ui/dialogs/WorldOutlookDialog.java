package singularity.ui.dialogs;

import arc.Core;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;

public class WorldOutlookDialog extends BaseDialog {
  public WorldOutlookDialog(String title) {
    super(title);
  }
  
  @SuppressWarnings("CodeBlock2Expr")
  public void showDialog(){
    cont.clear();
    Table textComp = new Table(Tex.button);
    Label text = new Label(Core.bundle.get("dialog.worldOutlook.defaultText"));
    textComp.add(text);
    
    cont.clear();
    cont.table(t -> {
      t.defaults().size(600, 60).left();
      t.button(Core.bundle.get("dialog.worldOutlook.background"), () -> {
        text.setText(Core.bundle.get("dialog.worldOutlook.backgroundText"));
      }).size(120, 60);
      t.button(Core.bundle.get("dialog.worldOutlook.technology"), () -> {
        text.setText(Core.bundle.get("dialog.worldOutlook.technologyText"));
      }).size(120, 60).padLeft(120);
      t.button(Core.bundle.get("dialog.worldOutlook.plans"), () -> {
        text.setText(Core.bundle.get("dialog.worldOutlook.plansText"));
      }).size(120, 60).padLeft(120);
    });
    cont.row();
    cont.add(new ScrollPane(textComp)).size(600, 500).padTop(40);
    cont.row();
    cont.button(Core.bundle.get("misc.close"), this::hide).size(100, 60);
    show();
  }
}
