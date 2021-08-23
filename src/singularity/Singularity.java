package singularity;

import arc.files.Fi;
import arc.files.ZipFi;
import arc.graphics.g2d.TextureRegion;
import arc.util.serialization.Jval;
import singularity.content.*;
import singularity.content.override.OverrideBlocks;
import singularity.type.SglContentType;
import arc.Core;
import arc.Events;
import arc.scene.ui.Image;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.TechTree;
import mindustry.core.ContentLoader;
import mindustry.ctype.*;
import mindustry.mod.Mod;
import mindustry.world.Block;
import universeCore.util.OverrideContentList;
import universeCore.util.UncContentType;
import universeCore.util.handler.ContentHandler;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import static mindustry.game.EventType.*;
import static singularity.ui.SglUI.*;

public class Singularity extends Mod{
  public boolean initialized = false;
  
  private final ContentList[] modContents = new ContentList[]{
    new SglItems(),//物品
    new SglLiquids(),//液体
    new Gases(),//气体
    new NuclearBlocks(),//核能方块
    new FactoryBlocks(),//工厂方块
    new TransportBlocks(),//物流方块
    new CollectBlocks(),//采集方块
  };
  
  private final OverrideContentList[] overrideContents = new OverrideContentList[]{
    new OverrideBlocks(),
  };
  
  public Singularity(){
    //加载模组配置数据
    ModConfig.load();
    
    Log.info("[Singularity] Singularity mod is loading!\nThanks for use this mod.\nauthor: EBwilson\nVisit the GitHub project about this mod: > " + Sgl.githubProject + " <");
    
    //载入新contentType
    for(UncContentType newType: SglContentType.allSglContentType){
      ContentHandler.addNewContentType(newType);
    }
    
    Events.on(ClientLoadEvent.class, e -> {
      mainMenu.show();
      
      Vars.ui.menuGroup.fill(t -> {
        //所以我选择在游戏logo上盖个透明的按钮(反正也能按)
        Image button = new Image(Singularity.getModAtlas("transparent"));
        button.clicked(mainMenu::show);
        t.top().add(button).size(1080, 170);
      });
    });
    
    Events.on(ContentInitEvent.class, e -> {
      Init.reloadContent();
    });
    
    Events.run(Trigger.update, () -> {
      if(initialized) Sgl.atmospheres.update();
    });
    
    Time.run(0, () -> {
      Events.on(WorldLoadEvent.class, event -> {
        Core.app.post(Init::handleBlockFrag);
        Sgl.atmospheres.loadAtmo();
      });
  
      Events.on(UnlockEvent.class, event -> {
        if(event.content instanceof Block){
          Init.handleBlockFrag();
        }
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
    if(ModConfig.loadInfo)Log.info("[Singularity] mod initialize finished");
  }
  
  @Override
  public void loadContent(){
    for(ContentList list: modContents){
      list.load();
    }
  
    for(OverrideContentList override: overrideContents){
      override.load();
    }
  
    try{
      Field arrayField = ContentLoader.class.getDeclaredField("content");
      arrayField.setAccessible(true);
      Object[] array = (Object[])arrayField.get(Vars.content);
  
      TechTree tree = new TechTree();
      Array.set(array, array.length - 1, tree);
      tree.load();
    }catch(NoSuchFieldException | IllegalAccessException e){
      Log.err(e);
    }
  
    if(ModConfig.debugMode) new DebugBlocks().load();
    if(ModConfig.loadInfo) Log.info("[Singularity] mod content loaded");
  }
  
  public static TextureRegion getModAtlas(String name){
    return Core.atlas.find(Sgl.modName + "-" + name);
  }
  
}