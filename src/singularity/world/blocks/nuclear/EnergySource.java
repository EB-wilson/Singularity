package singularity.world.blocks.nuclear;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Tex;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.meta.Env;
import singularity.Singularity;
import singularity.ui.SglStyles;
import singularity.world.blockComp.NuclearEnergyBuildComp;

import static mindustry.Vars.tilesize;

public class EnergySource extends NuclearBlock{
  public EnergySource(String name){
    super(name);
    energyCapacity = 1024;
    outputEnergy = true;
    consumeEnergy = true;
    energyBuffered = true;
    configurable = true;
    saveConfig = true;
    noUpdateDisabled = true;
    envEnabled = Env.any;
  }
  
  @Override
  public void appliedConfig(){
    config(Float.class, (EnergySourceBuild tile, Float value) -> tile.outputEnergy = value);
    configClear((EnergySourceBuild tile) -> tile.outputEnergy = 0);
  }
  
  @Override
  public void setBars(){
    super.setBars();
    bars.add("energy", e -> new Bar(
      () -> Core.bundle.get("fragment.bars.potentialNuclearEnergy"),
      () -> Pal.bar,
      () -> ((NuclearEnergyBuildComp)e).getEnergy() / energyCapacity
    ));
  }
  
  public class EnergySourceBuild extends SglBuilding{
    protected float outputEnergy = 0;
  
    @Override
    public void updateTile(){
      energy.set(outputEnergy);
      
      dumpEnergy();
    }
    
    @Override
    public void displayEnergy(Table table){
      //不执行任何操作
    }
  
    @Override
    public Object config(){
      return outputEnergy;
    }
    
    @Override
    public void buildConfiguration(Table table){
      table.table(Styles.black6, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image(Singularity.getModAtlas("nuclear")).size(40)).size(50);
        t.slider(-energyCapacity, energyCapacity, 0.01f, outputEnergy, this::configure).size(200, 50).padLeft(8).padRight(8).get().setStyle(SglStyles.sliderLine);
        t.add("0").size(50).update(lable -> lable.setText(Strings.autoFixed(outputEnergy, 2) + "NF"));
      });
    }
  
    @Override
    public void drawConfigure(){
      super.drawConfigure();
      for(NuclearEnergyBuildComp entity: energy.energyNet.consumer){
        for(NuclearEnergyBuildComp e: energy.energyNet.getPath(this, entity)){
          Drawf.square(e.getBuilding().x, e.getBuilding().y, e.getBuilding().block.size * tilesize / 2f + 1f, Pal.accent);
        }
      }
    }
  
    @Override
    public boolean acceptEnergy(NuclearEnergyBuildComp source){
      return source.getEnergy() > getEnergy();
    }
    
    @Override
    public void draw(){
      Draw.rect(region, x, y);
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      write.f(outputEnergy);
    }
    
    @Override
    public void read(Reads read, byte b){
      super.read(read, b);
      outputEnergy = read.f();
    }
  }
}
