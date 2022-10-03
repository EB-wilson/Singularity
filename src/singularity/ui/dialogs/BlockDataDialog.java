package singularity.ui.dialogs;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import universecore.debugs.ObjectDataMonitor;
import universecore.debugs.ObjectDataMonitor.VarStructure;
import universecore.util.handler.FieldHandler;

public class BlockDataDialog extends BaseDialog{
  private static final ObjectMap<String, VarStructure> emptyVars = new ObjectMap<>(0);
  
  private final ObjectDataMonitor monitor = new ObjectDataMonitor();
  private ObjectMap<String, VarStructure> vars = emptyVars;
  
  public final Seq<Building> targets;
  
  public BlockDataDialog(Seq<Building> targets){
    super(Core.bundle.get("dialog.dataMonitor.title"));
    this.targets = targets;
    addCloseButton();
    buttons.button(Core.bundle.get("debugModule.dataMonitor.vars"), monitor.varsDisplay::show);
  }
  
  public void setVars(ObjectMap<String, VarStructure> vars){
    this.vars = vars;
    monitor.setVars(vars);
    build();
  }
  
  public void unloadVars(){
    vars = emptyVars;
    monitor.unloadVars();
  }
  
  public void build(){
    cont.clearChildren();
    cont.pane(targetBoard -> {
      targetBoard.defaults().margin(4).pad(5).top().left().grow();
      int index = 0;
      for(Building target: targets){
        DataMonitorDialog dataMonitor = new DataMonitorDialog(target);
        int temp = index;
        targetBoard.table(Tex.pane, monitor -> {
          monitor.defaults().margin(4).grow().pad(4);
          Image image = new Image(target.getDisplayIcon());
        
          monitor.add(image).size(176).center();
          monitor.row();
          monitor.add(target.toString()).style(Styles.outlineLabel).color(Color.white);
          
          monitor.row();
        
          monitor.table(t -> {
            t.defaults().grow();
            t.add(Core.bundle.format("data.position", target.x, target.y)).color(Pal.gray).padLeft(3);
            t.row();
            
            Table button = new Table();
            Label text = new Label("[gray]flag");
            text.update(() -> {
              String flag = vars.findKey(target, true);
              text.setText(flag == null? "[gray]flag": flag);
            });
            button.clicked(() -> {
              String preString = text.getText().toString();
              Object result = FieldHandler.getValueDefault(targets, "items");
              BlockDataDialog.this.monitor.setFlag.show(text, preString, result == null? new ObjectDataMonitor.ObjectStructure(true): new ObjectDataMonitor.ArrayStructure(result, temp));
            });
            button.add(text);
            
            t.image().color(Pal.gray).colspan(3).height(4).growX().padTop(3);
            t.row();
            t.add(button).width(200).height(30).padTop(4);
          });
          
          monitor.clicked(dataMonitor::show);
        }).size(200, 290);
        index++;
      }
    }).fill();
  }
  
  private class DataMonitorDialog extends BaseDialog{
    final Building target;
  
    public DataMonitorDialog(Building target){
      super(Core.bundle.get("dialog.dataMonitor.blockData"));
      this.target = target;
      addCloseButton();
      buttons.button(Core.bundle.get("debugModule.dataMonitor.vars"), monitor.varsDisplay::show);
      build();
    }
  
    public void build(){
      if(target == null) return;
      cont.clearChildren();
      cont.table(data -> {
        data.defaults().left().padLeft(5f).grow();
        data.table(t -> {
          t.defaults().left();
          t.image(target.block.fullIcon).size(60, 60).center();
          t.row();
          t.table(c -> {
            c.add(Core.bundle.get("dialog.dataMonitor.target") + ":" + target.block.localizedName);
            c.row();
            c.add("[gray]" + target.block.name + " - " + target.id);
            c.row();
            c.add("[gray]" + Core.bundle.format("dialog.dataMonitor.blockPosition", target.x, target.y));
          });
        });
        data.row();
      
        data.pane(monitor.creatDataTable(target)).grow();
      });
    }
  }
}
