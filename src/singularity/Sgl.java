package singularity;

import arc.files.Fi;
import arc.files.ZipFi;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import singularity.core.*;
import singularity.graphic.SglDrawConst;
import singularity.ui.SglStyles;
import singularity.ui.SglUI;
import singularity.world.distribution.DistSupportContainerTable;
import singularity.world.blocks.distribute.IOPointBlock;
import universecore.util.handler.ClassHandler;
import universecore.util.mods.ModGetter;
import universecore.util.mods.ModInfo;

import static arc.Core.settings;

public class Sgl{
  static{
    UpdatePool.receive("sglUpdate", Sgl::update);
  }

  public static final String NL = System.lineSeparator();

  /**此mod内部名称*/
  public static final String modName = "singularity";
  /**此mod前置的内部名称*/
  public static final String libName = "universe-core";
  /**最大多方块结构尺寸限制*/
  public static final int maxStructureSize = 50;
  /**空白实体数组*/
  public static final Seq<Building> empty = new Seq<>(0);
  
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
  /**本模组前置版本号数值*/
  public static final long libVersionValue = parseVersion(libVersion);
  /**模组内配置文件存放位置*/
  public static final Fi internalConfigDir = modFile.child("config");
  /**模组数据文件夹*/
  public static final Fi dataDirectory = modDirectory.child("data").child(modName);
  /**模组配置文件夹*/
  public static final Fi configDirectory = modDirectory.child("config").child(modName);
  /**模组的mod_config.hjson配置文件*/
  public static final Fi configFile = configDirectory.child("mod_config.hjson");
  
  //URIs
  public static final String telegram = "https://t.me/EB_wilson";
  public static final String facebook = "https://www.facebook.com/profile.php?id=100024490163405";
  public static final String qqGroup1 = "https://jq.qq.com/?_wv=1027&k=BTHaN7gd";
  public static final String qqGroup2 = "";
  public static final String telegramGroup = "";
  public static final String modDevelopGroup = "https://jq.qq.com/?_wv=1027&k=vjybgqDG";
  public static final String githubUserAvatars = "https://avatars.githubusercontent.com/";
  public static final String githubProject = "https://github.com/EB-wilson/Singularity";
  public static final String libGithubProject = "https://github.com/EB-wilson/UniverseCore";
  public static final String discord = "";
  public static final String githubRawMaster = "https://raw.githubusercontent.com/EB-wilson/Singularity/master/";
  public static final String publicInfo = githubRawMaster + "publicInfo/";
  
  public static final String modAddress = "";
  public static final String libAddress = "";
  
  /**模组配置存储器*/
  public static ModConfig config = new ModConfig();
  /**模组配置存储器*/
  public static ClassHandler classes;
  /**ui类存放对象*/
  public static SglUI ui;
  /***/
  public static Contributors contributors;
  
  public static UpdateTiles updateTiles;
  
  public static IOPointBlock ioPoint;

  public static DistSupportContainerTable matrixContainers;
  
  public static void init(){
    //加载绘制资源
    SglDrawConst.load();
    //载入风格
    SglStyles.load();

    ui = new SglUI();
    contributors = new Contributors();
    
    updateTiles = new UpdateTiles();

    matrixContainers = new DistSupportContainerTable();

    matrixContainers.setDefaultSupports();
    ui.init();
  }
  
  public static void update(){
    if(Vars.state.isPaused()) return;

    if(Vars.state.isGame()) updateTiles.update();
  }
  
  public static long parseVersion(String version){
    long result = -1;
    if(version != null){
      String[] vars = version.split("\\.");
      result = 0;
      int priority = 10;
      for(String s: vars){
        long base = Long.parseLong(s);
        result += base*priority;
        priority /= 10;
      }
    }
    return result;
  }
}
