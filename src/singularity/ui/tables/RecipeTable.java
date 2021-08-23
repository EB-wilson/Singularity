package singularity.ui.tables;

import arc.Core;
import arc.scene.ui.ImageButton;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatValue;
import mindustry.world.meta.Stats;
import singularity.Singularity;

public class RecipeTable{
  public Stats[] stats;
  public int page = 0;
  
  public RecipeTable(int length){
    this.stats = new Stats[length];
  }
  
  public void init(Table parent){
    parent.clear();
    if(stats.length == 1){
      updateRecipe(parent, 0);
      return;
    }
    Table main = new Table();
    Table table = new Table(Tex.button);
    Table pageDisplay = new Table(Tex.button);
    Table pageChanger = new Table();
    
    updateRecipe(table, page);
    
    main.defaults().growX().marginLeft(0).marginRight(0);
    main.add(table).width(320);
    main.row();
    
    pageChanger.defaults().left();
    pageChanger.add(new ImageButton(Singularity.getModAtlas("arrow_left")){{
      clicked(() -> {
        if (page > 0) {
          page--;
          updatePage(pageDisplay);
          updateRecipe(table, page);
        }
      });
    }}).size(65, 65);
    pageChanger.add(pageDisplay).size(110, 70);
    updatePage(pageDisplay);
    pageChanger.add(new ImageButton(Singularity.getModAtlas("arrow_right")){{
      clicked(() -> {
        if (page < stats.length - 1) {
          page++;
          updatePage(pageDisplay);
          updateRecipe(table, page);
        }
      });
    }}).size(65, 65);
    
    Table t = new Table();
    t.clear();
    t.left();
    TextButton b = new TextButton(Core.bundle.get("misc.open"));
    b.clicked(() -> {
      t.clear();
      t.row();
      t.add(main);
    });
    t.add(b);
    
    main.add(pageChanger).size(250, 70);
    main.row();
    main.button(Core.bundle.get("misc.close"), () -> {
      t.clear();
      t.add(b);
    }).size(80, 50).padTop(5);
    
    parent.add(t);
  }
  
  public void updatePage(Table pageDisplay){
    pageDisplay.clearChildren();
    pageDisplay.add(Core.bundle.get("misc.page.a") + " " + (page + 1) + "/" + stats.length + " " + Core.bundle.get("misc.page.b"));
  }
  
  public void updateRecipe(Table table, int page){
    table.clear();
    for(StatCat cat : stats[page].toMap().keys()){
      OrderedMap<Stat, Seq<StatValue>> map = stats[page].toMap().get(cat);
      if(map.size == 0) continue;

      if(stats[page].useCategories){
        table.add("@category." + cat.name()).color(Pal.accent).fillX();
        table.row();
      }

      for(Stat stat : map.keys()){
        table.table(inset -> {
          inset.left();
          inset.add("[lightgray]" + stat.localized() + ":[] ").left();
          Seq<StatValue> arr = map.get(stat);
          for(StatValue value : arr){
            value.display(inset);
            inset.add().size(10f);
          }
        }).fillX().padLeft(10);
        table.row();
      }
    }
  }
}
