package singularity.ui.fragments;

import arc.Core;
import arc.Events;
import arc.func.Boolp;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.OrderedMap;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import universecore.util.handler.FieldHandler;

public class ToolBarFrag {
  private final OrderedMap<String, ToolEntry> tools = new OrderedMap<>();

  public Table toolsTable;

  public void init(){
    Events.on(EventType.WorldLoadEvent.class, e -> {
      Core.app.post(() -> {
        Table blockCatTable = FieldHandler.getValueDefault(Vars.ui.hudfrag.blockfrag, "blockCatTable");
        blockCatTable.table(((TextureRegionDrawable)Tex.whiteui).tint(Tmp.c1.set(Color.black).a(0.6f)),tools -> {
          tools.top().pane(Styles.noBarPane, this::buildTools).growY().width(50).left().get().setScrollingDisabledX(true);
        }).width(50).growY();
      });
    });
  }

  public void buildTools(Table tools){
    toolsTable = tools;
    tools.clearChildren();
    tools.top().defaults().top().size(50).pad(0);

    for (ToolEntry entry : this.tools.values()) {
      tools.button(entry.icon, Styles.clearNoneTogglei, entry.listener).update(b -> b.setChecked(entry.checked.get()));
      tools.row();
    }
  }

  public void addTool(String name, Drawable icon, Runnable listener, Boolp checked){
    tools.put(name, new ToolEntry(icon, listener, checked));
    if (toolsTable != null) buildTools(toolsTable);
  }

  public void removeTool(String name){
    tools.remove(name);
  }

  public void clearTools(){
    tools.clear();
  }

  public static class ToolEntry{
    Drawable icon;
    Runnable listener;
    Boolp checked;

    public ToolEntry(Drawable icon, Runnable listener, Boolp checked){
      this.icon = icon;
      this.listener = listener;
      this.checked = checked;
    }
  }
}
