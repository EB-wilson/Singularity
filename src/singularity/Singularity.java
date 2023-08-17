package singularity;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.Drawable;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;
import singularity.contents.*;
import singularity.contents.override.OverrideBlocks;
import singularity.contents.override.OverridePlanets;
import singularity.contents.override.OverrideTechThree;
import singularity.core.Init;
import singularity.type.SglCategory;
import singularity.type.SglContentType;
import singularity.ui.SglStyles;
import singularity.world.meta.SglAttribute;
import universecore.UncCore;
import universecore.annotations.Annotations;
import universecore.ui.elements.markdown.Markdown;
import universecore.util.OverrideContentList;

import static mindustry.game.EventType.*;

@Annotations.ImportUNC(requireVersion = "1.8.2")
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
       
       Visit the GitHub project about this mod:
       >\040""" + Sgl.githubProject + " <"
    );

    if(Sgl.config.showModMenuWenLaunch){
      Events.on(ClientLoadEvent.class, e -> {
        Sgl.ui.mainMenu.show();

        new BaseDialog(""){{
          addCloseButton();

          cont.pane(t -> t.add(new Markdown("""
              
              ![image](singularity-logo)
                            
              ---
              >mod目前尚处于开发阶段，内容不齐全，尽可能不要在发布前游玩此mod
              ## Singularity
              
              对`mindustry`原有的 $内容感到乏味了么$ ？奇点mod为mindustry游戏本体提供 _了大量全新_ 的内容，从工业到防线，全新的工厂，全新的防御武器，以及全新的机械。
              
              mod的内容有很多，~~包括但不限于令~~人耳目一新的++新型工业架构和中子++能量系统，强大 _的矩阵物流网_ 络， _以及一些你从未听闻过的全新机制_ ，各种意义上这是一个十分庞大的mod，这能给你带来的游戏乐趣远不止增长游戏流程那么简单。
              
              > 本mod需要一个前置mod：[__Universe Core__](https://github.com/EB-wilson/UniverseCore)，当然你不必担心，若在你安装了本mod后启动游戏会检查前置mod是否存在，如果你当前并没有安装前置mod，那么弹窗会引导你进行下载和安装。
              > > intest
              > > > in intest
              
              | 左对齐   |     右对齐    | 居中对齐 | __长表头测试12345678901234567890__ |
              |:------|-----------:|:----------------:|:----------------------:|
              | a     |          s |        p         |           d            |
              | a asd |          s |        p         |       $awdaswd d$        |
              | a ad  |          s |      adwawp      |           d            |
              | a     | adsa     s |        p         |           d            |
              
              ```
              code block test
              private static class DrawSUrl extends DrawStr implements ActivityDrawer{
                TextButton openUrl;
            
                static DrawSUrl get(Markdown owner, String str, Font font, String openUrl, Color color, float ox, float oy, float scl, Drawable background){
                  DrawSUrl res = Pools.obtain(DrawSUrl.class, DrawSUrl::new);
                  res.parent = owner;
                  res.text = str;//中文测试
                  res.font = font;
                  res.openUrl = new TextButton(str, new TextButton.TextButtonStyle(Styles.nonet){{ fontColor = color; }}){{
                    clicked(() -> Core.app.openURI(openUrl));
                    label.setScale(scl);
                    label.setWrap(false);
                  }};
                  res.offsetX = ox;
                  res.offsetY = oy;
                  res.scl = scl;
                  res.color = color;
                  res.drawable = background;
            
                  return res;
                }
            
                @Override
                void draw() {}
            
                @Override
                public Element getElem() {
                  return openUrl;
                }
            
                @Override
                public float width() {
                  return openUrl.getLabel().getPrefWidth();
                }
            
                @Override
                public float height() {
                  return openUrl.getLabel().getPrefHeight();
                }
            
                @Override
                public void reset() {
                  super.reset();
                  openUrl = null;
                }
              }
              ```
              """, SglStyles.defaultMD, true)).growX()).scrollX(false).grow().padLeft(200).padRight(200);
        }}.show();
      });
    }

    if(Sgl.config.modReciprocalContent){
      Events.on(ContentInitEvent.class, e -> {
        Init.reloadContent();
      });
    }

    if(Sgl.config.debugMode) Events.on(WorldLoadEvent.class, e -> Vars.state.rules.infiniteResources = true);
  }
  
  @Override
  public void init(){
    //游戏本体更改初始化
    Init.init();

    //加载全局变量
    Sgl.init();

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

    if(Sgl.config.modReciprocalContent){
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

  public static <T extends Drawable> T getModDrawable(String name){
    return Core.atlas.getDrawable(Sgl.modName + "-" + name);
  }

  public static Fi getInternalFile(String path){
    return Sgl.modFile.child(path);
  }
}