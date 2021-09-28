package singularity.world.blocks.product;

import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.util.Strings;
import mindustry.ctype.MappableContent;
import mindustry.entities.Puddles;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.consumers.ConsumePower;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blockComp.HeatBlockComp;
import singularity.world.blocks.SglBlock;
import singularity.world.modules.ReactionModule;
import singularity.world.reaction.ReactContainer;

public class ReactionKettle extends SglBlock implements HeatBlockComp{
  public float maxTemperature = 4;
  public float productHeat = 0.02f;
  
  public ReactionKettle(String name){
    super(name);
    hasPower = true;
    hasItems = true;
    hasLiquids = true;
    outputsLiquid = false;
    hasGases = true;
    outputGases = false;
    
    update = true;
    
    consumes.add(new ConsumePower(1, powerCapacity, false){
      @Override
      public float requestedPower(Building e){
        ReactionKettleBuild entity = (ReactionKettleBuild) e;
        return Math.max(0, productHeat*5*entity.heatScl);
      }
    });
  }
  
  public class ReactionKettleBuild extends SglBuilding implements ReactContainer{
    public float heat;
    public float internalTemperature;
    
    public ReactionModule reacts;
    public float heatScl;
    public float heatCapacity;
    
    public ObjectSet<MappableContent> added = new ObjectSet<>();
  
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      reacts = new ReactionModule(this);
  
      setModules();
      return this;
    }
  
    @Override
    public void updateTile(){
      heatScl = 1 - temperature()/internalTemperature*edelta();
      
      if(heatScl > 0){
        heat += heatScl*productHeat;
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
    }
  
    @Override
    public void buildConfiguration(Table table){
      table.table(Styles.black8, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans).size(50);
        t.slider(0, maxTemperature, 0.01f, f -> internalTemperature = f).size(200, 50).padLeft(8).padRight(8).get();
        t.add("0").size(50).update(lable -> lable.setText(Strings.autoFixed(internalTemperature, 2) + "%"));
      });
    }
  
    @Override
    public void handleItem(Building source, Item item){
      super.handleItem(source, item);
      if(added.add(item)){
        reacts.matchAll(item);
      }
    }
    
    @Override
    public void handleLiquid(Building source, Liquid liquid, float amount){
      super.handleLiquid(source, liquid, amount);
      if(added.add(liquid)){
        reacts.matchAll(liquid);
      }
    }
    
    @Override
    public void handleGas(GasBuildComp source, Gas gas, float amount){
      super.handleGas(source, gas, amount);
      if(added.add(gas)){
        reacts.matchAll(gas);
      }
    }
  
    @Override
    public void heat(float heat){
      this.heat = heat;
    }
  
    @Override
    public float pressure(){
      return super.pressure();
    }
  }
}
