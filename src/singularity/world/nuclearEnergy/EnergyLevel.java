package singularity.world.nuclearEnergy;

import arc.Core;

/**核势能的级别，用于区分不同能量需求的设备*/
public enum EnergyLevel{
  /**超低压 势能级别:1 数量级:64NE/160NF*/
  ultraLow,
  /**低压 势能级别:2 数量级:256NE/320NF*/
  low,
  /**中压 势能级别:3 数量级:1024NE/640NF*/
  medium,
  /**高压 势能级别:4 数量级:4096NE/1280NF*/
  high,
  /**超高压 势能级别:5 数量级:16384NE/2560NF*/
  ultraHigh,
  /**特高压 势能级别:6 数量级:65536NE/5120NF*/
  extraHigh,
  /**临界压 势能级别:7 数量级:262144NE/10240NF*/
  critical,
  /**边界压 势能级别:8 数量级:1048576NE/20480NF*/
  bound,
  /**超界压 势能级别:9 数量级:4194304NE/40960NF*/
  overBound,
  
  /**警告压 势能级别:10 数量级:>16777216NE/81920NF*/
  warning;
  
  /**此势能级别的int数据表示*/
  public final int level;
  
  /**此势能级别的文本表示*/
  public final String localization;
  
  /**该级别对应的中子通量大小(注意:对于核能缓冲设备而言核势能是一定的)*/
  public final float energyContent;
  
  /**该级别所对应的(最小)核势能大小*/
  public final float potentialEnergy;
  
  EnergyLevel(){
    level = ordinal() + 1;
    localization = Core.bundle.get("misc.energyLevel." + name());
    energyContent = 160 * (float)Math.pow(2, ordinal());
    potentialEnergy = (energyContent/20)*(energyContent/20);
  }
  
  public static EnergyLevel getLevel(float potentialEnergy){
    if(potentialEnergy <= ultraLow.potentialEnergy) return ultraLow;
    for(int i=values().length - 2; i>=0; i--){
      if(potentialEnergy >= values()[i].potentialEnergy) return values()[i];
    }
    return warning;
  }
  
  public static EnergyLevel getLevelByNF(float energyContent){
    return getLevel((energyContent/20)*(energyContent/20));
  }
}
