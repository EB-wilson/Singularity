package singularity.ui.dialogs;

import arc.Core;
import arc.func.Cons;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import universeCore.UncCore;
import universeCore.util.animLayout.CellAnimateGroup;
import universeCore.util.animLayout.CellChangeColorAction;

public class BaseListDialog extends BaseDialog{
  public float width = 750, height = 520;
  public float itemBoardWidth = 200, itemBoardHeight = 440, itemHeight = 60;
  public float pad = 4, margin = 8;
  
  public Cons<Table> defaultInfo = info -> {
    info.add(Core.bundle.get("misc.noInfo"));
  };
  
  public Table infoTable, itemsTable, buttonTable;
  
  Cell<Table> infoCell;
  ItemEntry current, lastEntry;
  
  boolean rebuild;
  
  Seq<ItemEntry> items = new Seq<>();
  Cons<Table> buildPage = pane -> {
    pane.clearChildren();
    for(ItemEntry item: items){
      pane.button(t -> {
        t.defaults().grow().margin(0).top();
        item.itemDisplay.get(t);
      }, Styles.underlineb, () -> current = item).update(b -> {
        b.setChecked(current == item);
        b.touchable(() -> b.isChecked()? Touchable.disabled: Touchable.enabled);
      }).height(itemHeight).growX();
      pane.row();
    }
  };
  
  public BaseListDialog(){
    super("");
    
    titleTable.clear();
  }
  
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
  
  public void set(Seq<ItemEntry> seq){
    items = seq;
  }
  
  public void build(){
    cont.clearChildren();
    
    float infoBoardWidth = width - itemBoardWidth - 4;
    
    cont.table(left -> {
      left.table(Tex.buttonTrans, itemTable -> {
        itemTable.pane(t -> {
          itemsTable = t;
          buildPage.get(t);
        }).grow().margin(0).update(s -> {
          if(lastEntry != current || rebuild){
            lastEntry = current;
            rebuild = false;
            UncCore.cellActions.add(new CellAnimateGroup(
                new CellChangeColorAction(infoCell, infoTable,  infoTable.color.cpy().a(1), infoTable.color.cpy().a(0), 6f),
                (Runnable) () -> {
                  infoTable.clearChildren();
                  if(current == null){
                    defaultInfo.get(infoTable);
                  }
                  else{
                    current.infoDisplay.get(infoTable);
                  }
                },
                new CellChangeColorAction(infoCell, infoTable,  infoTable.color.cpy().a(0), infoTable.color.cpy().a(1), 6f)
            ));
          }
        });
      }).growX().margin(0).height(itemBoardHeight).pad(0);
      left.row();
      left.button(Core.bundle.get("misc.back"), Icon.left, this::hide).padTop(pad).grow();
    }).size(itemBoardWidth, height);
    
    cont.table(Tex.buttonTrans, t -> {
      t.pane(pane -> {
        pane.margin(margin);
        infoCell = pane.table(i -> defaultInfo.get(i)).grow().margin(0);
        infoTable = infoCell.get();
      }).grow();
      t.row();
      buttonTable = t.table().fill().bottom().padBottom(pad).get();
    }).size(infoBoardWidth, height).padLeft(pad);
  }
  
  public void rebuild(){
    current = null;
    rebuild = true;
    
    buildPage.get(itemsTable);
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
