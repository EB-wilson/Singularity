package singularity.core;

import arc.util.Log;
import singularity.Sgl;
import universeCore.util.ini.Ini;
import universeCore.util.ini.IniFile;
import universeCore.util.ini.IniTypes;

import java.lang.reflect.Field;

public class ModConfig{
  //base基础设置
  public boolean dataBaseUnlocked;
  public String language;
  
  //游戏设置
  public boolean energyTransferLoss;
  public boolean overpressureExplosion;
  
  //Advanced config/高级设置
  public boolean modReciprocal;
  
  //debug
  public boolean loadInfo;
  public boolean debugMode;
  
  public void load(){
    if(!Sgl.configFile.exists()){
      Sgl.internalConfigDir.child("mod_config.ini").copyTo(Sgl.configFile);
      Log.info("Configuration file is not exist, copying the default configuration");
    }
    
    Field[] configs = ModConfig.class.getFields();
    StringBuilder results = new StringBuilder();
    Ini config = new IniFile(Sgl.configFile).object;
    for(Field cfg: configs){
      IniTypes.IniObject temp = config.get(cfg.getName());
      results.append("  ").append(cfg.getName()).append(" = ").append(temp.get().toString()).append(";\n");
      try{
        cfg.set(null, temp.get());
      }
      catch(IllegalArgumentException | IllegalAccessException e){
        Log.err(e);
      }
    }
    if(loadInfo) Log.info("Mod config loaded! The config data:[\n" + results + "]");
  }
}
