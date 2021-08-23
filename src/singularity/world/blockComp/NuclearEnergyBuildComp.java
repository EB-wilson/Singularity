package singularity.world.blockComp;

import arc.func.Boolf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.world.modules.NuclearEnergyModule;
import universeCore.entityComps.blockComps.BuildCompBase;
import universeCore.entityComps.blockComps.FieldGetter;

/**这个接口表明此Building是具有核能的方块，需要在create当中初始化一个NuclearEnergyModule
 * 必须创建的变量：
 * <pre>{@code
 *   NuclearEnergyModule [energy]
 * }<pre/>
 * 若使用非默认命名则需要重写调用方法*/
public interface NuclearEnergyBuildComp extends BuildCompBase, FieldGetter{
  /**获得该块的NuclearEnergyBlock*/
  default NuclearEnergyBlockComp getNuclearBlock(){
    return getBlock(NuclearEnergyBlockComp.class);
  }
  
  /**用于获得该方块的核能模块*/
  default NuclearEnergyModule energy(){
    return getField(NuclearEnergyModule.class, "energy");
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
    if(!(next instanceof Building)) return 0;
    if(!next.getNuclearBlock().hasEnergy() || !next.acceptEnergy(this)) return 0;
    float rate = Math.min(energy().getIncluded(), Math.min(next.getNuclearBlock().energyCapacity() - next.energy().getIncluded(), getEnergyMoveRate(next)));
    if(rate < 0) return 0;
    handleEnergy(-rate);
    next.handleEnergy(rate);
    return rate;
  }
  
  /**获取该块对目标块的核能传输速度*/
  default float getEnergyMoveRate(NuclearEnergyBuildComp next){
    if(!next.getNuclearBlock().hasEnergy() || !next.acceptEnergy(this) || getPotentialEnergy(next) < next.energy().basePotential) return 0;
    float potenDiff = getPotentialEnergy(next) - next.getPotentialEnergy(this);
    if(potenDiff > next.getNuclearBlock().maxEnergyPressure()) next.onOverpressure(potenDiff);
    return potenDiff > 0.05? (float)(potenDiff*0.1)/getMoveResident(next): 0;
  }
  
  default Seq<NuclearEnergyBuildComp> getEnergyLinked(){
    Seq<NuclearEnergyBuildComp> temp = new Seq<>();
    if(energy() == null) return temp;
    
    for(Building entity: getBuilding().proximity){
      if(!(entity instanceof NuclearEnergyBuildComp)) continue;
      NuclearEnergyBuildComp other = (NuclearEnergyBuildComp)entity;
      if(other.energy() != null && (other.getNuclearBlock().outputEnergy() || other.getNuclearBlock().consumeEnergy())) temp.add(other);
    }
    
    for(int i=0; i<energy().linked.size; i++){
      Tile entity = Vars.world.tile(energy().linked.get(i));
      if(entity == null || !(entity.build instanceof NuclearEnergyBuildComp) || ((NuclearEnergyBuildComp)entity.build).energy() == null) continue;
      if(!temp.contains((NuclearEnergyBuildComp)entity.build)) temp.add((NuclearEnergyBuildComp)entity.build);
    }
    
    return temp;
  }
  
  default float getResident(){
    return getNuclearBlock().resident();
  }
  
  /**获取这个块到目标方块间的核能阻值*/
  default float getMoveResident(NuclearEnergyBuildComp destination){
    return getResident() + destination.getResident();
  }
  
  /**获取该方块的核势能值，传入获取者的实体，之后的类里会有用*/
  default float getPotentialEnergy(NuclearEnergyBuildComp getter){
    return energy().getPotentialEnergy();
  }
  
  /**返回该块是否接受核能输入*/
  default boolean acceptEnergy(NuclearEnergyBuildComp source){
    return false;
  }
  
  /**向周围的方块输出核能量，如果那个方块接受的话*/
  void dumpEnergy();
  
  /**当能压过载以后触发的方法
   * @param potentialEnergy 此时的方块间核势能差值
   */
  void onOverpressure(float potentialEnergy);
}
