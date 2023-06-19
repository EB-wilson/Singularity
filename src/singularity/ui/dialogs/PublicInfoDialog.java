package singularity.ui.dialogs;

import arc.Core;
import arc.func.Cons;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Queue;
import arc.util.serialization.Jval;

import java.util.regex.Pattern;

public class PublicInfoDialog extends BaseListDialog{
  private static final String titlesUrl = "https://raw.githubusercontent.com/EB-wilson/Singularity/master/publicInfo/titles.hjson";
  private static final String langRegex = "#locale#";
  private static final Pattern imagePattern = Pattern.compile("<image *=.*>");
  
  Cons<Throwable> error = e -> {
    infoTable.table(t -> {
      StringBuilder errInfo = new StringBuilder(e.getMessage() + "\n");
      for(StackTraceElement err: e.getStackTrace()){
        errInfo.append(err).append("\n");
      }
      t.add(Core.bundle.format("warn.publicInfo.connectFailed", errInfo));
      t.row();
      t.button(Core.bundle.get("misc.refresh"), this::refresh).size(140, 60);
    });
  };
  
  volatile boolean initialized, titleLoaded;
  Throwable titleStatus;
  Jval titles;
  
  ObjectMap<String, Table> pages = new ObjectMap<>();
  
  Runnable loadPage;
  Queue<Runnable> queue = new Queue<>();
  
  public PublicInfoDialog(){
    super(Core.bundle.get("misc.publicInfo"));

  
    build();
    
    shown(this::refresh);
    
    update(() -> {
      if(initialized){
        if(!queue.isEmpty()){
          Runnable task = queue.removeLast();
          task.run();
        }
      }
    });
  }

  public void refresh(){

  }
  
  void buildChild(Jval sect){

  }
  

}
