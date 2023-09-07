package singularity.world.components;

import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import singularity.world.modules.NuclearEnergyModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;
import universecore.components.blockcomp.Takeable;

/**这个接口表明此Building是具有核能的方块，需要在create当中初始化一个NuclearEnergyModule*/
public interface NuclearEnergyBuildComp extends BuildCompBase, Takeable{
  Seq<NuclearEnergyBuildComp> tmp = new Seq<>(){{ordered = false;}};

  /**获得该块的NuclearEnergyBlock*/
  default NuclearEnergyBlockComp getNuclearBlock(){
    return getBlock(NuclearEnergyBlockComp.class);
  }
  
  /**用于获得该方块的核能模块*/
  @Annotations.BindField("energy")
  default NuclearEnergyModule energy(){
    return null;
  }
  
  @Annotations.BindField(value = "energyLinked", initialize = "new arc.struct.Seq<>()")
  default Seq<NuclearEnergyBuildComp> energyLinked(){
    return null;
  }
  
  /**将核能面板显示出来，或者显示别的什么东西*/
  default void displayEnergy(Table table){
    if(getNuclearBlock().hasEnergy()) energy().display(table);
  }
  
  /**操作核能量的方法，通常直接引用EnergyModule的handle方法*/
  default void handleEnergy(float value){
    energy().handle(value);
  }
  
  /**转运核能量到指定的块中，速度取决于势能差*/
  default float moveEnergy(NuclearEnergyBuildComp next){
    if(!next.getNuclearBlock().hasEnergy() || !next.acceptEnergy(this)) return 0;
    float rate = getEnergyMoveRate(next);

    if (rate > 0) {
      float energyDiff = getEnergyPressure(next);
      if (energyDiff > next.getNuclearBlock().maxEnergyPressure()) next.onOverpressure(energyDiff);

      rate = Math.min(rate, next.getNuclearBlock().energyCapacity() - next.getEnergy());
      rate = Math.min(rate, getEnergy());

      handleEnergy(-rate);
      next.handleEnergy(rate);

      energyMoved(next, rate);
    }
    return rate;
  }

  default float getEnergyPressure(NuclearEnergyBuildComp other){
    if(!other.getNuclearBlock().hasEnergy()) return 0;
    return Mathf.maxZero(getOutputPotential() - other.getInputPotential() - other.getNuclearBlock().basicPotentialEnergy());
  }

  @Annotations.MethodEntry(entryMethod = "update", insert = Annotations.InsertPosition.HEAD)
  default void updateEnergy(){
    if(getNuclearBlock().hasEnergy()) energy().update();
  }
  
  /**获取该块对目标块的核能传输速度*/
  default float getEnergyMoveRate(NuclearEnergyBuildComp next){
    if(!next.getNuclearBlock().hasEnergy() || !next.acceptEnergy(this) || getOutputPotential() < next.getInputPotential()) return 0;
    float energyDiff = getEnergyPressure(next);

    float flowRate = (Math.min(energyDiff*energyDiff/60, energyDiff) - next.getResident())*getBuilding().delta();
    flowRate = Math.min(flowRate, next.getNuclearBlock().energyCapacity() - next.getEnergy());
    flowRate = Math.min(flowRate, getEnergy());

    return Math.max(flowRate, 0);
  }

  default Seq<NuclearEnergyBuildComp> proximityNuclearBuilds(){
    tmp.clear();

    for (Building building : getBuilding().proximity) {
      if (building instanceof NuclearEnergyBuildComp n && n.getNuclearBlock().hasEnergy()){
        tmp.add(n);
      }
    }

    return tmp;
  }

  default float getEnergy(){
    return energy().getEnergy();
  }

  default float getInputPotential(){
    return energy().getEnergy();
  }

  default float getOutputPotential(){
    return energy().getEnergy();
  }
  
  default float getResident(){
    return getNuclearBlock().resident();
  }
  
  /**返回该块是否接受核能输入*/
  default boolean acceptEnergy(NuclearEnergyBuildComp source){
    return getBuilding().interactable(source.getBuilding().team) && getNuclearBlock().hasEnergy() && energy().getEnergy() < getNuclearBlock().energyCapacity();
  }

  default void dumpEnergy(){
    dumpEnergy(proximityNuclearBuilds());
  }

  /**向连接的方块输出核能量，如果那个方块接受的话*/
  default void dumpEnergy(Seq<NuclearEnergyBuildComp> dumpTargets){
    NuclearEnergyBuildComp dump = getNext("energy", dumpTargets, e -> {
      if(e == null || e == this) return false;
      return e.acceptEnergy(this) && getEnergyMoveRate(e) > 0;
    });
    if(dump != null){
      moveEnergy(dump);
    }
  }

  default void energyMoved(NuclearEnergyBuildComp next, float rate){}

  /**当能压过载以后触发的方法
   * @param energyPressure 此时的方块间核势能差值
   */
  void onOverpressure(float energyPressure);
}
