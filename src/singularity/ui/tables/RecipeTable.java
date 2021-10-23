package singularity.ui.tables;

import arc.Core;
import arc.scene.ui.layout.Cell;
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
import universeCore.UncCore;
import universeCore.util.animLayout.CellAnimateGroup;
import universeCore.util.animLayout.CellChangeColorAction;
import universeCore.util.animLayout.CellResizeAction;

public class RecipeTable extends Table{
  public Stats[] stats;
  public int page = 0;
  
  Cell<Table> mainCell = table(Tex.buttonTrans).size(80, 50);
  Table main = mainCell.get();
  Cell<Table> recipeTable;
  Table animContainer = new Table();
  Cell<Table> animCell = main.add(animContainer).grow();
  
  CellAnimateGroup mainAnim;
  
  Runnable rebuild = () -> {};
  
  public RecipeTable(int length){
    this.stats = new Stats[length];
    
    rebuild = () -> {
      animContainer.clearChildren();
      
      animContainer.button(Core.bundle.get("misc.unfold"), () -> {
        if(mainAnim == null || !UncCore.cellActions.acting(mainAnim) || mainAnim.getCurrIndex() == 2){
          mainAnim = new CellAnimateGroup(
              new CellChangeColorAction(animCell, this, main.color.cpy().a(1), main.color.cpy().a(0), 20f),
              new CellResizeAction(mainCell, main, 360, 280, 20f).gradient(0.15f),
              (Runnable) () -> {
                animContainer.clearChildren();
        
                animContainer.pane(t -> {
                  recipeTable = t.table(table -> {
                    table.defaults().left().grow();
                    updateRecipe(table, page);
                  });
                }).size(340, 160).left();
        
                CellAnimateGroup pageAnim = new CellAnimateGroup(
                    new CellChangeColorAction(recipeTable, animContainer, recipeTable.get().color.cpy().a(1) ,recipeTable.get().color.cpy().a(0), 20f),
                    (Runnable) () -> updateRecipe(recipeTable.get(), page),
                    new CellChangeColorAction(recipeTable, animContainer, recipeTable.get().color.cpy().a(0), recipeTable.get().color.cpy().a(1), 20f)
                );
        
                animContainer.row();
                animContainer.table(pages -> {
                  pages.button(b -> b.image(Singularity.getModAtlas("arrow_left")), () -> {
                    if(page > 0) page--;
                    if(!UncCore.cellActions.acting(pageAnim)){
                      UncCore.cellActions.add(pageAnim);
                    }else if(pageAnim.getCurrIndex() == 1) pageAnim.restart();
                  }).size(40).update(button -> button.setDisabled(page <= 0));
          
                  pages.table(t -> t.add("").update(label -> {
                    label.setText(Core.bundle.format("misc.page", page+1, stats.length));
                  })).size(100, 50);
          
                  pages.button(b -> b.image(Singularity.getModAtlas("arrow_right")), () -> {
                    if(page < stats.length - 1) page++;
                    if(! UncCore.cellActions.acting(pageAnim)){
                      UncCore.cellActions.add(pageAnim);
                    }else if(pageAnim.getCurrIndex() == 1) pageAnim.restart();
                  }).size(40).update(button -> button.setDisabled(page >= stats.length - 1));
                });
  
                animContainer.row();
                animContainer.button(Core.bundle.get("misc.fold"), () -> {
                  if(! UncCore.cellActions.acting(mainAnim) || mainAnim.getCurrIndex() == 2){
                    mainAnim = new CellAnimateGroup(
                        new CellChangeColorAction(animCell, this, main.color.cpy().a(1), main.color.cpy().a(0), 20f),
                        new CellResizeAction(mainCell, main, 360, 280, 80, 50, 20f).gradient(0.15f),
                        rebuild,
                        new CellChangeColorAction(animCell, this, main.color.cpy().a(0), main.color.cpy().a(1), 20f)
                    );
                    UncCore.cellActions.add(
                        mainAnim
                    );
                  }
                }).size(80, 50);
              },
              new CellChangeColorAction(animCell, this, main.color.cpy().a(0), main.color.cpy().a(1), 20f)
          );
  
          UncCore.cellActions.add(mainAnim);
        }
      }).size(80, 50).margin(0);
    };
    rebuild.run();
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
