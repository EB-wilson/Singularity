package singularity.world.atmosphere;

import arc.files.Fi;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.type.SglContents;
import universecore.util.ini.Ini;
import universecore.util.ini.IniFile;
import universecore.util.ini.IniTypes;

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
  
  public float[] ingredientsBase = new float[SglContents.gases().size];
  public float[] ingredients = new float[SglContents.gases().size];
  public float[] recoverCoeff = new float[SglContents.gases().size];
  
  public float baseTotal;
  public float basePressure;
  public float defaultRecoverCoeff;
  public float heatCapacity;
  public float baseTemperature;
  
  public DefaultAtmosphere(Atmosphere parent){
    this.parent = parent;
    Arrays.fill(ingredientsBase, parent == null? 1: -1);
    Arrays.fill(recoverCoeff, -1);
    
    if(parent == null){
      loadData("defaults");
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
        baseTemperature = ((IniTypes.IniNumber)map.get("baseTemperature")).floatValue();
      }
      else{
        baseTotal = map.get("baseTotal") != null? ((IniTypes.IniNumber)map.get("baseTotal")).floatValue(): defaults.baseTotal;
        basePressure = map.get("basePressure") != null? ((IniTypes.IniNumber)map.get("basePressure")).floatValue(): defaults.basePressure;
        defaultRecoverCoeff = map.get("defaultRecoverCoeff") != null? ((IniTypes.IniNumber)map.get("defaultRecoverCoeff")).floatValue(): defaults.defaultRecoverCoeff;
        baseTemperature = map.get("baseTemperature") != null? ((IniTypes.IniNumber)map.get("baseTemperature")).floatValue(): defaults.baseTemperature;
      }
      
      IniTypes.IniMap ingre = (IniTypes.IniMap) map.get("ingredients");
      IniTypes.IniMap recov = (IniTypes.IniMap) map.get("recoverCoeff");
  
      if(ingre != null) ingre.get().forEach((k, v) -> {
        Gas gas = SglContents.gas(Sgl.modName + "-" + k);
        ingredientsBase[gas.id] = ((IniTypes.IniNumber)v).floatValue();
      });
      
      if(recov != null) recov.get().forEach((k, v) -> {
        Gas gas = SglContents.gas(Sgl.modName + "-" + k);
        recoverCoeff[gas.id] = ((IniTypes.IniNumber)v).floatValue();
      });
    }
    else{
      baseTotal = defaults.baseTotal;
      basePressure = defaults.basePressure;
      defaultRecoverCoeff = defaults.defaultRecoverCoeff;
      baseTemperature = defaults.baseTemperature;
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
      heatCapacity += SglContents.gas(i).heatCapacity*ingredients[i];
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
