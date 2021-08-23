package singularity;

import arc.files.Fi;
import arc.files.ZipFi;
import arc.struct.Seq;
import arc.util.serialization.Jval;
import mindustry.gen.Building;
import universeCore.util.Ini;

import static arc.Core.settings;

public class Statics{
  //静态常量
  /**此mod内部名称*/
  public static final String modName = "singularity";
  /**最大多方块结构尺寸限制*/
  public static final int maxStructSizeLimit = 50;
  //public static final Constructs globalConstructs = new Constructs();
  
  /**空白实体数组*/
  public static final Seq<Building> empty = new Seq<>(0);
  
  //文件路径
  /**本模组的文件位置*/
  public static final Fi modFile = getModFile();
  /**模组数据文件夹*/
  public static final Fi dataDirectory = modFile.child("data").child(modName);
  /**模组内配置文件存放位置*/
  public static final Fi internalConfigDir = modFile.child("config");
  /**模组配置文件夹*/
  public static final Fi configDirectory = settings.getDataDirectory().child("mods").child("config").child(modName);
  /**模组的mod_config.ini配置文件*/
  public static final Fi configFile = configDirectory.child("mod_config.ini");
  
  private static Fi getModFile(){
    Fi[] modsFiles = settings.getDataDirectory().child("mods").list();
    Fi temp = null;
  
    for(Fi file : modsFiles){
      if(file.isDirectory()) continue;
      Fi zipped = new ZipFi(file);
      Fi modManifest = zipped.child("mod.hjson");
      if(modManifest.exists()){
        String name = Jval.read(modManifest.readString()).get("name").toString();
        if(name.equals("singularity")) temp = zipped;
      }
    }
    
    return temp;
  }
}
