package singularity.type;

import universeCore.util.UncContentType;

public class SglContentType{
  public final static UncContentType gas = new UncContentType("gas", 4);
  public final static UncContentType reaction = new UncContentType("reaction");
  
  public final static UncContentType[] allSglContentType = new UncContentType[]{gas, reaction};
}
