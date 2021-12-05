package singularity.world.blocks.nuclear;

import arc.Core;
import arc.Events;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;
import singularity.Sgl;
import singularity.contents.SglItems;
import singularity.type.GasStack;
import singularity.ui.tables.GasDisplay;
import singularity.world.Particle;
import singularity.world.SglFx;
import singularity.world.blockComp.HeatBlockComp;
import singularity.world.blockComp.HeatBuildComp;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.consumers.SglConsumeType;
import singularity.world.draw.DrawNuclearReactor;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import singularity.world.products.ProduceEnergy;
import universeCore.world.consumers.BaseConsumers;
import universeCore.world.consumers.UncConsumeItems;
import universeCore.world.consumers.UncConsumeLiquids;
import universeCore.world.consumers.UncConsumeType;
import universeCore.world.producers.BaseProduce;
import universeCore.world.producers.ProducePower;

import static mindustry.Vars.state;
import static mindustry.Vars.tilesize;
import static singularity.world.blockComp.HeatBuildComp.getItemAbsTemperature;
import static singularity.world.blockComp.HeatBuildComp.getLiquidAbsTemperature;

public class NuclearReactor extends NormalCrafter implements HeatBlockComp{
  public float maxTemperature = 1273.15f;
  public float heatCoefficient = 1f;
  public float blockHeatCoff = 12f;
  public float baseHeatCapacity = 1250;
  public float productHeat = 1400f;
  public float smokeThreshold = 500;
  public int explosionRadius = 19;
  public int explosionDamageBase = 350;
  
  public Effect explodeEffect = SglFx.reactorExplode;
  
  public Seq<BaseConsumers> coolants = new Seq<>();
  public Seq<BaseConsumers> flues = new Seq<>();
  
  ObjectSet<Item> consItems = new ObjectSet<>();
  
  public NuclearReactor(String name){
    super(name);
    hasEnergy = true;
    outputEnergy = true;
    autoSelect = true;
    canSelect = false;
    
    draw = new DrawNuclearReactor(this);
  }
  
  public void newReact(Item fuel, float time, float output, boolean prodWaste){
    newConsume();
    consume.time(time);
    consume.item(fuel, 1);
    newProduce();
    produce.energy(output);
    if(prodWaste) produce.item(SglItems.nuclear_waste, 1);
    
    flues.add(consume);
  }
  
  public void addCoolant(float consHeat){
    BaseConsumers cons = newOptionalConsume((e, c) -> {
      NuclearReactorBuild entity = e.getBuilding(NuclearReactorBuild.class);
      entity.handleHeat(-consHeat*e.getBuilding().delta());
    }, (s, c) -> {
      s.add(SglStat.effect, t -> {
        t.row();
        t.add(Core.bundle.get("misc.absorbHeat") + ": " + consHeat*60/1000 + SglStatUnit.kHeat.localized() + Core.bundle.get("misc.preSecond"));
      });
    });
    
    Time.run(0, () -> {
      UncConsumeItems<?> ci = cons.get(UncConsumeType.item);
      UncConsumeLiquids<?> cl = cons.get(UncConsumeType.liquid);
      
      float lowTemp = 0;
      if(ci != null) for(ItemStack items: ci.items){
        lowTemp = Math.min(lowTemp, getItemAbsTemperature(items.item));
      }
      if(cl != null) for(LiquidStack liquids: cl.liquids){
        lowTemp = Math.min(lowTemp, getLiquidAbsTemperature(liquids.liquid));
      }
      
      float lowTempF = lowTemp;
      cons.valid = e -> {
        NuclearReactorBuild entity = e.getBuilding(NuclearReactorBuild.class);
        return entity.heat > 0 && entity.absTemperature() > lowTempF;
      };
    });
    
    coolants.add(consume);
  }
  
  public void addTransfer(ItemStack output){
    newOptionalConsume((e, c) -> {}, (e, s) -> {
      e.add(Stat.output, StatValues.items(s.craftTime, output));
    });
    consume.valid = ent -> ent.getBuilding().consValid();
    consume.trigger = ent -> {
      for(int i = 0; i < output.amount; i++){
        ent.getBuilding().handleItem(ent.getBuilding(), output.item);
      }
    };
  }
  
  public void addTransfer(LiquidStack output){
    newOptionalConsume((e, c) -> {
      NuclearReactorBuild entity = e.getBuilding(NuclearReactorBuild.class);
      entity.handleLiquid(entity, output.liquid, output.amount);
    }, (e, s) -> {
      e.add(Stat.output, StatValues.liquid(output.liquid, output.amount*60, true));
    });
    consume.valid = ent -> ent.getBuilding().consValid();
  }
  
  public void addTransfer(GasStack output){
    newOptionalConsume((e, c) -> {
      NuclearReactorBuild entity = e.getBuilding(NuclearReactorBuild.class);
      entity.handleGas(entity, output.gas, output.amount);
    }, (e, s) -> {
      e.add(Stat.output, t -> t.add(new GasDisplay(output.gas, output.amount*60, true, true)));
    });
    consume.valid = ent -> ent.getBuilding().consValid();
  }
  
  @Override
  public void setStats(){
    super.setStats();
    setHeatStats(stats);
    stats.add(SglStat.heatProduct, productHeat*60/1000 + SglStatUnit.kHeat.localized() + Core.bundle.get("misc.preSecond"));
  }
  
  @Override
  public void setBars(){
    super.setBars();
    bars.add("temperature", (NuclearReactorBuild e) -> new Bar(
        () -> Core.bundle.get("misc.temperature"),
        () -> Pal.lightOrange,
        () -> e.temperature() / HeatBuildComp.getTemperature(maxTemperature)
    ));
    bars.add("efficiency", (NuclearReactorBuild e) -> new Bar(
        () -> Core.bundle.get("misc.efficiency") + ": " + Strings.autoFixed(e.smoothEfficiency*100, 0) + "%",
        () -> Pal.accent,
        () -> e.smoothEfficiency
    ));
  }
  
  @Override
  public void init(){
    super.init();
    for(BaseConsumers cons: consumers){
      for(ItemStack stack: cons.get(SglConsumeType.item).items){
        consItems.add(stack.item);
      }
    }
  }
  
  @Override
  public float maxTemperature(){
    return maxTemperature;
  }
  
  @Override
  public float heatCoefficient(){
    return heatCoefficient;
  }
  
  @Override
  public float blockHeatCoff(){
    return blockHeatCoff;
  }
  
  @Override
  public float baseHeatCapacity(){
    return baseHeatCapacity;
  }
  
  public class NuclearReactorBuild extends NormalCrafterBuild implements HeatBuildComp{
    public float heat;
    public float lastMulti;
    
    public float smoothEfficiency;
  
    @Override
    public NormalCrafterBuild create(Block block, Team team){
      super.create(block, team);
      heat = Sgl.atmospheres.current.getAbsTemperature()*heatCapacity();
      
      return this;
    }
  
    @Override
    public NuclearReactor block(){
      return (NuclearReactor)block;
    }
  
    @Override
    public float heat(){
      return heat;
    }
  
    @Override
    public void heat(float heat){
      this.heat = heat;
    }
  
    @Override
    public float heatCapacity(){
      return baseHeatCapacity;
    }
  
    @Override
    public void heatCapacity(float value){}
  
    @Override
    public float productMultiplier(BaseProduce<?> prod){
      if(!(prod instanceof ProduceEnergy) && !(prod instanceof ProducePower)) return 1;
      int total = 0;
      for(BaseConsumers cons: consumer.get()){
        UncConsumeItems<?> ci = cons.get(UncConsumeType.item);
        if(ci == null) return 1;
  
        for(ItemStack stack : ci.items){
          total += items.get(stack.item);
        }
      }
      return lastMulti = (float)total/itemCapacity;
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      if(!consValid()) lastMulti = 0;
      smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, lastMulti*efficiency(), 0.02f);
      heat += lastMulti*productHeat*delta();
      
      if(absTemperature() > maxTemperature){
        onOverTemperature();
      }
      
      dump(SglItems.nuclear_waste);
  
      if(absTemperature() > smokeThreshold){
        float smoke = 1.0f + (absTemperature() - smokeThreshold) / (maxTemperature - smokeThreshold);
        if(Mathf.chance(smoke / 20.0 * delta())){
          Fx.reactorsmoke.at(x + Mathf.range(size * tilesize / 2f),
              y + Mathf.range(size * tilesize / 2f));
        }
      }
      
      swapHeat();
    }
  
    @Override
    public void onOverTemperature(){
      Events.fire(EventType.Trigger.thoriumReactorOverheat);
      kill();
    }
  
    @Override
    public void onDestroyed(){
      Sounds.explosionbig.at(tile);
      int fuel = 0;
      for(BaseConsumers cons: flues){
        for(ItemStack stack: cons.get(SglConsumeType.item).items){
          fuel += items.get(stack.item);
        }
      }
      super.onDestroyed();
      
      if((fuel < itemCapacity/3f && absTemperature() < maxTemperature/2) || !state.rules.reactorExplosions) return;
    
      Effect.shake(8f, 24f, x, y);
      float strength = explosionDamageBase*fuel;
      Damage.damage(x, y, (float) explosionRadius*tilesize, strength);
    
      explodeEffect.at(x, y, 0, (float) explosionRadius*tilesize);
      Angles.randLenVectors(System.nanoTime(), Mathf.random(28, 36), 3f, 7.5f, (x, y) -> {
        float len = Tmp.v1.set(x, y).len();
        Particle.create(this.x, this.y, x, y, Mathf.random(5f, 7f)*((len - 3)/4.5f));
      });
    }
    
    public int majorConsTotal(){
      int result = 0;
      for(BaseConsumers cons: flues){
        for(ItemStack stack: cons.get(SglConsumeType.item).items){
          result += items.get(stack.item);
        }
      }
      return result;
    }
  
    @Override
    public boolean acceptItem(Building source, Item item){
      return super.acceptItem(source, item) && (!consItems.contains(item) || (consItems.contains(item) && majorConsTotal() < itemCapacity));
    }
  }
}
