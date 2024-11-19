package singularity.world.blocks.nuclear;

import arc.Core;
import arc.Events;
import arc.audio.Sound;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.ui.Bar;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;
import singularity.contents.SglItems;
import singularity.world.SglFx;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.consumers.SglConsumeType;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import singularity.world.particles.SglParticleModels;
import universecore.annotations.Annotations;
import universecore.world.consumers.BaseConsumers;

import static mindustry.Vars.state;
import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class NuclearReactor extends NormalCrafter{
  public float maxHeat = 100f;
  public float productHeat = 0.2f;
  public float smokeThreshold = 50;
  public int explosionRadius = 19;
  public int explosionDamageBase = 350;
  
  public Effect explodeEffect = SglFx.reactorExplode;
  
  public Seq<BaseConsumers> coolants = new Seq<>();
  public Seq<BaseConsumers> fuels = new Seq<>();
  public Sound explosionSound = Sounds.largeExplosion;
  public float explosionSoundVolume = 3.5f, explosionSoundPitch = 0.8f;
  
  ObjectSet<Item> consItems = new ObjectSet<>();

  public NuclearReactor(String name){
    super(name);
    hasEnergy = true;
    oneOfOptionCons = false;
    outputEnergy = true;
    autoSelect = true;
    canSelect = false;
  }
  
  public void newReact(Item fuel, float time, float output, boolean prodWaste){
    newConsume();
    consume.time(time);
    consume.item(fuel, 1);

    newProduce();
    produce.energy(output);
    if(prodWaste) produce.item(SglItems.nuclear_waste, 1);
    
    fuels.add(consume);
  }
  
  public void addCoolant(float consHeat){
    BaseConsumers cons = newOptionalConsume((e, c) -> {
      NuclearReactorBuild entity = e.getBuilding(NuclearReactorBuild.class);
      entity.heat -= consHeat*entity.delta();
      entity.heat = Math.max(entity.heat, 0);
    }, (s, c) -> {
      s.add(SglStat.effect, t -> {
        t.row();
        t.add(Core.bundle.get("misc.absorbHeat") + ": " + Strings.autoFixed(consHeat*60, 2)
            + SglStatUnit.heat.localized() + Core.bundle.get("misc.perSecond"));
      });
    });

    cons.setConsDelta(e -> e.getBuilding().delta())
        .consValidCondition((NuclearReactorBuild e) -> e.heat > 0)
        .optionalAlwaysValid = true;
    
    coolants.add(consume);
  }
  
  public void addTransfer(ItemStack output){
    newOptionalProduct();
    consume.optionalAlwaysValid = false;
    produce.item(output.item, output.amount);
  }
  
  public void addTransfer(LiquidStack output){
    newOptionalProduct();
    consume.optionalAlwaysValid = false;
    produce.liquid(output.liquid, output.amount);
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(SglStat.heatProduct, Strings.autoFixed(productHeat*60, 2) + SglStatUnit.heat.localized() + Core.bundle.get("misc.perSecond"));
    stats.add(SglStat.maxHeat, maxHeat, SglStatUnit.heat);
  }
  
  @Override
  public void setBars(){
    super.setBars();
    addBar("efficiency", (NuclearReactorBuild e) -> new Bar(
        () -> Core.bundle.get("misc.efficiency") + ": " + Strings.autoFixed(e.smoothEfficiency*100, 0) + "%",
        () -> Pal.accent,
        () -> e.smoothEfficiency
    ));
    addBar("heat", (NuclearReactorBuild e) -> new Bar(
        Core.bundle.get("misc.heat"),
        Pal.lightOrange,
        () -> e.heat/maxHeat
    ));
  }
  
  @Override
  public void init(){
    super.init();
    for(BaseConsumers cons: consumers()){
      for(ItemStack stack: cons.get(SglConsumeType.item).consItems){
        consItems.add(stack.item);
      }
    }
  }
  
  @Annotations.ImplEntries
  public class NuclearReactorBuild extends NormalCrafterBuild{
    public float heat;
    
    public float smoothEfficiency;
  
    @Override
    public NuclearReactor block(){
      return (NuclearReactor)block;
    }

    @Override
    public float consEfficiency(){
      return (float) fuelItemsTotal()/itemCapacity*super.consEfficiency()*(1 - Mathf.clamp((items.get(SglItems.nuclear_waste) - itemCapacity/3f)/(itemCapacity), 0, 1));
    }

    @Override
    public boolean shouldConsume() {
      return super.shouldConsume() && (items == null || items.get(SglItems.nuclear_waste) < itemCapacity);
    }

    @Override
    public void updateTile(){
      super.updateTile();
      smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, consEfficiency(), 0.02f);
      heat += productHeat*consumer.consDelta();
      
      if(heat > maxHeat){
        onOverTemperature();
      }
      
      dump(SglItems.nuclear_waste);
  
      if(heat > smokeThreshold){
        float smoke = 1.0f + (heat - smokeThreshold) / (maxHeat - smokeThreshold);
        if(Mathf.chance(smoke / 20.0 * delta())){
          Fx.reactorsmoke.at(x + Mathf.range(size * tilesize / 2f),
              y + Mathf.range(size * tilesize / 2f));
        }
      }
    }

    @Override
    public double sense(LAccess sensor){
      if(sensor == LAccess.heat){
        return heat/maxHeat;
      }

      return super.sense(sensor);
    }

    public void onOverTemperature(){
      Events.fire(EventType.Trigger.thoriumReactorOverheat);
      kill();
    }
  
    @Override
    public void onDestroyed(){
      int fuel = 0;
      for(BaseConsumers cons: fuels){
        for(ItemStack stack: cons.get(SglConsumeType.item).consItems){
          fuel += items.get(stack.item);
        }
      }
      super.onDestroyed();
      
      if((fuel < itemCapacity/3f && heat < maxHeat/2) || !state.rules.reactorExplosions) return;
    
      Effect.shake(8f, 120f, x, y);
      float strength = explosionDamageBase*fuel;
      Damage.damage(x, y, (float) explosionRadius*tilesize, strength);

      explosionSound.at(x, y, explosionSoundPitch, explosionSoundVolume);
    
      explodeEffect.at(x, y, 0, (float) explosionRadius*tilesize);
      Angles.randLenVectors(System.nanoTime(), Mathf.random(28, 36), 3f, 7.5f, (x, y) -> {
        float len = Tmp.v1.set(x, y).len();
        SglParticleModels.floatParticle.create(this.x, this.y, Pal.reactorPurple, x, y, Mathf.random(5f, 7f)*((len - 3)/4.5f));
      });
    }
    
    public int fuelItemsTotal(){
      int result = 0;
      for(BaseConsumers cons: fuels){
        for(ItemStack stack: cons.get(SglConsumeType.item).consItems){
          result += items.get(stack.item);
        }
      }
      return result;
    }
  
    @Override
    public boolean acceptItem(Building source, Item item){
      return super.acceptItem(source, item) && (!consItems.contains(item) || (consItems.contains(item) && fuelItemsTotal() < itemCapacity));
    }
  }
}
