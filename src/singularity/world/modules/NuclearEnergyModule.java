package singularity.world.modules;

import arc.Core;
import arc.func.Func;
import arc.graphics.Color;
import arc.math.WindowedMean;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.util.Interval;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.ui.Bar;
import mindustry.world.modules.BlockModule;
import singularity.world.components.NuclearEnergyBuildComp;
import singularity.world.blocks.SglBlock.SglBuilding;
import singularity.world.blocks.nuclear.NuclearEnergyNet;

public class NuclearEnergyModule extends BlockModule {
  public final NuclearEnergyBuildComp entity;
  public final float baseAcceptPres;
  public final boolean buffered;
  public final IntSeq linked = new IntSeq();
  
  public NuclearEnergyNet energyNet;
  
  private float added;
  private final WindowedMean moveMean = new WindowedMean(6);
  private static final Interval flowTimer = new Interval(2);
  
  /**目前具有的核能量大小*/
  private float energy = 0f;
  
  public float displayMoving = 0f;
  
  public NuclearEnergyModule(NuclearEnergyBuildComp entity, float base, boolean buffered){
    this.entity = entity;
    this.baseAcceptPres = base;
    this.buffered = buffered;
  }
  
  public void update(boolean showFlow){
    if(showFlow){
      if(flowTimer.get(1, 20f)){
        moveMean.add(added);
        added = 0;
        displayMoving = moveMean.hasEnoughData() ? moveMean.mean()/20 : - 1;
      }
    }
    else moveMean.clear();
    
    energyNet.update();
  }
  
  public void setNet(){
    setNet(null);
  }
  
  public void setNet(NuclearEnergyNet net){
    if(net == null){
      new NuclearEnergyNet().add(entity);
    }
    else energyNet = net;
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
        info.add("--").update(t -> t.setText(Core.bundle.format("fragment.bars.nuclearContain", energy)));
        info.row();
        info.add("--").update(t -> t.setText(Core.bundle.format("fragment.bars.nuclearMoving", displayMoving >= 0? displayMoving*60: "--")));
        info.row();
      });
    }).fillX().growY();
  }
  
  @Override
  public void write(Writes write){
    write.f(energy);
    write.i(linked.size);
    for(int i=0; i<linked.size; i++){
      write.i(linked.get(i));
    }
  }

  @Override
  public void read(Reads read){
    energy = read.f();
    int length = read.i();
    for(int i=0; i<length; i++){
      linked.add(read.i());
    }
  }
}