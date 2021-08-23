package singularity.world.modules;

import singularity.world.blocks.SglBlock.SglBuilding;
import singularity.world.blockComp.NuclearEnergyBuildComp;
import singularity.world.nuclearEnergy.EnergyGroup;
import singularity.world.nuclearEnergy.EnergyLevel;
import arc.Core;
import arc.func.Func;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.WindowedMean;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.util.Interval;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.ui.Bar;
import mindustry.world.modules.BlockModule;

public class NuclearEnergyModule extends BlockModule {
  public final NuclearEnergyBuildComp entity;
  public final float basePotential;
  public final boolean buffered;
  public final IntSeq linked = new IntSeq();
  
  private float added;
  private final WindowedMean moveMean = new WindowedMean(6);
  private static final Interval flowTimer = new Interval(2);
  
  /**目前具有的核能量大小
  * 关于核势能的计算： N = (E / 20)^2 ，N为核势能，E为容纳的能量*/
  private float included = 0f;
  
  public float displayMoving = 0f;
  
  public EnergyGroup group;
  
  public NuclearEnergyModule(NuclearEnergyBuildComp entity, float base, boolean buffered){
    this.entity = entity;
    this.basePotential = base;
    this.buffered = buffered;
    this.group = entity.getNuclearBlock().hasEnergyGroup()? new EnergyGroup(): null;
  }
  
  public void update(){
    if(flowTimer.get(1, 20f)){
      moveMean.add(added);
      added = 0;
      displayMoving = moveMean.hasEnoughData()? moveMean.mean() / 20 : -1;
    }
  }
  
  public float getIncluded(){
    return included;
  }
  
  public void handle(float value){
    included += value;
    if(value > 0){
      added += value;
    }
  }
  
  public void set(float value){
    included = value;
  }
  
  public void display(Table table){
    table.row();
    table.table(Tex.pane, energyBoard -> {
      energyBoard.add(Core.bundle.get("fragment.bars.nuclearEnergy")).center();
      energyBoard.defaults().pad(5).growX();
      energyBoard.row();
      Func<Building, Bar> bar = (ent -> {
        SglBuilding e = (SglBuilding) ent;
        return new Bar(
          () -> "energy",
          () -> Color.white,
          () -> e.energy.included/e.block().energyCapacity
        );
      });
  
      energyBoard.add(bar.get((Building)entity)).height(18).pad(4);
      energyBoard.row();
      energyBoard.table(info -> {
        info.defaults().left().padLeft(5);
        info.left();
        info.add("--").update(t -> t.setText(Core.bundle.format("fragment.bars.nuclearContain", included)));
        info.row();
        info.add("--").update(t -> t.setText(Core.bundle.format("fragment.bars.nuclearMoving", displayMoving >= 0? displayMoving: "--")));
        info.row();
        float potentialEnergy = getPotentialEnergy();
        
        info.add("--").update(warn -> {
          if(potentialEnergy >= EnergyLevel.warning.potentialEnergy){
            warn.setText(Core.bundle.get("warn.nuclearPressure.highest"));
            warn.color.set(Color.orange).lerp(Color.scarlet, Mathf.absin(Time.time, 2f, 1f));
          }
          else warn.setText(Core.bundle.format("fragment.bars.nuclearPressure", EnergyLevel.getLevel(potentialEnergy).level));
        });
      });
    }).fillX().growY();
  }
  
  public final float getPotentialEnergy(){
    return buffered? basePotential: (included/20)*(included/20);
  }
  
  @Override
  public void write(Writes write){
    write.f(included);
  }

  @Override
  public void read(Reads read){
    included = read.f();
  }
}