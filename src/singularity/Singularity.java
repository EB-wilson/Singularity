package singularity;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Floatc;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Streams;
import mindustry.Vars;
import mindustry.ctype.ContentList;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.ui.Bar;
import mindustry.ui.dialogs.BaseDialog;
import singularity.contents.*;
import singularity.contents.override.OverrideBlocks;
import singularity.contents.override.OverridePlanets;
import singularity.core.Init;
import singularity.type.SglCategory;
import singularity.type.SglContentType;
import universeCore.util.OverrideContentList;
import universeCore.util.UncContentType;
import universeCore.util.handler.ContentHandler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static mindustry.Vars.tmpDirectory;
import static mindustry.Vars.ui;
import static mindustry.game.EventType.*;
import static singularity.Sgl.*;

public class Singularity extends Mod{
  private static final long libVersionRequire = 11;
  
  private float downloadProgress;
  
  public boolean initialized = false;
  public boolean loaded = false;
  
  private ContentList[] modContents;
  private OverrideContentList[] overrideContents;
  
  @SuppressWarnings("unchecked")
  public Singularity() throws Exception{
    try{
      Class.forName("universeCore.UncCore", false, getClass().getClassLoader());
    }catch(ClassNotFoundException e){
      if(libFile == null || !libFile.exists() || libVersion < libVersionRequire){
        Time.run(0, () -> {
          BaseDialog tip = new BaseDialog(""){{
            cont.table(t -> {
              t.defaults().grow();
              t.table(Tex.pane, info -> {
                info.defaults().padTop(4);
                if(libVersion < libVersionRequire){
                  info.add(Core.bundle.get("infos.libVersionOld")).color(Color.crimson).top().padTop(10);
                }
                else info.add(Core.bundle.get("infos.libNotExist")).color(Color.crimson).top().padTop(10);
                info.row();
                info.add(Core.bundle.get("infos.downloadLib"));
                info.row();
                info.add(Core.bundle.get("infos.downLibTip1")).color(Color.gray);
                info.row();
                info.add(Core.bundle.get("infos.downLibTip2")).color(Color.gray).bottom().padBottom(10);
              }).height(215);
              t.row();
              t.table(buttons -> {
                buttons.defaults().grow();
                buttons.button(Core.bundle.get("misc.download"), () -> downloadLib());
                buttons.button(Core.bundle.get("infos.goLibPage") , () -> {
                  if(!Core.app.openURI(libGithubProject)){
                    ui.showErrorMessage("@linkfail");
                    Core.app.setClipboardText(libGithubProject);
                  }
                });
                buttons.row();
                buttons.button(Core.bundle.get("infos.openModDir"), () -> {
                  if(!Core.app.isAndroid()){
                    Core.app.openFolder(Vars.modDirectory.path());
                  }
                  else{
                    ui.showInfo(Core.bundle.get("infos.androidOpenFolder"));
                    Core.app.setClipboardText(Vars.modDirectory.path());
                  }
                });
                buttons.button(Core.bundle.get("misc.exit"), () -> Core.app.exit());
              }).padTop(10);
            }).size(340);
          }};
          tip.titleTable.clearChildren();
          tip.show();
        });
        
        return;
      }
      
      Log.info("dependence was not loaded, load it now");
      Log.info("you will receive an exception that threw by game, tell you the UniverseCore was load fail and skipped.\n" +
          "don't worry, this is expected, it will not affect your game");
      Method load = Mods.class.getDeclaredMethod("loadMod", Fi.class);
      load.setAccessible(true);
      Field f = Mods.class.getDeclaredField("mods");
      f.setAccessible(true);
      Seq<Mods.LoadedMod> mods = (Seq<Mods.LoadedMod>)f.get(Vars.mods);
      mods.add((Mods.LoadedMod)load.invoke(Vars.mods, libFile));
    }
  
    modContents = new ContentList[]{
        new SglItems(),//物品
        new SglLiquids(),//液体
        new Gases(),//气体
        new Environments(),//环境块
        new NuclearBlocks(),//核能方块
        new CrafterBlocks(),//工厂方块
        new GasBlocks(),//气体相关方块
        new LiquidBlocks(),//物流方块
        new CollectBlocks(),//采集方块
        new DefenceBlocks(),//防御方块
        new Reactions(),//化学反应
      
        new SglTechThree(),//科技树
    };
    
    overrideContents = new OverrideContentList[]{
        new OverrideBlocks(),
        new OverridePlanets(),
    };
    
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
    
    loaded = true;
  }
  
  @Override
  public void init(){
    if(loaded){
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
      if(Sgl.config.loadInfo) Log.info("[Singularity] mod initialize finished");
    }
  }
  
  @Override
  public void loadContent(){
    if(modContents != null) for(ContentList list: modContents){
      list.load();
    }
  
    if(overrideContents != null) for(OverrideContentList override: overrideContents){
      override.load();
    }
  
    new DebugBlocks().load();
    if(Sgl.config.loadInfo && loaded) Log.info("[Singularity] mod content loaded");
  }
  
  public static TextureRegion getModAtlas(String name){
    return Core.atlas.find(Sgl.modName + "-" + name);
  }
  
  private void downloadLib(){
    InputStream[] stream = new InputStream[1];
    
    Http.get(libAddress, request -> {
      stream[0] = request.getResultAsStream();
      
      Fi temp = tmpDirectory.child("UniverseCore.jar");
      Fi file = tmpDirectory.child("UniverseCore.jar");
      long length = request.getContentLength();
      Floatc cons = length <= 0 ? f -> {} : p -> downloadProgress = p;
      
      Streams.copyProgress(stream[0], temp.write(false), length, 4096, cons);
      if(libFile != null && libFile.exists()) libFile.delete();
      temp.moveTo(file);
      
      new BaseDialog(""){{
        titleTable.clearChildren();
        
        cont.add(Core.bundle.get("infos.updatedRestart"));
        cont.row();
        cont.button(Core.bundle.get("misc.sure"), () -> Core.app.exit()).fill();
      }}.show();
    }, e -> {
      if(!(e instanceof IOException)){
        StringBuilder error = new StringBuilder();
        for(StackTraceElement ele: e.getStackTrace()){
          error.append(ele);
        }
        ui.showErrorMessage(Core.bundle.get("warning.downloadFailed") + "\n" + error);
      }
    });
  
    new BaseDialog(""){
      {
        titleTable.clearChildren();
      
        cont.table(Tex.pane, t -> {
          t.add(Core.bundle.get("misc.downloading")).top().padTop(10).get();
          t.row();
          t.add(new Bar(
              () -> Strings.autoFixed(downloadProgress, 1) + "%",
              () -> Pal.accent,
              () -> downloadProgress
          )).growX().height(30).pad(4);
        }).size(320, 175);
        cont.row();
        cont.button(Core.bundle.get("misc.cancel"), () -> {
          hide();
          try{
            if(stream[0] != null) stream[0].close();
          }catch(IOException e){
            Log.err(e);
          }
        }).fill();
      }
    }.show();
  }
}