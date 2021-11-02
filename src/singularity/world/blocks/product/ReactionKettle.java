package singularity.world.blocks.product;

import arc.Core;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.core.UI;
import mindustry.entities.Puddles;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.consumers.ConsumeType;
import singularity.Sgl;
import singularity.Singularity;
import singularity.type.Gas;
import singularity.ui.SglStyles;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blockComp.HeatBlockComp;
import singularity.world.blockComp.HeatBuildComp;
import singularity.world.blocks.SglBlock;
import singularity.world.meta.SglStatUnit;
import singularity.world.modules.ReactionModule;
import singularity.world.reaction.ReactContainer;

public class ReactionKettle extends SglBlock implements HeatBlockComp{
  public float totalItemCapacity = 60;
  public float heatCoefficient = 0.2f;
  public float totalLiquidCapacity = 60;
  public float maxTemperature = 920;
  public float baseHeatCapacity = 600;
  public float productHeat = 30000f;
  
  public ReactionKettle(String name){
    super(name);
    hasPower = true;
    consumesPower = true;
    
    hasItems = true;
    hasLiquids = true;
    outputsLiquid = false;
    hasGases = true;
    outputGases = false;
    
    configurable = true;
    
    update = true;
  }
  
  @Override
  public void appliedConfig(){
    config(Float.class, (ReactionKettleBuild e, Float f) -> e.internalTemperature = f);
    configClear((ReactionKettleBuild e) -> e.internalTemperature = 0);
  }
  
  @Override
  public void initPower(float powerCapacity){
    consumes.add(new ConsumePower(0, powerCapacity, false){
      @Override
      public float requestedPower(Building e){
        ReactionKettleBuild entity = (ReactionKettleBuild) e;
        return Math.max(0, productHeat/500*entity.heatScl);
      }
    });
  }
  
  @Override
  public void setStats(){
    super.setStats();
    setHeatStats(stats);
  }
  
  @Override
  public void setBars(){
    super.setBars();
    ConsumePower cons = consumes.getPower();
    boolean buffered = cons.buffered;
    float capacity = cons.capacity;
  
    bars.add("power", entity -> new Bar(() -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * capacity) ? "<ERROR>" : UI.formatAmount((int)(entity.power.status * capacity))) :
        Core.bundle.get("bar.power"), () -> Pal.powerBar, () -> Mathf.zero(cons.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status));
  
    bars.add("temperature", (ReactionKettleBuild ent) -> new Bar(
        () -> Core.bundle.get("misc.temperature") + ":" + Strings.autoFixed(ent.temperature(), 2) + SglStatUnit.temperature.localized() +
            "-" + Core.bundle.get("misc.heat") + ":" + Strings.autoFixed(ent.heat/1000, 0) + SglStatUnit.kHeat.localized(),
        () -> Pal.bar,
        () -> ent.absTemperature()/maxTemperature
    ));
  }
  
  public class ReactionKettleBuild extends SglBuilding implements ReactContainer{
    public float heat;
    public float internalTemperature;
    
    public ReactionModule reacts;
    public float heatScl;
    public float heatCapacity = baseHeatCapacity;
  
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      reacts = new ReactionModule(this);
  
      setModules();
      internalTemperature = Sgl.atmospheres.current.getAbsTemperature();
      heat = internalTemperature*heatCapacity();
      
      return this;
    }
  
    @Override
    public float efficiency(){
      if(!this.enabled){
        return 0;
      }
      else{
        return this.power != null && this.block.consumes.has(ConsumeType.power) && !this.block.consumes.getPower().buffered ? this.power.status : 1;
      }
    }
  
    @Override
    public void updateTile(){
      heatScl = internalTemperature > 0? Math.max(0, 1 - absTemperature()/internalTemperature): 0;
  
      if(heatScl > 0){
        heat += heatScl*productHeat*edelta();
      }
      
      reacts.update();
      reacts.each((react, progress) -> {
        if(react.product.isItem){
          if(items.get((Item) react.product.get()) > itemCapacity){
            int lost = items.get((Item) react.product.get()) - itemCapacity;
      
            items.remove((Item) react.product.get(), lost);
          }
        }
        else if(react.product.isLiquid){
          if(liquids.get((Liquid) react.product.get()) > liquidCapacity){
            Liquid liquid = (Liquid) react.product.get();
            float leak = liquids.get(liquid) - liquidCapacity;
            liquids.remove(liquid, leak);
            Puddles.deposit(tile, liquid, leak);
          }
        }
        else if(react.product.isGas){
          if(pressure() > maxGasPressure){
            float leak = (pressure() - maxGasPressure)*gasCapacity;
            float total = gases.total();
      
            gases.each(stack -> {
              float amount = leak*stack.amount/total;
              gases.remove(stack.gas, amount);
              Sgl.gasAreas.pour(tile, stack.gas, amount);
            });
          }
        }
      });
      
      swapHeat();
    }
  
    @Override
    public void buildConfiguration(Table table){
      table.table(Styles.black6, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image(Singularity.getModAtlas("icon_temperature")).size(40)).size(50);
        t.slider(Sgl.atmospheres.current.getAbsTemperature(), maxTemperature, 0.01f, internalTemperature, this::configure).size(200, 50).padLeft(8).padRight(8).get().setStyle(SglStyles.sliderLine);
        t.add("0").size(50).update(lable -> lable.setText(Strings.autoFixed(HeatBuildComp.getTemperature(internalTemperature), 2) + SglStatUnit.temperature.localized()));
      });
    }
  
    @Override
    public Object config(){
      return internalTemperature;
    }
  
    @Override
    public void handleItem(Building source, Item item){
      super.handleItem(source, item);
      reacts.matchAll(item);
    }
    
    @Override
    public void handleLiquid(Building source, Liquid liquid, float amount){
      super.handleLiquid(source, liquid, amount);
      reacts.matchAll(liquid);
    }
    
    @Override
    public void handleGas(GasBuildComp source, Gas gas, float amount){
      super.handleGas(source, gas, amount);
      reacts.matchAll(gas);
    }
  
    @Override
    public void heat(float heat){
      this.heat = heat;
    }
  
    @Override
    public float pressure(){
      return super.pressure();
    }
  
    @Override
    public boolean acceptItem(Building source, Item item){
      return hasItems && items.get(item) < itemCapacity && items.total() < totalItemCapacity;
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return hasLiquids && liquids.get(liquid) < liquidCapacity && liquids.total() < totalLiquidCapacity;
    }
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return hasGases && source.getBuilding().team == this.team && pressure() < maxGasPressure;
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      reacts.write(write);
      
      write.f(heat);
      write.f(internalTemperature);
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      reacts.read(read);
      heat = read.f();
      internalTemperature = read.f();
      
      heatCapacity = accurateHeatCapacity();
    }
  }
}
