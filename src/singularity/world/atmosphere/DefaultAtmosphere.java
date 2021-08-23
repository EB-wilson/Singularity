package singularity.world.atmosphere;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.Vars;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.type.SglContentType;
import universeCore.util.Ini;

import java.util.Arrays;
import java.util.regex.Pattern;

public class DefaultAtmosphere{
  public final static Ini configure;
  static{
    Fi atmosphereConfigFile = Sgl.configDirectory.child("atmosphere.ini");
    
    if(!atmosphereConfigFile.exists()){
      Sgl.internalConfigDir.child("atmosphere.ini").copyTo(atmosphereConfigFile);
    }
    configure = new Ini(atmosphereConfigFile);
  }
  
  public static final DefaultAtmosphere defaults = new DefaultAtmosphere(null);
  
  public final Atmosphere parent;
  
  public float[] ingredients = new float[Vars.content.getBy(SglContentType.gas.value).size];
  public float[] recoverCoeff = new float[Vars.content.getBy(SglContentType.gas.value).size];
  
  public float baseTotal;
  public float basePressure;
  public float defaultRecoverCoeff;
  
  public DefaultAtmosphere(Atmosphere parent){
    this.parent = parent;
    if(parent != null) Arrays.fill(ingredients, -1);
    Arrays.fill(recoverCoeff, -1);
    
    if(parent == null){
      loadData("defaults");
    }
    else if(parent.attach == null){
      parent.defaults = defaults;
    }
    else loadData(parent.attach.name);
  }
  
  public void loadData(String section){
    Pattern mark = Pattern.compile("\\[(\\w+\\*\\d+.*\\d*\\s*,?\\s*)+]");
    ObjectMap<String, String> map = configure.getSection(section);
    if(map != null){
      if(section.equals("defaults")){
        baseTotal = Float.parseFloat(map.get("baseTotal"));
        basePressure = Float.parseFloat(map.get("basePressure"));
        defaultRecoverCoeff = Float.parseFloat(map.get("defaultRecoverCoeff"));
      }
      else{
        baseTotal = map.get("baseTotal") != null? Float.parseFloat(map.get("baseTotal")): defaults.baseTotal;
        basePressure = map.get("basePressure") != null? Float.parseFloat(map.get("basePressure")): defaults.basePressure;
        defaultRecoverCoeff = map.get("defaultRecoverCoeff") != null? Float.parseFloat(map.get("defaultRecoverCoeff")): defaults.defaultRecoverCoeff;
      }
      
      String ingredientsStr = map.get("ingredients", "");
      String recoverCoeffStr = map.get("recoverCoeff", "");
      
      if(mark.matcher(ingredientsStr).matches()){
        String[] ingStrs = ingredientsStr.replace("[", "").replace("]", "").split(",");
        
        for(String str : ingStrs){
          String[] data = str.split("\\*");
          if(data.length != 2) continue;
          Gas gas = Vars.content.getByName(SglContentType.gas.value, Sgl.modName + "-" + data[0].strip());
          ingredients[gas.id] = Float.parseFloat(data[1].strip())*baseTotal;
        }
      }
      
      if(mark.matcher(recoverCoeffStr).matches()){
        String[] recCoeffStrs = recoverCoeffStr.replace("[", "").replace("]", "").split(",");
  
        for(String str : recCoeffStrs){
          String[] data = str.split("\\*");
          if(data.length != 2) continue;
          Gas gas = Vars.content.getByName(SglContentType.gas.value, Sgl.modName + "-" + data[0].strip());
          recoverCoeff[gas.id] = Float.parseFloat(data[1].strip());
        }
      }
    }
    else{
      baseTotal = defaults.baseTotal;
      basePressure = defaults.basePressure;
      defaultRecoverCoeff = defaults.defaultRecoverCoeff;
    }
  
    if(!section.equals("defaults")) for(int i = 0; i < ingredients.length; i++){
      if(ingredients[i] == -1) ingredients[i] = defaults.ingredients[i];
      if(recoverCoeff[i] == -1) recoverCoeff[i] = defaults.recoverCoeff[i] == -1? defaultRecoverCoeff: defaults.recoverCoeff[i];
    }
  }
  
  @Override
  public String toString(){
    return "baseTotal: " + baseTotal + ", " +
      "basePressure: " + basePressure + ", " +
      "defaultRecoverCoeff: " + defaultRecoverCoeff + ", " +
      "ingredients: " + Arrays.toString(ingredients) + ", " +
      "recoverCoeff: " + Arrays.toString(recoverCoeff);
  }
}
