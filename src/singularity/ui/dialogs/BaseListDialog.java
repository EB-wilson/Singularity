package singularity.ui.dialogs;

import arc.Core;
import arc.func.Cons;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import universeCore.UncCore;
import universeCore.util.animLayout.CellAnimateGroup;
import universeCore.util.animLayout.CellChangeColorAction;

public class BaseListDialog extends BaseDialog{
  public float width = 750, height = 520;
  public float itemBoardWidth = 180, itemHeight = 65;
  public float pad = 4, margin = 8;
  
  Seq<ItemEntry> items = new Seq<>();
  
  Table infoTable;
  Cell<Table> infoCell;
  ItemEntry current;
  
  public BaseListDialog(String title){
    super(title);
  }
  
  public BaseListDialog(String title, Seq<ItemEntry> items){
    this(title);
    this.items = items;
  }
  
  public BaseListDialog(String title, ItemEntry... items){
    this(title, Seq.with(items));
  }
  
  public void add(ItemEntry item){
    items.add(item);
  }
  
  public void rebuild(){
    cont.clearChildren();
    
    float infoBoardWidth = width - itemBoardWidth - 4;
    
    cont.table(left -> {
      left.table(Tex.buttonTrans, itemTable -> {
        itemTable.pane(pane -> {
          for(ItemEntry item: items){
            pane.button(t -> {
              t.defaults().grow().margin(0);
              item.itemDisplay.get(t);
            }, Styles.underlineb, () -> {
              UncCore.cellActions.add(new CellAnimateGroup(
                  new CellChangeColorAction(infoCell, infoTable, infoTable.color.cpy().a(0), 6f),
                  (Runnable) () -> {
                    infoTable.clearChildren();
                    item.infoDisplay.get(infoTable);
                    current = item;
                  },
                  new CellChangeColorAction(infoCell, infoTable, infoTable.color.cpy().a(1), 6f)
              ));
            }).update(b -> b.setChecked(current == item)).height(itemHeight).growX();
          }
        }).grow().margin(0);
      }).grow().margin(0);
      left.row();
      left.button(Core.bundle.get("misc.back"), this::hide).padTop(pad).growX().height(84);
    }).size(itemBoardWidth, height);
    
    cont.table(Tex.buttonTrans, t -> {
      t.pane(pane -> {
        infoCell = t.table(info -> {
          info.add(Core.bundle.get("misc.noInfo"));
        }).grow().margin(0);
        infoTable = infoCell.get();
      }).grow();
    }).size(infoBoardWidth, height).padLeft(pad);
  }
  
  public static class ItemEntry{
    Cons<Table> itemDisplay;
    Cons<Table> infoDisplay;
    
    public ItemEntry(Cons<Table> itemDisplay, Cons<Table> infoDisplay){
      this.itemDisplay = itemDisplay;
      this.infoDisplay = infoDisplay;
    }
  }
}
