package singularity.world.blockComp;

import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.world.blocks.nuclear.NuclearEnergyNet;
import singularity.world.modules.NuclearEnergyModule;
import universeCore.entityComps.blockComps.BuildCompBase;
import universeCore.entityComps.blockComps.Dumpable;
import universeCore.entityComps.blockComps.FieldGetter;

/**这个接口表明此Building是具有核能的方块，需要在create当中初始化一个NuclearEnergyModule
 * 必须创建的变量：
 * <pre>{@code
 *   NuclearEnergyModule [energy]
 *   Seq<NuclearEnergyBuildComp> [energyLinked]
 * }<pre/>
 * 若使用非默认命名则需要重写调用方法*/
public interface NuclearEnergyBuildComp extends BuildCompBase, FieldGetter, Dumpable{
  /**获得该块的NuclearEnergyBlock*/
  default NuclearEnergyBlockComp getNuclearBlock(){
    return getBlock(NuclearEnergyBlockComp.class);
  }
  
  /**用于获得该方块的核能模块*/
  default NuclearEnergyModule energy(){
    return getField(NuclearEnergyModule.class, "energy");
  }
  
  @SuppressWarnings("unchecked")
  default Seq<NuclearEnergyBuildComp> energyLinked(){
    return getField(Seq.class, "energyLinked");
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
    float rate = getEnergyMoveRate(next);
    onMoveEnergy(next, rate);
    handleEnergy(-rate);
    next.handleEnergy(rate);
    return rate;
  }
  
  default float getEnergyPressure(NuclearEnergyBuildComp other){
    if(!other.getNuclearBlock().hasEnergy()) return 0;
    return getEnergy() - other.getEnergy();
  }
  
  default NuclearEnergyNet getEnergyNetwork(){
    return energy().energyNet;
  }
  
  default void deLink(NuclearEnergyBuildComp other){
    other.energy().linked.removeValue(getBuilding().pos());
    other.updateLinked();
    energy().linked.removeValue(other.getBuilding().pos());
    updateLinked();
  }
  
  default void link(NuclearEnergyBuildComp other){
    other.energy().linked.add(getBuilding().pos());
    other.updateLinked();
    energy().linked.add(other.getBuilding().pos());
    updateLinked();
  }
  
  default void onEnergyNetworkRemoved(){
    if(energy() == null) return;
    getEnergyNetwork().remove(this);
    
    for(int i=0; i<energy().linked.size; i++){
      Tile tile = Vars.world.tile(energy().linked.get(i));
      if(!(tile.build instanceof NuclearEnergyBuildComp)) return;
      NuclearEnergyBuildComp other = (NuclearEnergyBuildComp)tile.build;
      deLink(other);
    }
    energy().linked.clear();
    updateLinked();
  }
  
  default void onEnergyNetworkUpdated(){
    updateLinked();
    for(NuclearEnergyBuildComp other : energyLinked()){
      if(other.energy() != null){
        other.getEnergyNetwork().addNet(getEnergyNetwork());
      }
    }
  }
  
  /**获取该块对目标块的核能传输速度*/
  default float getEnergyMoveRate(NuclearEnergyBuildComp next){
    if(!next.getNuclearBlock().hasEnergy() || !next.acceptEnergy(this) || getEnergy() < next.getEnergy()) return 0;
    float avg = (getEnergy()+ next.getEnergy())/2;
    
    float energyDiff = Mathf.maxZero(getEnergyPressure(next));
    float flowRate = Math.min(energyDiff*energyDiff/60*getBuilding().delta(), getEnergy() - avg);
    flowRate = Math.min(Math.min(flowRate, avg - next.getEnergy()), next.getNuclearBlock().energyCapacity() - next.getEnergy());
    
    return Math.max(flowRate, 0)-getMoveResident(next)*getBuilding().delta();
  }
  
  default void updateLinked(){
    Seq<NuclearEnergyBuildComp> linked = energyLinked();
    linked.clear();
    for(Building entity: getBuilding().proximity){
      if(!(entity instanceof NuclearEnergyBuildComp)) continue;
      NuclearEnergyBuildComp other = (NuclearEnergyBuildComp)entity;
      if(entity.team == getBuilding().team && other.getNuclearBlock().hasEnergy()
          && !(getNuclearBlock().consumeEnergy() && other.getNuclearBlock().consumeEnergy()
          && !getNuclearBlock().outputEnergy() && !other.getNuclearBlock().outputEnergy())) linked.add(other);
    }
  
    for(int i=0; i<energy().linked.size; i++){
      Tile entity = Vars.world.tile(energy().linked.get(i));
      if(entity == null || !(entity.build instanceof NuclearEnergyBuildComp) || ((NuclearEnergyBuildComp)entity.build).energy() == null) continue;
      if(! linked.contains((NuclearEnergyBuildComp)entity.build)) linked.add((NuclearEnergyBuildComp)entity.build);
    }
  }
  
  default Seq<Building> getEnergyDumpBuild(){
    Seq<Building> result = new Seq<>();
    for(NuclearEnergyBuildComp entity: getEnergyNetwork().consumer){
      result.add(entity.getBuilding());
    }
    return result;
  }
  
  default void onMoveEnergy(NuclearEnergyBuildComp dest, float rate){
    Seq<NuclearEnergyBuildComp> path = getEnergyNetwork().getPath(this, dest);
    for(NuclearEnergyBuildComp child: path) child.onMovePathChild(rate);
  }
  
  default void onMovePathChild(float flow){
  }
  
  default float getEnergy(){
    return energy().getEnergy();
  }
  
  default float getResident(){
    return getNuclearBlock().resident();
  }
  
  /**获取这个块到目标方块间的核能阻值*/
  default float getMoveResident(NuclearEnergyBuildComp destination){
    float resident = 0;
    for(NuclearEnergyBuildComp entity: getEnergyNetwork().getPath(this, destination)){
      resident += entity.getResident();
    }
    return resident;
  }
  
  /**返回该块是否接受核能输入*/
  default boolean acceptEnergy(NuclearEnergyBuildComp source){
    return getNuclearBlock().hasEnergy();
  }
  
  /**向连接的方块输出核能量，如果那个方块接受的话*/
  default void dumpEnergy(){
    NuclearEnergyBuildComp dump = (NuclearEnergyBuildComp)getDump(e -> {
      if(!(e instanceof NuclearEnergyBuildComp) || e == this) return false;
      return ((NuclearEnergyBuildComp) e).acceptEnergy(this);
    }, getEnergyDumpBuild());
    if(dump != null){
      moveEnergy(dump);
    }
  }
  
  /**当能压过载以后触发的方法
   * @param energyPressure 此时的方块间核势能差值
   */
  void onOverpressure(float energyPressure);
}
