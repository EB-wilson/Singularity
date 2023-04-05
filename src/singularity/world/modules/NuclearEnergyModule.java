package singularity.world.modules;

import arc.Core;
import arc.Events;
import arc.func.Func;
import arc.graphics.Color;
import arc.math.WindowedMean;
import arc.scene.ui.layout.Table;
import arc.util.Interval;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.ui.Bar;
import mindustry.world.modules.BlockModule;
import singularity.world.blocks.SglBlock.SglBuilding;
import singularity.world.components.NuclearEnergyBuildComp;
import universecore.util.handler.FieldHandler;

public class NuclearEnergyModule extends BlockModule {
  static NuclearEnergyBuildComp lastShowFlow;

  static {
    Events.run(EventType.Trigger.update, () -> {
      Building nextFlowBuild = FieldHandler.getValueDefault(Vars.ui.hudfrag.blockfrag, "nextFlowBuild");

      if(nextFlowBuild instanceof NuclearEnergyBuildComp nuclearBuild && nuclearBuild.getNuclearBlock().hasEnergy()){
        if (lastShowFlow != nuclearBuild){
          nuclearBuild.energy().stopFlow();
          lastShowFlow = nuclearBuild;
        }

        nuclearBuild.energy().updateFlow();
      }
    });
  }

  public final NuclearEnergyBuildComp entity;
  public final boolean buffered;
  
  private float added;
  private final WindowedMean moveMean = new WindowedMean(6);
  private final Interval flowTimer = new Interval();
  
  /**目前具有的核能量大小*/
  private float energy = 0f;
  
  public float displayMoving = 0f;
  
  public NuclearEnergyModule(NuclearEnergyBuildComp entity, boolean buffered){
    this.entity = entity;
    this.buffered = buffered;
  }

  public void stopFlow() {
    added = 0;
    moveMean.clear();
    displayMoving = -1;
  }

  public void updateFlow(){
    if(flowTimer.get(20f)){
      moveMean.add(added);
      added = 0;
      displayMoving = moveMean.hasEnoughData() ? moveMean.mean()/20 : - 1;
    }
  }
  
  public void update(){
  }
  
  public float getEnergy(){
    return energy;
  }
  
  public void handle(float value){
    energy += value;
    if(value > 0){
      added += value;
    }
  }
  
  public void set(float value){
    energy = value;
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
          () -> e.energy.energy/e.block().energyCapacity
        );
      });
  
      energyBoard.add(bar.get((Building)entity)).height(18).pad(4);
      energyBoard.row();
      energyBoard.table(info -> {
        info.defaults().left().padLeft(5);
        info.left();
        info.add("--").update(t -> t.setText(Core.bundle.format("fragment.bars.nuclearContain",
            energy >= 1000? UI.formatAmount((long) energy): Strings.autoFixed(energy, 1))));
        info.row();
        info.add("--").update(t -> t.setText(Core.bundle.format("fragment.bars.nuclearMoving", displayMoving >= 0? Strings.autoFixed(displayMoving*60, 1): "--")));
        info.row();
      });
    }).fillX().growY();
  }
  
  @Override
  public void write(Writes write){
    write.f(energy);
  }

  @Override
  public void read(Reads read, boolean revision){
    energy = read.f();
    if (revision) {
      int length = read.i();
      for (int i = 0; i < length; i++) {
        read.i();
      }
    }
  }
}