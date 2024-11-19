package singularity;

import arc.Core;
import arc.Events;
import arc.Settings;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.graphics.g2d.PixmapRegion;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.ui.Styles;
import mindustry.world.Block;
import singularity.core.ModConfig;
import singularity.core.ModsInteropAPI;
import singularity.core.UpdatePool;
import singularity.game.SglHint;
import singularity.game.researchs.ResearchManager;
import singularity.graphic.MathRenderer;
import singularity.graphic.ScreenSampler;
import singularity.graphic.SglDrawConst;
import singularity.graphic.SglShaders;
import singularity.ui.SglStyles;
import singularity.ui.SglUI;
import singularity.world.blocks.BytePackAssign;
import singularity.world.blocks.turrets.SglTurret;
import singularity.world.distribution.DistSupportContainerTable;
import singularity.world.unit.EMPHealthManager;
import universecore.UncCore;
import universecore.util.handler.ClassHandler;
import universecore.util.handler.FieldHandler;
import universecore.util.mods.ModGetter;
import universecore.util.mods.ModInfo;

import static arc.Core.keybinds;
import static arc.Core.settings;

public class Sgl{
  public static final String NL = System.lineSeparator();

  /**此mod内部名称*/
  public static final String modName = "singularity";
  /**此mod前置的内部名称*/
  public static final String libName = "universe-core";
  
  /**模组文件夹位置*/
  public static final Fi modDirectory = settings.getDataDirectory().child("mods");
  /**本模组的文件位置*/
  public static final ModInfo mod = ModGetter.getModWithName(modName);
  /**此模组的压缩包对象*/
  public static final ZipFi modFile = new ZipFi(mod.file);
  /**本模组前置的文件位置*/
  public static final ModInfo libMod = ModGetter.getModWithName(libName);
  /**本模组版本号*/
  public static final String modVersion = mod.version;
  /**本模组前置版本号*/
  public static final String libVersion = libMod.version;
  /**模组内配置文件存放位置*/
  public static final Fi internalConfigDir = modFile.child("config");
  /**模组数据文件夹*/
  public static final Fi dataDirectory = modDirectory.child("data").child(modName);
  /**模组配置文件夹*/
  public static final Fi configDirectory = modDirectory.child("config").child(modName);
  /**模组的mod_config.hjson配置文件*/
  public static final Fi configFile = configDirectory.child("mod_config.hjson");
  /**模组持久全局变量存储文件*/
  public static final Fi globalVars = configDirectory.child("global_vars.bin");
  /**模组持久全局变量备份文件*/
  public static final Fi globalVarsBackup = configDirectory.child("global_vars.bin.bak");

  //URIs
  public static final String telegram = "https://t.me/EB_wilson";
  public static final String facebook = "https://www.facebook.com/profile.php?id=100024490163405";
  public static final String qqGroup = "https://jq.qq.com/?_wv=1027&k=BTHaN7gd";
  public static final String telegramGroup = "https://t.me/+Dv_nTdi3e_k0ZDk9";
  public static final String modDevelopGroup = "https://jq.qq.com/?_wv=1027&k=vjybgqDG";
  public static final String githubUserAvatars = "https://avatars.githubusercontent.com/";
  public static final String githubProject = "https://github.com/EB-wilson/Singularity";
  public static final String afdian = "";
  public static final String patreon = "";
  public static final String libGithubProject = "https://github.com/EB-wilson/UniverseCore";
  public static final String discord = "";
  public static final String githubRawMaster = "https://raw.githubusercontent.com/EB-wilson/Singularity/master/";
  public static final String githubProjReleaseApi = "https://api.github.com/repos/EB-wilson/Singularity/releases/latest";
  public static final String publicInfo = githubRawMaster + "publicInfo/";


  public static final Team none = Team.get(255);
  
  /**模组配置存储器*/
  public static ModConfig config = new ModConfig();
  /**持久保存的全局变量集*/
  public static Settings globals;
  /**模组配置存储器*/
  public static ClassHandler classes;
  /**ui类存放对象*/
  public static SglUI ui;
  /**贡献者列表*/
  public static Contributors contributors;

  public static DistSupportContainerTable matrixContainers;

  public static EMPHealthManager empHealth;

  public static ResearchManager researches = new ResearchManager();
  public static ModsInteropAPI interopAPI = new ModsInteropAPI();

  public static void init(){
    //注册所有打包数据类型id
    BytePackAssign.assignAll();

    globals = new Settings(){
      {
        setAutosave(true);
        setDataDirectory(configDirectory);
      }

      @Override
      public Fi getSettingsFile() {
        return globalVars;
      }

      @Override
      public Fi getBackupFolder() {
        return configDirectory.child("global_backups");
      }

      @Override
      public Fi getBackupSettingsFile() {
        return globalVarsBackup;
      }

      @Override
      public synchronized void load() {
        try{
          loadValues();
        }catch(Throwable error){
          Log.err("Error in load: " + Strings.getStackTrace(error));
          if(errorHandler != null){
            if(!hasErrored) errorHandler.get(error);
          }else{
            throw error;
          }
          hasErrored = true;
        }
        loaded = true;
      }

      @Override
      public synchronized void forceSave() {
        if(!loaded) return;
        try{
          saveValues();
        }catch(Throwable error){
          Log.err("Error in forceSave to " + getSettingsFile() + ":\n" + Strings.getStackTrace(error));
          if(errorHandler != null){
            if(!hasErrored) errorHandler.get(error);
          }else{
            throw error;
          }
          hasErrored = true;
        }
        modified = false;
      }
    };

    globals.load();
    UpdatePool.receive("autosaveGlobal", globals::autosave);

    contributors = new Contributors();
    matrixContainers = new DistSupportContainerTable();
    empHealth = new EMPHealthManager();

    if (!Core.app.isHeadless()) {
      //设置屏幕采样器
      ScreenSampler.setup();
      //载入着色器
      SglShaders.load();
      //载入数学着色器
      MathRenderer.load();
      //加载绘制资源
      SglDrawConst.load();
      //载入风格
      SglStyles.load();

      ui = new SglUI();
      ui.init();

      int count = SglHint.all.size;
      if (Sgl.config.loadInfo) Log.info("[Singularity][INFO] loading sgl hints, hints count: " + count);
    }

    matrixContainers.setDefaultSupports();
    interopAPI.init();
    empHealth.init();
    researches.init();

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

    Events.on(EventType.ClientLoadEvent.class, e -> interopAPI.updateModels());

    for (Block block : Vars.content.blocks()) {
      if (block.minfo.mod != null && block.minfo.mod.name.equals(modName) && !(block instanceof SglTurret)){
        PixmapRegion image = Core.atlas.getPixmap(block.region);
        block.squareSprite = image.getA(0, 0) > 0.5f;
      }
    }
  }
}
