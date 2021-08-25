package singularity.ui.dialogs.override;

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
import universeCore.util.UncContentType;

import static mindustry.Vars.mobile;
import static mindustry.Vars.ui;

/**重写database对话框以重新排序类型（鬼知道臭猫什么时候才能把读写机制改一改）*/
public class SglDatabaseDialog extends DatabaseDialog{
  @Override
  public Dialog show(){
    super.show();
    cont.clear();
    
    Table table = new Table();
    table.margin(20);
    ScrollPane pane = new ScrollPane(table);
    
    Seq<Content>[] allContent = new Seq[UncContentType.newContentList.length];
    
    for(int i=0; i<UncContentType.newContentList.length; i++){
      allContent[i] = Vars.content.getBy(UncContentType.newContentList[i]);
    }
    
    for(int j = 0; j < allContent.length; j++){
      ContentType type = UncContentType.newContentList[j];
      
      Seq<Content> array = allContent[j].select(c -> c instanceof UnlockableContent && (!((UnlockableContent) c).isHidden() || ((UnlockableContent)c).node() != null));
      if(array.size == 0) continue;
      
      table.add("@content." + type.name() + ".name").growX().left().color(Pal.accent);
      table.row();
      table.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
      table.row();
      table.table(list -> {
        list.left();
        
        int cols = Mathf.clamp((Core.graphics.getWidth() - 30) / (32 + 10), 1, 18);
        int count = 0;
        
        for(int i = 0; i < array.size; i++){
          UnlockableContent unlock = (UnlockableContent)array.get(i);
          
          Image image = unlocked(unlock) ? new Image(unlock.uiIcon).setScaling(Scaling.fit) : new Image(Icon.lock, Pal.gray);
          list.add(image).size(8 * 4).pad(3);
          ClickListener listener = new ClickListener();
          image.addListener(listener);
          if(!mobile && unlocked(unlock)){
            image.addListener(new HandCursorListener());
            image.update(() -> image.color.lerp(!listener.isOver() ? Color.lightGray : Color.white, Mathf.clamp(0.4f * Time.delta)));
          }
          
          if(unlocked(unlock)){
            image.clicked(() -> {
              if(Core.input.keyDown(KeyCode.shiftLeft) && Fonts.getUnicode(unlock.name) != 0){
                Core.app.setClipboardText((char)Fonts.getUnicode(unlock.name) + "");
                ui.showInfoFade("@copied");
              }else{
                ui.content.show(unlock);
              }
            });
            image.addListener(new Tooltip(t -> t.background(Tex.button).add(unlock.localizedName)));
          }
          
          if((++count) % cols == 0){
            list.row();
          }
        }
      }).growX().left().padBottom(10);
      table.row();
    }
    
    cont.add(pane);
    return this;
  }
  
  boolean unlocked(UnlockableContent content){
    return (!Vars.state.isCampaign() && !Vars.state.isMenu()) || content.unlocked();
  }
}
