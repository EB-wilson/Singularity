package singularity.world.atmosphere;

import arc.files.Fi;
import mindustry.Vars;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.type.SglContentType;
import universeCore.util.ini.Ini;
import universeCore.util.ini.IniFile;
import universeCore.util.ini.IniTypes;

import java.util.Arrays;

public class DefaultAtmosphere{
  public final static Ini configure;
  static{
    Fi atmosphereConfigFile = Sgl.configDirectory.child("atmosphere.ini");
    
    if(!atmosphereConfigFile.exists()){
      Sgl.internalConfigDir.child("atmosphere.ini").copyTo(atmosphereConfigFile);
    }
    configure = new IniFile(atmosphereConfigFile).parseIni();
  }
  
  public static final DefaultAtmosphere defaults = new DefaultAtmosphere(null);
  
  public final Atmosphere parent;
  
  public float[] ingredientsBase = new float[Vars.content.getBy(SglContentType.gas.value).size];
  public float[] ingredients = new float[Vars.content.getBy(SglContentType.gas.value).size];
  public float[] recoverCoeff = new float[Vars.content.getBy(SglContentType.gas.value).size];
  
  public float baseTotal;
  public float basePressure;
  public float defaultRecoverCoeff;
  
  public DefaultAtmosphere(Atmosphere parent){
    this.parent = parent;
    Arrays.fill(ingredientsBase, parent == null? 1: -1);
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
    Ini.IniSection map = configure.getSection(section);
    if(map != null){
      if(section.equals("defaults")){
        baseTotal = ((IniTypes.IniNumber)map.get("baseTotal")).floatValue();
        basePressure = ((IniTypes.IniNumber)map.get("basePressure")).floatValue();
        defaultRecoverCoeff = ((IniTypes.IniNumber)map.get("defaultRecoverCoeff")).floatValue();
      }
      else{
        baseTotal = map.get("baseTotal") != null? ((IniTypes.IniNumber)map.get("baseTotal")).floatValue(): defaults.baseTotal;
        basePressure = map.get("basePressure") != null? ((IniTypes.IniNumber)map.get("basePressure")).floatValue(): defaults.basePressure;
        defaultRecoverCoeff = map.get("defaultRecoverCoeff") != null? ((IniTypes.IniNumber)map.get("defaultRecoverCoeff")).floatValue(): defaults.defaultRecoverCoeff;
      }
      
      IniTypes.IniMap ingre = (IniTypes.IniMap) map.get("ingredients");
      IniTypes.IniMap recov = (IniTypes.IniMap) map.get("recoverCoeff");
  
      ingre.get().forEach((k, v) -> {
        Gas gas = Vars.content.getByName(SglContentType.gas.value, Sgl.modName + "-" + k);
        ingredientsBase[gas.id] = ((IniTypes.IniNumber)v).floatValue();
      });
      
      recov.get().forEach((k, v) -> {
        Gas gas = Vars.content.getByName(SglContentType.gas.value, Sgl.modName + "-" + k);
        recoverCoeff[gas.id] = ((IniTypes.IniNumber)v).floatValue();
      });
    }
    else{
      baseTotal = defaults.baseTotal;
      basePressure = defaults.basePressure;
      defaultRecoverCoeff = defaults.defaultRecoverCoeff;
    }
  
    if(!section.equals("defaults")) for(int i = 0; i < ingredientsBase.length; i++){
      if(ingredientsBase[i] == -1) ingredientsBase[i] = defaults.ingredientsBase[i];
      if(recoverCoeff[i] == -1) recoverCoeff[i] = defaults.recoverCoeff[i] == -1? defaultRecoverCoeff: defaults.recoverCoeff[i];
    }
    
    float total = 0;
    for(float base : ingredientsBase){
      total += base;
    }
    
    for(int i=0; i<ingredients.length; i++){
      ingredients[i] = baseTotal*(ingredientsBase[i]/total);
    }
  }
  
  @Override
  public String toString(){
    return "baseTotal: " + baseTotal + ", " +
      "basePressure: " + basePressure + ", " +
      "defaultRecoverCoeff: " + defaultRecoverCoeff + ", " +
      "ingredients: " + Arrays.toString(ingredientsBase) + ", " +
      "recoverCoeff: " + Arrays.toString(recoverCoeff);
  }
}
