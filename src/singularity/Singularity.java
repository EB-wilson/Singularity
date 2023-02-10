package singularity;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.mod.Mod;
import singularity.contents.*;
import singularity.contents.override.OverrideBlocks;
import singularity.contents.override.OverridePlanets;
import singularity.contents.override.OverrideTechThree;
import singularity.core.Init;
import singularity.type.SglCategory;
import singularity.type.SglContentType;
import singularity.world.meta.SglAttribute;
import universecore.UncCore;
import universecore.annotations.Annotations;
import universecore.util.OverrideContentList;

import static mindustry.game.EventType.*;

@Annotations.ImportUNC(requireVersion = "1.6.2")
public class Singularity extends Mod{
  private static final ContentList[] modContents = new ContentList[]{
      new OtherContents(),//其他内容

      new SglItems(),//物品
      new SglLiquids(),//液体
      new Environments(),//环境块
      new PowerBlocks(),//电力方块
      new NuclearBlocks(),//核能方块
      new CrafterBlocks(),//工厂方块
      new LiquidBlocks(),//物流方块
      new ProductBlocks(),//采集方块
      new DistributeBlocks(),//物流运输方块
      new SglTurrets(),//炮台
      new SglUnits(),//单位相关内容（单位、工厂）
      new DefenceBlocks(),//防御方块

      new SglTechThree(),//科技树
  };
  
  private static final OverrideContentList[] overrideContents = new OverrideContentList[]{
      new OverrideBlocks(),
      new OverridePlanets(),
      new OverrideTechThree(),
  };
  
  public boolean initialized = false;
  
  public Singularity(){
    //加载模组配置数据
    Sgl.config.load();
    Sgl.classes = UncCore.classes.newInstance(Singularity.class);

    Log.info(
       """
       [Singularity] Singularity mod is loading!
       Thanks for your play.
       author: EBwilson
       Visit the GitHub project about this mod:
       >\040""" + Sgl.githubProject + " <"
    );

    if(Sgl.config.showModMenuWenLaunch){
      Events.on(ClientLoadEvent.class, e -> {
        Sgl.ui.mainMenu.show();
      });
    }

    if(Sgl.config.modReciprocal){
      Events.on(ContentInitEvent.class, e -> {
        Init.reloadContent();
      });
    }

    Events.on(ResetEvent.class, e -> {
      Sgl.updateTiles.clear();
    });

    if(Sgl.config.debugMode) Events.on(WorldLoadEvent.class, e -> Vars.state.rules.infiniteResources = true);
  }
  
  @Override
  public void init(){
    //加载全局变量
    Sgl.init();

    //游戏本体更改初始化
    if(Sgl.config.modReciprocal) Init.init();

    initialized = true;

    Sgl.classes.finishGenerate();
    if(Sgl.config.loadInfo) Log.info("[Singularity] mod initialize finished");
  }
  
  @Override
  public void loadContent(){
    //加载属性类型
    SglAttribute.load();
    //加载方块类型
    SglCategory.load();
    //载入所有新内容类型
    SglContentType.load();

    for(ContentList list: Singularity.modContents){
      list.load();
    }

    if(Sgl.config.modReciprocal){
      for(OverrideContentList override: Singularity.overrideContents){
        override.load();
      }
    }

    if(Sgl.config.debugMode){
      new DebugBlocks().load();

      for (ContentType type : ContentType.all) {
        for (Content content : Vars.content.getBy(type)) {
          if (content instanceof UnlockableContent uc){
            uc.alwaysUnlocked = true;
          }
        }
      }
    }

    if(Sgl.config.loadInfo) Log.info("[Singularity] mod content load finished");
  }
  
  public static TextureRegion getModAtlas(String name){
    return Core.atlas.find(Sgl.modName + "-" + name);
  }

  public static Fi getInternalFile(String path){
    return Sgl.modFile.child(path);
  }
}