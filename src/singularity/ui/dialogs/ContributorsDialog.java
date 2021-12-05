package singularity.ui.dialogs;

import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.BaseDialog;
import singularity.Contribute;
import singularity.Contributors;
import singularity.Sgl;
import singularity.Singularity;

public class ContributorsDialog extends BaseDialog{
  public float maxZoom = 1f, minZoom = 0.4f;
  protected float lastZoom;
  
  public ContributorsDialog(){
    super("");
    margin(0f).marginBottom(8);
    
    titleTable.clearChildren();
  
    //scaling/drag input
    addListener(new InputListener(){
      @Override
      public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
        cont.setScale(Mathf.clamp(cont.scaleX - amountY / 10f * cont.scaleX, 0.25f, 1f));
        cont.setOrigin(Align.center);
        cont.setTransform(true);
        return true;
      }
    
      @Override
      public boolean mouseMoved(InputEvent event, float x, float y){
        cont.requestScroll();
        return super.mouseMoved(event, x, y);
      }
    });
  
    touchable = Touchable.enabled;
  
    addCaptureListener(new ElementGestureListener(){
      @Override
      public void zoom(InputEvent event, float initialDistance, float distance){
        if(lastZoom < 0){
          lastZoom = scaleX;
        }
      
        cont.setScale(Mathf.clamp(distance / initialDistance * lastZoom, minZoom, maxZoom));
        cont.setOrigin(Align.center);
        cont.setTransform(true);
      }
    
      @Override
      public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
        lastZoom = cont.scaleX;
      }
    
      @Override
      public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
        cont.moveBy(deltaX, deltaY);
      }
    });
  }
  
  public void build(){
    addCloseButton();
    
    touchable = Touchable.enabled;
  
    cont.table(table -> {
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
    });
  }
  
  protected static class ContributorTable extends Table{
    public ContributorTable(Contributors.Contributor contributor){
      image(contributor.avatar).size(180);
      row();
      add(contributor.displayName).color(Pal.accent).fill();
    }
  }
}
