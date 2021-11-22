package singularity.type;

import universeCore.util.UncContentType;

public class SglContentType extends UncContentType{
  public static SglContentType gas;
  public static SglContentType ability;
  public static SglContentType structure;
  public static SglContentType reaction;
  
  public static SglContentType[] allSglContentType;
  
  public SglContentType(String name, int ordinal){
    super(name, ordinal);
  }
  
  public SglContentType(String name){
    super(name);
  }
  
  public static void load(){
    gas = new SglContentType("gas", 4);
    ability = new SglContentType("ability");
    structure = new SglContentType("structure");
    reaction = new SglContentType("reaction");
    
    allSglContentType = new SglContentType[]{gas, ability, structure, reaction};
  }
}
