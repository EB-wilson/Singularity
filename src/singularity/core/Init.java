package singularity.core;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.liquid.Conduit;
import singularity.Sgl;
import singularity.graphic.SglDrawConst;
import singularity.world.meta.SglAttribute;
import universecore.util.handler.FieldHandler;

/**改动游戏原内容重初始化，用于对游戏已定义的实例进行操作*/
public class Init{
  public static void init(){
    if(Sgl.config.modReciprocal){
      //添加设置项入口
      Vars.ui.settings.shown(() -> {
        Table table = FieldHandler.getValueDefault(Vars.ui.settings, "menu");
        table.button(
            Core.bundle.get("settings.singularity"),
            SglDrawConst.sglIcon,
            Styles.flatt,
            32,
            () -> Sgl.ui.config.show()
        ).marginLeft(8).row();
      });
    }
  }
  
  /**内容重载，对已加载的内容做出变更(或者覆盖)*/
  public static void reloadContent(){
    //设置方块及地板属性
    Blocks.stone.attributes.set(SglAttribute.bitumen, 0.5f);

    for(Block target: Vars.content.blocks()){
      //为液体装卸器保证不从(常规)导管中提取液体
      if(target instanceof Conduit) target.unloadable = false;

      //禁用所有超速器
      if(target instanceof OverdriveProjector over){
        over.placeablePlayer = false;
        over.update = false;
        over.breakable = true;
      }
    }
  }
}
