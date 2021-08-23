package singularity.ui.dialogs;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;

public class OpenUriDialog extends BaseDialog {
  public String uri;
  public String str;
  
  public OpenUriDialog(String title, String uri, String str){
    this(title, uri);
    this.str = str;
  }
  
  public OpenUriDialog(String title, String uri){
    this(title);
    this.uri = uri;
  }
  
  public OpenUriDialog(String title){
    super(title);
    this.uri = "";
  }
  
  @SuppressWarnings("CodeBlock2Expr")
  public void showDialog(){
    StringBuilder displayUri = new StringBuilder("uri:");
    cont.clear();
    Table textTable = new Table(Tex.button);
    textTable.defaults().left();
    Table buttons = new Table();
  
    if(uri.length() > 30){
      for(int index = 0; index < uri.length(); index++){
        if(index%30 == 0){
          displayUri.append("\n").append(uri, index, Math.min(uri.length() - 1, index + 30));
        }
      }
    }
    else{
      displayUri.append("\n").append(uri);
    }
    textTable.add(str + "\n" + displayUri).size(385, 160);
    
    buttons.button(Core.bundle.get("misc.back"), this::hide).size(90, 60);
    buttons.button(Core.bundle.get("misc.copy"), () -> {
      Core.app.setClipboardText(uri);
    }).size(90, 60).padLeft(50);
    buttons.button(Core.bundle.get("misc.open"), () -> {
      if(!Core.app.openURI(uri)){
        Vars.ui.showErrorMessage("[red]" + Core.bundle.get("dialog.openUriDialog.warning"));
        Core.app.setClipboardText(uri);
      }
    }).size(90, 60).padLeft(50);
    
    cont.add(textTable).size(400, 170);
    cont.row();
    cont.add(buttons).size(400, 60).padTop(20);
    show();
  }
}
