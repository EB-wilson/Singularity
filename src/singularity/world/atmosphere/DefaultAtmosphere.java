package singularity.world.atmosphere;

import arc.files.Fi;
import arc.struct.ObjectMap;
import mindustry.Vars;
import singularity.Statics;
import singularity.type.SglContentType;
import universeCore.util.Ini;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

public class DefaultAtmosphere{
  public final static Ini configure;
  static{
    Fi atmosphereConfigFile = Statics.configDirectory.child("atmosphere.ini");
    
    if(!atmosphereConfigFile.exists()){
      Statics.internalConfigDir.child("atmosphere.ini").copyTo(atmosphereConfigFile);
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
    if(parent != null){
      Arrays.fill(ingredients, -1);
      Arrays.fill(recoverCoeff, -1);
    }
    
    loadData(parent == null? "defaults" :parent.attach.name);
  }
  
  public void loadData(String section){
    Pattern mark = Pattern.compile("\\[\\w+]");
    ObjectMap<String, String> map = configure.getSection(section);
    if(map != null){
      baseTotal = Objects.requireNonNullElse(Float.parseFloat(map.get("baseTotal")), defaults.baseTotal);
      basePressure = Objects.requireNonNullElse(Float.parseFloat(map.get("basePressure")), defaults.basePressure);
      defaultRecoverCoeff = Objects.requireNonNullElse(Float.parseFloat(map.get("defaultRecoverCoeff")), defaults.defaultRecoverCoeff);
      
      String ingredientsStr = map.get("ingredients", "");
      String recoverCoeffStr = map.get("recoverCoeff", "");
  
      if(mark.matcher(ingredientsStr).matches() && mark.matcher(recoverCoeffStr).matches()){
        String[] ingStrs = ingredientsStr.replace("[", "").replace("]", "").split(",");
        String[] recCoeffStrs = recoverCoeffStr.replace("[", "").replace("]", "").split(",");
        for(String str : ingStrs){
          String[] data = str.split("\\*");
          if(data.length != 2) continue;
          ingredients[Vars.content.getByName(SglContentType.gas.value, data[0].strip()).id] = Float.parseFloat(data[1].strip())*baseTotal;
        }
    
        for(String str : recCoeffStrs){
          String[] data = str.split("\\*");
          if(data.length != 2) continue;
          recoverCoeff[Vars.content.getByName(SglContentType.gas.value, data[0].strip()).id] = Float.parseFloat(data[1].strip());
        }
      }
    }
    else{
      baseTotal = defaults.baseTotal;
      basePressure = defaults.basePressure;
      defaultRecoverCoeff = defaults.defaultRecoverCoeff;
    }
  
    for(int i = 0; i < ingredients.length; i++){
      if(ingredients[i] == -1) ingredients[i] = defaults.ingredients[i];
      if(recoverCoeff[i] == -1 && !section.equals("defaults")) recoverCoeff[i] = defaults.recoverCoeff[i] == -1? defaultRecoverCoeff: defaults.recoverCoeff[i];
    }
  }
}
