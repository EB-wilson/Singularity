package singularity.world.blocks.nuclear;

import singularity.Singularity;
import singularity.world.blocks.SglBlock;
import singularity.world.blockComp.NuclearEnergyBuildComp;
import singularity.world.nuclearEnergy.EnergyLevel;
import arc.Core;
import arc.graphics.g2d.Draw;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;

import java.util.concurrent.atomic.AtomicBoolean;

public class EnergySource extends SglBlock{
  public EnergySource(String name){
    super(name);
    hasEnergy = true;
    energyCapacity = EnergyLevel.warning.energyContent;
    outputEnergy = true;
    energyBuffered = true;
    configurable = true;
    saveConfig = true;
    group = BlockGroup.transportation;
    update = true;
    solid = true;
    noUpdateDisabled = true;
    envEnabled = Env.any;
  
    config(Number.class, (EnergySourceBuild tile, Number value) -> tile.potentialEnergy = value.floatValue());
    configClear((EnergySourceBuild tile) -> tile.potentialEnergy = 0);
  }
  
  @Override
  public void setBars(){
    super.setBars();
    bars.add("energy", e -> new Bar(
      () -> Core.bundle.get("fragment.bars.potentialNuclearEnergy"),
      () -> Pal.bar,
      () -> ((NuclearEnergyBuildComp)e).getPotentialEnergy((NuclearEnergyBuildComp)e) / EnergyLevel.warning.potentialEnergy
    ));
  }
  
  public class EnergySourceBuild extends SglBuilding{
    protected float potentialEnergy = 0;
  
    @Override
    public void updateTile(){
      energy.set(energyCapacity()/2);
      
      dumpEnergy();
      
      energy.set(0);
    }
    
    @Override
    public void displayEnergy(Table table){
      //不执行任何操作
    }
  
    @Override
    public float getPotentialEnergy(NuclearEnergyBuildComp getter){
      return potentialEnergy;
    }
  
    @Override
    public Object config(){
      return potentialEnergy;
    }
    
    @Override
    public void buildConfiguration(Table table){
      table.table(Tex.buttonTrans, config -> {
        config.add("--").padLeft(5).padTop(5).padBottom(5).update(e -> e.setText(Core.bundle.format("fragment.buttons.nuclearOut", potentialEnergy)));
        config.row();
        
        config.table(buttons -> {
          buttons.defaults().size(35, 35);
          for(int i=3; i>=-3; i--){
            final int[] data = {i, 0, 60};
            float delta = (float)Math.min(EnergyLevel.warning.potentialEnergy - potentialEnergy, Math.pow(data[0] > 0? 10: -10, Math.abs(data[0])));
            
            if(i == 0){
              ImageButton button = new ImageButton(Singularity.getModAtlas("reset"));
              button.clicked(() -> potentialEnergy = 0);
              buttons.add(button).size(50, 50);
              continue;
            }
            
            AtomicBoolean isClick = new AtomicBoolean(false);
            ImageButton button = new ImageButton(Singularity.getModAtlas("energy_source_potent_" + (i>0? "up": "down") + "_" + (Math.abs(i) - 1)));
            button.tapped(() -> {
              isClick.set(true);
              potentialEnergy += delta;
            });
            button.released(() -> isClick.set(false));
            button.update(() -> {
              boolean click = isClick.get();
              if(click){
                data[1]++;
                if(data[2] > 1 && data[1]%2 == 0) data[2]--;
                if(data[1]%data[2] == 0){
                  potentialEnergy += delta;
                }
              }
              else{
                data[1] = 0;
                data[2] = 60;
              }
            });
            buttons.add(button);
          }
        });
      }).grow();
    }
  
    @Override
    public boolean acceptEnergy(NuclearEnergyBuildComp source){
      return false;
    }
    
    @Override
    public void draw(){
      Draw.rect(region, x, y);
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      write.f(potentialEnergy);
    }
    
    @Override
    public void read(Reads read){
      super.read(read);
      potentialEnergy = read.f();
    }
  }
}
