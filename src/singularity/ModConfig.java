package singularity;

import arc.util.Log;
import universeCore.util.Ini;

import java.lang.reflect.Field;

/**模组配置的加载类，包含了所有配置项的静态变量
 * 从配置文件config.ini加载，必须在init事件之后进行*/
public class ModConfig{
  //base基础设置
  public static boolean dataBaseUnlocked;
  public static String language;
  
  //游戏设置
  public static boolean energyTransferLoss;
  public static boolean overpressureExplosion;
  
  //Advanced config/高级设置
  public static boolean modReciprocal;
  
  //debug
  public static boolean loadInfo;
  public static boolean debugMode;
  
  public static void load(){
    if(!Sgl.configFile.exists()){
      Sgl.internalConfigDir.child("mod_config.ini").copyTo(Sgl.configFile);
      Log.info("Configuration file is not exist, copying the default configuration");
    }
    
    Field[] configs = ModConfig.class.getFields();
    StringBuilder results = new StringBuilder();
    Ini config = new Ini(Sgl.configFile);
    for(Field cfg: configs){
      String temp = config.get(cfg.getName())[0];
      Class<?> type = cfg.getType();
      results.append("  ").append(cfg.getName()).append(" = ").append(temp).append(";\n");
      try{
        if(type == int.class){
          cfg.set(null, Integer.valueOf(temp));
        }
        else if(type == boolean.class){
          cfg.set(null, Boolean.valueOf(temp));
        }
        else if(type == byte.class){
          cfg.set(null, Byte.valueOf(temp));
        }
        else if(type == float.class){
          cfg.set(null, Float.valueOf(temp));
        }
        else cfg.set(null, temp);
      }
      catch(IllegalArgumentException | IllegalAccessException e){
        Log.err(e);
      }
    }
    if(loadInfo) Log.info("Mod config loaded! The config data:[\n" + results + "]");
  }
}
