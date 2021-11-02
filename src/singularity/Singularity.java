package singularity;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ctype.ContentList;
import mindustry.mod.Mod;
import singularity.contents.*;
import singularity.contents.override.OverrideBlocks;
import singularity.contents.SglTechThree;
import singularity.core.Init;
import singularity.type.SglCategory;
import singularity.type.SglContentType;
import universeCore.util.OverrideContentList;
import universeCore.util.UncContentType;
import universeCore.util.handler.ContentHandler;

import static mindustry.game.EventType.*;

public class Singularity extends Mod{
  public boolean initialized = false;
  
  private final ContentList[] modContents = new ContentList[]{
      new SglItems(),//物品
      new SglLiquids(),//液体
      new Gases(),//气体
      new Environments(),//环境块
      new NuclearBlocks(),//核能方块
      new CrafterBlocks(),//工厂方块
      new GasBlocks(),//气体相关方块
      new TransportBlocks(),//物流方块
      new CollectBlocks(),//采集方块w
      new Reactions(),//化学反应
      
      new SglTechThree(),//科技树
  };
  
  private final OverrideContentList[] overrideContents = new OverrideContentList[]{
    new OverrideBlocks(),
  };
  
  public Singularity(){
    //加载模组配置数据
    Sgl.config.load();
    
    //加载方块类型
    SglCategory.load();
    //载入所有新内容类型
    SglContentType.load();
    
    Log.info("[Singularity] Singularity mod is loading!\nThanks for use this mod.\nauthor: EBwilson\nVisit the GitHub project about this mod: > " + Sgl.githubProject + " <");
    
    //载入新contentType
    for(UncContentType newType: SglContentType.allSglContentType){
      ContentHandler.addNewContentType(newType);
    }
    
    Events.on(ClientLoadEvent.class, e -> {
      Sgl.ui.mainMenu.show();
    });
    
    Events.on(ContentInitEvent.class, e -> {
      Init.reloadContent();
    });
    
    Events.run(Trigger.update, () -> {
      if(initialized) Sgl.update();
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
    Vars.mods.locateMod(Sgl.modName).meta.description = Core.bundle.get("mod.description");
    Vars.mods.locateMod(Sgl.modName).meta.author = Core.bundle.get("mod.author");
    Vars.mods.locateMod(Sgl.modName).meta.version = Core.bundle.get("mod.version");
    
    //游戏本体更改初始化
    Init.init();
    
    initialized = true;
    if(Sgl.config.loadInfo)Log.info("[Singularity] mod initialize finished");
  }
  
  @Override
  public void loadContent(){
    for(ContentList list: modContents){
      list.load();
    }
  
    for(OverrideContentList override: overrideContents){
      override.load();
    }
  
    if(Sgl.config.debugMode) new DebugBlocks().load();
    if(Sgl.config.loadInfo) Log.info("[Singularity] mod content loaded");
  }
  
  public static TextureRegion getModAtlas(String name){
    return Core.atlas.find(Sgl.modName + "-" + name);
  }
  
}