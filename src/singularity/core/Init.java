package singularity.core;

import mindustry.ui.fragments.OverlayFragment;
import singularity.Sgl;
import singularity.Singularity;
import singularity.core.ModConfig;
import singularity.type.SglCategory;
import singularity.ui.SglUI;
import singularity.ui.fragments.SglBlockInventoryFragment;
import singularity.world.meta.SglAttribute;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.type.Category;
import mindustry.ui.fragments.PlacementFragment;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.liquid.Conduit;
import universeCore.util.handler.FieldHandler;

import java.lang.reflect.Field;

import static mindustry.Vars.ui;

/**改动游戏原内容重初始化，用于对游戏已定义的实例进行操作*/
public class Init{
  public static void init(){
    SglUI.init();
    
    FieldHandler.setValue(OverlayFragment.class, "inv", Vars.control.input.frag, new SglBlockInventoryFragment());
    
    if(Sgl.config.loadInfo) Log.info("[Singularity] mod initialization is complete");
  }
  
  public static void handleBlockFrag(){
    try{
      Field toggler = PlacementFragment.class.getDeclaredField("toggler");
      toggler.setAccessible(true);
      Table togglerTable = (Table)toggler.get(ui.hudfrag.blockfrag);
    
      Table frame = (Table)togglerTable.getChildren().get(0);
      Table blockSelect = (Table)frame.getChildren().get(2);
      Table categories = (Table)frame.getChildren().get(3);
      
      Cell<?> pane = blockSelect.getCells().get(0);
      pane.height(240f);
      
      for(Category cat: SglCategory.values()){
        ImageButton button = ((ImageButton)categories.getChildren().find(e -> ("category-" + cat.name()).equals(e.name)));
        Drawable icon = new TextureRegionDrawable(Singularity.getModAtlas(cat.name()));
        if(button == null) continue;
        button.getStyle().imageUp = icon;
        button.resizeImage(icon.imageSize());
      }
    }catch(NoSuchFieldException | IllegalAccessException e){
      Log.err(e);
    }
  }
  
  /**内容重载器，用于对已加载的内容做出变更(或者覆盖)*/
  public static void reloadContent(){
    //为液体装卸器保证不从(常规)导管中提取液体
    for(Block target: Vars.content.blocks()){
      if(target instanceof Conduit) target.unloadable = false;
    }
    
    //为某些地板添加属性
    ((Floor)Blocks.basalt).attributes.set(SglAttribute.bitumen, 0.28f);
  }
}
