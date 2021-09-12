package singularity.type;

import universeCore.util.UncContentType;

public class SglContentType{
  public static UncContentType gas;
  public static UncContentType reaction;
  
  public static UncContentType[] allSglContentType;
  
  public static void load(){
    gas = new UncContentType("gas", 4);
    reaction = new UncContentType("reaction");
    
    allSglContentType = new UncContentType[]{gas, reaction};
  }
}
