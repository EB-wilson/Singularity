package singularity.world.blocks.product;

import arc.Core;
import arc.audio.Sound;
import arc.func.Cons;
import arc.func.Func;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.EnumSet;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BlockStatus;
import singularity.Singularity;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.type.SglLiquidStack;
import singularity.world.blocks.SglBlock;
import singularity.world.components.GasBuildComp;
import singularity.world.consumers.SglConsumeGases;
import singularity.world.consumers.SglConsumeType;
import singularity.world.draw.DrawFactory;
import singularity.world.meta.SglBlockStatus;
import singularity.world.meta.SglStat;
import singularity.world.modules.SglProductModule;
import singularity.world.products.ProduceGases;
import singularity.world.products.Producers;
import singularity.world.products.SglProduceType;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.FactoryBlockComp;
import universecore.components.blockcomp.FactoryBuildComp;
import universecore.util.UncLiquidStack;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.UncConsumeLiquids;
import universecore.world.consumers.UncConsumePower;
import universecore.world.producers.BaseProduce;
import universecore.world.producers.BaseProducers;
import universecore.world.producers.ProduceItems;
import universecore.world.producers.ProduceLiquids;

import java.util.ArrayList;

/**常规的工厂类方块，具有强大的consume-produce制造系统的近乎全能的制造类方块*/
@Annotations.ImplEntries
public class NormalCrafter extends SglBlock implements FactoryBlockComp{
  public final ArrayList<BaseProducers> producers = new ArrayList<>();
  
  public float updateEffectChance = 0.05f;
  public Effect updateEffect = Fx.none;
  public Effect craftEffect = Fx.none;
  public float effectRange = -1;
  
  public Sound craftedSound = Sounds.none;
  public float craftedSoundVolume = 0.5f;
  
  public boolean shouldConfig;
  
  /**同样的，这也是一个指针，指向当前编辑的produce*/
  public Producers produce;
  
  public Cons<? extends NormalCrafterBuild> craftTrigger;
  public Cons<? extends NormalCrafterBuild> crafting;
  
  public float warmupSpeed = 0.02f;
  public float stopSpeed = 0.02f;
  
  /**常规的工厂类方块，具有强大的consume-produce制造系统的近乎全能的制造类方块*/
  public NormalCrafter(String name) {
    super(name);
    update = true;
    solid = true;
    sync = true;
    ambientSound = Sounds.machine;
    ambientSoundVolume = 0.03f;
    draw = new DrawFactory<>(this);
    flags = EnumSet.of(BlockFlag.factory);
  }
  
  @Override
  public Producers newProduce(){
    produce = new Producers();
    this.producers().add(produce);
    return produce;
  }

  @Override
  public void init(){
    if(effectRange == -1) effectRange = size;
    
    if(producers.size() > 0) for(BaseProducers prod: producers){
      hasItems |= outputItems |= prod.get(SglProduceType.item) != null;
      hasLiquids |= outputsLiquid |= prod.get(SglProduceType.liquid) != null;
      hasPower |= outputsPower |= prod.get(SglProduceType.power) != null && prod.get(SglProduceType.power).powerProduction != 0;
      hasGases |= outputGases |= prod.get(SglProduceType.gas) != null;
      hasEnergy |= outputEnergy |= prod.get(SglProduceType.energy) != null;
    }
    
    super.init();
  
    if(producers.size() > 1) configurable = canSelect;
    if(shouldConfig) configurable = true;
  }

  @Override
  public void load() {
    super.load();
    if(displaySelectPrescripts) prescriptSelector = Core.atlas.has(name + "_prescriptSelector")?
    Core.atlas.find(name + "_prescriptSelector"): Singularity.getModAtlas("prescriptSelector" + size);
  }

  @Override
  public void setStats() {
    super.setStats();
    if(producers.size() > 1){
      stats.add(SglStat.autoSelect, autoSelect);
      stats.add(SglStat.controllable, canSelect);
    }
  }

  @Override
  public TextureRegion[] icons(){
    return draw.icons();
  }

  @SuppressWarnings("unchecked")
  @Annotations.ImplEntries
  public class NormalCrafterBuild extends SglBuilding implements FactoryBuildComp{
    private final Seq<Liquid> tempLiquid = new Seq<>();
    
    public SglProductModule producer;
    
    public Seq<Item> outputItems;
    public Seq<Liquid> outputLiquids;
    public Seq<Gas> outputGases;
    
    public float powerProdEfficiency;
  
    @Override
    public NormalCrafterBuild create(Block block, Team team) {
      super.create(block, team);
      producer = new SglProductModule(this);
      return this;
    }
    
    public void produce(){
      producer.trigger();
    }
  
    @Override
    public void reset(){
      super.reset();
      progress(0);
    }
  
    @Override
    public void updateDisplayLiquid() {
      if(!block.hasLiquids) return;
      displayLiquids.clear();
      
      tempLiquid.clear();
      if(recipeCurrent >= 0 && consumer.current != null){
        if(consumer.current.get(SglConsumeType.liquid) != null) for(UncLiquidStack stack : consumer.current.get(SglConsumeType.liquid).liquids) {
          tempLiquid.add(stack.liquid);
        }
      }
      if(recipeCurrent >= 0 && producer.current != null) {
        if(producer.current.get(SglProduceType.liquid) != null) for(UncLiquidStack stack : producer.current.get(SglProduceType.liquid).liquids) {
          tempLiquid.add(stack.liquid);
        }
      }
      liquids.each((key, val) -> {
        if(! tempLiquid.contains(key) && val > 0.1f) displayLiquids.add(new SglLiquidStack(key, val));
      });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void displayBars(Table bars){
      if(recipeCurrent != -1 && producer.current != null && block.hasPower && block.outputsPower && producer.current.get(SglProduceType.power) != null){
        float productPower = powerProdEfficiency*producer.current.get(SglProduceType.power).powerProduction;
        UncConsumePower<NormalCrafterBuild> cp;
        float consPower = consumesPower && consumer.current != null && (cp = (UncConsumePower<NormalCrafterBuild>) consumer.current.get(SglConsumeType.power)) != null?
            cp.usage*cp.multiple(this): 0;
        Func<Building, Bar> bar = (entity -> new Bar(
          () -> Core.bundle.format("bar.poweroutput",Strings.fixed(Math.max(productPower-consPower, 0) * 60 * entity.timeScale(), 1)),
          () -> Pal.powerBar,
          () -> powerProdEfficiency
        ));
        bars.add(bar.get(this)).growX();
        bars.row();
      }
      super.displayBars(bars);
      if(recipeCurrent == -1 || producer.current == null) return;
  
      UncConsumeLiquids<?> cl = consumer.current.get(SglConsumeType.liquid);
      SglConsumeGases<?> cg = consumer.current.get(SglConsumeType.gas);
  
      ProduceLiquids<?> pl = producer.current.get(SglProduceType.liquid);
      ProduceGases<?> pg = producer.current.get(SglProduceType.gas);
      if(pl != null || pg != null){
        bars.table(cl == null && cg == null? Tex.buttonEdge1: Tex.pane, t -> t.left().add(Core.bundle.get("fragment.bars.product")).pad(4)).pad(0).height(38);
        bars.row();
        bars.table(t -> {
          t.defaults().grow().margin(0);
          if(pl != null){
            t.table(pg == null? Tex.buttonRight: Tex.pane2, liquid -> {
              liquid.defaults().growX().margin(0).pad(4).height(18);
              liquid.add(Core.bundle.get("misc.liquid")).color(Pal.gray);
              liquid.row();
              for(UncLiquidStack stack: pl.liquids){
                Func<Building, Bar> bar = (entity -> new Bar(
                  () -> stack.liquid.localizedName,
                  () -> stack.liquid.barColor != null? stack.liquid.barColor: stack.liquid.color,
                  () -> Math.min(entity.liquids.get(stack.liquid) / entity.block().liquidCapacity, 1f)
                ));
                liquid.add(bar.get(this));
                liquid.row();
              }
            });
          }
      
          if(pg != null){
            t.table(Tex.buttonRight, gases -> {
              gases.defaults().growX().margin(0).pad(4).height(18);
              gases.left().add(Core.bundle.get("misc.gas")).color(Pal.gray);
              gases.row();
              for(GasStack stack: pg.gases){
                Func<Building, Bar> bar = (ent -> {
                  GasBuildComp entity = (GasBuildComp) ent;
                  return new Bar(
                    () -> stack.gas.localizedName,
                    () -> stack.gas.color,
                    () -> Math.min(entity.gases().get(stack.gas) / entity.getGasBlock().gasCapacity(), 1f)
                  );
                });
                gases.add(bar.get(this));
                gases.row();
              }
            });
          }
        }).height(46 + Math.max((pl != null? pl.liquids.length: 0), (pg != null? pg.gases.length: 0))*26).padTop(2);
      }
    }
  
    @Override
    public Seq<Item> outputItems(){
      if(recipeCurrent == -1) return null;
      return outputItems;
    }
  
    @Override
    public Seq<Liquid> outputLiquids(){
      if(recipeCurrent == -1) return null;
      return outputLiquids;
    }
  
    @Override
    public Seq<Gas> outputGases(){
      return outputGases;
    }
  
    @Override
    public boolean shouldConsume(){
      return producer.valid() && super.shouldConsume();
    }
  
    @Override
    public BlockStatus status(){
      if(autoSelect && !canSelect && recipeCurrent == -1) return BlockStatus.noInput;
      return super.status();
    }

    @Override
    public void updateTile() {
      if(updateRecipe){
        if(producer.current.get(SglProduceType.item) != null) outputItems = new Seq<>(producer.current.get(SglProduceType.item).items).map(e -> e.item);
        if(producer.current.get(SglProduceType.liquid) != null) outputLiquids = new Seq<>(producer.current.get(SglProduceType.liquid).liquids).map(e -> e.liquid);
        if(producer.current.get(SglProduceType.gas) != null) outputGases = new Seq<>(producer.current.get(SglProduceType.gas).gases).map(e -> e.gas);
      }
    }
  
    @Override
    public void onUpdateCurrent(){
      super.onUpdateCurrent();
      producer.setCurrent();
    }

    @SuppressWarnings("unchecked")
    @Override
    public float getPowerProduction(){
      if(!outputsPower || producer.current == null || producer.current.get(SglProduceType.power) == null) return 0;
      powerProdEfficiency = Mathf.num(shouldConsume() && consValid())*((BaseProduce<NormalCrafterBuild>)producer.current.get(SglProduceType.power)).multiple(this);
      return producer.getPowerProduct()*efficiency();
    }
  
    @Override
    public float efficiency(){
      return super.efficiency()*warmup();
    }
    
    @Override
    public void buildConfiguration(Table table){
      if(status == SglBlockStatus.broken || !canSelect) return;

      if(producers.size() > 1){
        Table prescripts = new Table(Tex.buttonTrans);
        prescripts.defaults().grow().marginTop(0).marginBottom(0).marginRight(5).marginRight(5);
        prescripts.add(Core.bundle.get("fragment.buttons.selectPrescripts")).padLeft(5).padTop(5).padBottom(5);
        prescripts.row();

        TextureRegion icon;
        Table buttons = new Table();
        for(int i=0; i<producers.size(); i++){
          int s = i;
          BaseProducers p = producers.get(i);
          BaseConsumers c = consumers.get(i);

          if(c.selectable.get() == BaseConsumers.Visibility.hidden) continue;

          icon = p.icon != null? p.icon.get(): c.icon.get();

          ImageButton button = new ImageButton(icon, Styles.selecti);
          button.touchablility = () -> c.selectable.get().buttonValid;
          button.clicked(() -> {
            configure(s);
          });
          button.update(() -> button.setChecked(recipeCurrent == s));
          buttons.add(button).size(50, 50);
          if((i+1) % 4 == 0) buttons.row();
        }

        prescripts.add(buttons);
        table.add(prescripts);
        table.row();
      }
    }

    @Override
    public NormalCrafter block() {
      return NormalCrafter.this;
    }

    @Override
    public void draw(){
      super.draw();
      drawSelectRecipe(this);
      drawStatus();
      Draw.blend();
    }
    
    public void drawSelectRecipe(NormalCrafterBuild entity){
      if(entity.block().displaySelectPrescripts && entity.recipeCurrent != -1){
        BaseProducers current = producer.current;
        ProduceItems<?> produceItems = current.get(SglProduceType.item);
        ProduceLiquids<?> produceLiquids = current.get(SglProduceType.liquid);
        Color color = current.color != null? current.color: produceItems.items != null? produceItems.items[0].item.color: produceLiquids.liquids != null? produceLiquids.liquids[0].liquid.color: Items.copper.color;
        Draw.color(color);
        Draw.rect(entity.block().prescriptSelector, entity.x, entity.y);
        Draw.color();
      }
    }
  
    @Override
    public void write(Writes write) {
      super.write(write);
      write.bool(recipeSelected);
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      recipeSelected = read.bool();
    }

    @Override
    public void crafted(){
      craftEffect.at(getX(), getY());
      if(craftTrigger != null) ((Cons<NormalCrafterBuild>)craftTrigger).get(this);
      if(craftedSound != Sounds.none) craftedSound.at(x, y, 1, craftedSoundVolume);
    }

    @Override
    public void crafting(){
      if(Mathf.chanceDelta(updateEffectChance)){
        updateEffect.at(getX() + Mathf.range(effectRange * 4f), getY() + Mathf.range(effectRange * 4));
      }

      if(crafting != null) ((Cons<NormalCrafterBuild>)crafting).get(this);
    }
  }
}