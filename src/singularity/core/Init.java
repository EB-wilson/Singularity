package singularity.core;

import arc.util.Log;
import mindustry.Vars;
import mindustry.world.Block;
import mindustry.world.blocks.liquid.Conduit;
import singularity.Sgl;
import singularity.ui.fragments.override.SglMenuFrag;

/**改动游戏原内容重初始化，用于对游戏已定义的实例进行操作*/
public class Init{
  public static void init(){
    Vars.ui.menufrag = new SglMenuFrag();
    Vars.ui.menuGroup.clearChildren();
    Vars.ui.menufrag.build(Vars.ui.menuGroup);
    
    if(Sgl.config.loadInfo) Log.info("[Singularity] mod initialization is complete");
  }
  
  /**内容重载器，用于对已加载的内容做出变更(或者覆盖)*/
  public static void reloadContent(){
    //为液体装卸器保证不从(常规)导管中提取液体
    for(Block target: Vars.content.blocks()){
      if(target instanceof Conduit) target.unloadable = false;
    }
  }
}
