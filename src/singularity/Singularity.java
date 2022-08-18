package singularity;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.mod.Mod;
import singularity.contents.*;
import singularity.contents.override.OverrideBlocks;
import singularity.contents.override.OverridePlanets;
import singularity.core.Init;
import singularity.type.SglCategory;
import singularity.type.SglContentType;
import singularity.world.meta.SglAttribute;
import universecore.UncCore;
import universecore.annotations.Annotations;
import universecore.util.OverrideContentList;

import static mindustry.game.EventType.*;

@Annotations.ImportUNC(requireVersion = 12)
public class Singularity extends Mod{
  private static final ContentList[] modContents = new ContentList[]{
      new SglItems(),//物品
      new SglLiquids(),//液体
      new Gases(),//气体
      new Environments(),//环境块
      new PowerBlocks(),//电力方块
      new NuclearBlocks(),//核能方块
      new CrafterBlocks(),//工厂方块
      new GasBlocks(),//气体相关方块
      new LiquidBlocks(),//物流方块
      new CollectBlocks(),//采集方块
      new DistributeBlocks(),//物流运输方块
      new DefenceBlocks(),//防御方块
      new Reactions(),//化学反应

      new OtherContents(),//其他内容

      new SglTechThree(),//科技树
  };
  
  private static final OverrideContentList[] overrideContents = new OverrideContentList[]{
      new OverrideBlocks(),
      new OverridePlanets(),
  };
  
  public boolean initialized = false;
  
  public Singularity(){
    //加载模组配置数据
    Sgl.config.load();
    Sgl.classes = UncCore.classes.newInstance(Singularity.class);

    //加载属性类型
    SglAttribute.load();
    //加载方块类型
    SglCategory.load();
    //载入所有新内容类型
    SglContentType.load();
    
    Log.info(
       """
       [Singularity] Singularity mod is loading!
       Thanks for your play.
       author: EBwilson
       Visit the GitHub project about this mod: >
       """ + Sgl.githubProject + " <");
    
    Events.on(ClientLoadEvent.class, e -> {
      Sgl.ui.mainMenu.show();
    });
    
    Events.on(ContentInitEvent.class, e -> {
      Init.reloadContent();
    });
    
    Events.on(SaveWriteEvent.class, e -> {
      Sgl.atmospheres.write();
    });
    
    Events.on(ResetEvent.class, e -> {
      Sgl.updateTiles.clear();
    });
  
    Time.run(0, () -> {
      Events.on(WorldLoadEvent.class, event -> {
        Sgl.atmospheres.loadAtmo();
      });
    });
  }
  
  @Override
  public void init(){
    //加载全局变量
    Sgl.init();

    //重加载mod元信息
    Vars.mods.locateMod(Sgl.modName).meta.displayName = Core.bundle.get("mod.name");
    Vars.mods.locateMod(Sgl.modName).meta.author = Core.bundle.get("mod.author");
    Vars.mods.locateMod(Sgl.modName).meta.version = Core.bundle.get("mod.version");

    //游戏本体更改初始化
    Init.init();
    initialized = true;
    if(Sgl.config.loadInfo) Log.info("[Singularity] mod initialize finished");
  }
  
  @Override
  public void loadContent(){
    for(ContentList list: Singularity.modContents){
      list.load();
    }
  
    for(OverrideContentList override: Singularity.overrideContents){
      override.load();
    }
  
    new DebugBlocks().load();
    Log.info("[Singularity] mod content loaded");
  }
  
  public static TextureRegion getModAtlas(String name){
    return Core.atlas.find(Sgl.modName + "-" + name);
  }
}