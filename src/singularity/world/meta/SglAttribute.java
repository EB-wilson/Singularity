package singularity.world.meta;

import mindustry.world.meta.Attribute;

public class SglAttribute{
  //沥青属性，影响采集深岩沥青相关的机器效率
  public static final Attribute bitumen = Attribute.add("bitumen");
  
  public static final Attribute[] all = {bitumen};
  
  public static Attribute[] values(){
    return all;
  }
}
