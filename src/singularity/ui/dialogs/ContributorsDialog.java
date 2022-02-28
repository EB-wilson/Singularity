package singularity.ui.dialogs;

import arc.Core;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.BaseDialog;
import singularity.Contribute;
import singularity.Contributors;
import singularity.Sgl;
import singularity.Singularity;
import universeCore.ui.table.ZoomableTable;

public class ContributorsDialog extends BaseDialog{
  protected Table table = new ZoomableTable();
  
  public ContributorsDialog(){
    super("");
    margin(0f).marginBottom(8);
    
    titleTable.clearChildren();
    
    cont.add(table).size(Core.scene.getWidth(), Core.scene.getHeight());
  }
  
  public void build(){
    addCloseButton();
    
    touchable = Touchable.enabled;
  
    table.table(table -> {
      table.defaults().pad(8);
      table.table(Tex.pane, t -> {
        t.defaults().center().top().padTop(6);
        t.image(Singularity.getModAtlas("author")).size(64);
        t.row();
        t.add(Contribute.author.localize()).color(Pal.accent);
        t.row();
        t.add(new ContributorTable(Sgl.contributors.get(Contribute.author).get(0)));
      }).growX().fillY().padLeft(30).padRight(30).margin(10);
      
      table.row();
      table.image().color(Pal.accent).growX().height(3).colspan(4).pad(0).padTop(4).padBottom(4);
      table.row();
      
      table.table(cons -> {
        cons.defaults().pad(8).padLeft(8).padRight(8);
        for(Contribute contribute: Contribute.values()){
          if(contribute == Contribute.author) continue;
          
          cons.table(Tex.buttonTrans, t -> {
            t.defaults().center().top().pad(16).padTop(12);
            t.image(Singularity.getModAtlas(contribute.name())).size(64);
            t.row();
            t.add(contribute.localize()).padTop(6);
            t.row();
            Seq<Contributors.Contributor> seq = Sgl.contributors.get(contribute);
            if(seq != null) for(Contributors.Contributor contributor: seq){
              t.add(new ContributorTable(contributor)).fillY();
              t.row();
            }
          }).fillY();
        }
      });
    }).get();
  }
  
  protected static class ContributorTable extends Table{
    public ContributorTable(Contributors.Contributor contributor){
      image(contributor.avatar).size(180);
      row();
      add(contributor.displayName).color(Pal.accent).fill();
    }
  }
}
