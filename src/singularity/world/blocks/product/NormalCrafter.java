package singularity.world.blocks.product;

import arc.Core;
import arc.audio.Sound;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Floatp;
import arc.graphics.Color;
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
import mindustry.gen.Sounds;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.ui.ItemDisplay;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Stat;
import mindustry.world.meta.Stats;
import singularity.type.SglLiquidStack;
import singularity.world.blocks.SglBlock;
import singularity.world.consumers.SglConsumeType;
import singularity.world.meta.SglStat;
import singularity.world.modules.SglProductModule;
import singularity.world.products.Producers;
import singularity.world.products.SglProduceType;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.FactoryBlockComp;
import universecore.components.blockcomp.FactoryBuildComp;
import universecore.components.blockcomp.ProducerBlockComp;
import universecore.components.blockcomp.ProducerBuildComp;
import universecore.util.UncLiquidStack;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumeLiquidBase;
import universecore.world.consumers.ConsumePower;
import universecore.world.consumers.ConsumeType;
import universecore.world.producers.BaseProduce;
import universecore.world.producers.BaseProducers;
import universecore.world.producers.ProduceLiquids;
import universecore.world.producers.ProduceType;

/**常规的工厂类方块，具有强大的consume-produce制造系统的近乎全能的制造类方块*/
@Annotations.ImplEntries
public class NormalCrafter extends SglBlock implements FactoryBlockComp{
  public float updateEffectChance = 0.04f;
  public Effect updateEffect = Fx.none;
  public Color updateEffectColor = Color.white;
  public Effect craftEffect = Fx.none;
  public Color craftEffectColor = Color.white;
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

  public Seq<Item> willDumpItems = new Seq<>();
  public Seq<Liquid> willDumpLiquids = new Seq<>();

  /**常规的工厂类方块，具有强大的consume-produce制造系统的近乎全能的制造类方块*/
  public NormalCrafter(String name) {
    super(name);
    update = true;
    solid = true;
    sync = true;
    conductivePower = true;
    ambientSound = Sounds.machine;
    ambientSoundVolume = 0.03f;
    flags = EnumSet.of(BlockFlag.factory);
  }
  
  @Override
  public Producers newProduce(){
    produce = new Producers();
    this.producers().add(produce);
    return produce;
  }

  /**对当前生产清单设置随机副产物
   *
   * @param chance 副产物产出的机会，为任意大于0的值，实际机会计算值从0到chance之间随机取值，结果将超出1的部分直接作为产出数量，与1取模后剩余机会取{@link Mathf#chance(double)}
   * @param item 副产物物品*/
  public void setByProduct(float chance, Item item){
    setByProduct(0, chance, item);
  }

  /**对当前生产清单设置随机副产物
   *
   * @param base 最小产出量，机会的计算结果会直接加上这个数值
   * @param chance 副产物产出的机会，为任意大于0的值，实际机会计算值从0到chance之间随机取值，结果将超出1的部分直接作为产出数量，与1取模后剩余机会取{@link Mathf#chance(double)}
   * @param item 副产物物品*/
  public void setByProduct(int base, float chance, Item item){
    consume.addSelfAccess(ConsumeType.item, item);
    consume.setConsTrigger((NormalCrafterBuild e) -> {
      float chanceV = Mathf.random(chance) + base;
      while(chanceV >= 1){
        if(e.acceptItem(e, Items.thorium)) e.offload(Items.thorium);
        chanceV--;
      }

      if(Mathf.chance(chanceV)) if(e.acceptItem(e, Items.thorium)) e.offload(Items.thorium);
    });
    Cons2<Stats, BaseConsumers> old = consume.display;
    consume.display = (s, c) -> s.add(Stat.output, t -> {
      old.get(s, c);
      t.row();
      t.table(i -> {
        i.add(Core.bundle.get("misc.extra") + ":");
        i.add(new ItemDisplay(item, base)).left().padLeft(6);
        i.add("[gray]" + (base > 0? " +": "") + Strings.autoFixed(chance*100, 2) + "%[]");
      }).left().padLeft(5);
    });
    willDumpItems.add(item);
  }

  @Override
  public void init(){
    if(effectRange == -1) effectRange = size;
    
    if(producers().size > 0) for(BaseProducers prod: producers()){
      hasItems |= outputItems |= prod.get(SglProduceType.item) != null;
      hasLiquids |= outputsLiquid |= prod.get(SglProduceType.liquid) != null;
      hasPower |= outputsPower |= prod.get(SglProduceType.power) != null && prod.get(SglProduceType.power).powerProduction != 0;
      hasEnergy |= outputEnergy |= prod.get(SglProduceType.energy) != null;
    }

    for (BaseProducers producer : producers()) {
      outputsPayload |= producer.get(ProduceType.payload) != null;
    }
    
    super.init();
  
    if(producers().size > 1) configurable = canSelect;
    if(shouldConfig) configurable = true;
  }

  @Override
  public void setStats() {
    super.setStats();
    if(producers().size > 1){
      stats.add(SglStat.autoSelect, autoSelect);
      stats.add(SglStat.controllable, canSelect);
    }
  }

  @SuppressWarnings("unchecked")
  @Annotations.ImplEntries
  public class NormalCrafterBuild extends SglBuilding implements FactoryBuildComp{
    private final Seq<Liquid> tempLiquid = new Seq<>();
    
    public SglProductModule producer;
    
    public Seq<Item> outputItems;
    public Seq<Liquid> outputLiquids;
    
    public float powerProdEfficiency;

    @Override
    public NormalCrafterBuild create(Block block, Team team) {
      super.create(block, team);
      producer = new SglProductModule(this);
      return this;
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
        if(consumer.current.get(SglConsumeType.liquid) != null){
          for(UncLiquidStack stack: consumer.current.get(SglConsumeType.liquid).consLiquids){
            tempLiquid.add(stack.liquid);
          }
        }
      }
      if(recipeCurrent >= 0 && producer.current != null) {
        if(producer.current.get(SglProduceType.liquid) != null){
          for(UncLiquidStack stack : producer.current.get(SglProduceType.liquid).liquids) {
            tempLiquid.add(stack.liquid);
          }
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
        Floatp prod = () -> powerProdEfficiency*producer.current.get(SglProduceType.power).powerProduction;
        Floatp cons = () -> {
          ConsumePower<NormalCrafterBuild> cp;
          return consumesPower && consumer.current != null && (cp =
              (ConsumePower<NormalCrafterBuild>) consumer.current.get(SglConsumeType.power)) != null?
              cp.usage*cp.multiple(this): 0;
        };
        bars.add(new Bar(
            () -> Core.bundle.format("bar.poweroutput", Strings.fixed(Math.max(prod.get() - cons.get(), 0)*60*timeScale(), 1)),
            () -> Pal.powerBar,
            () -> powerProdEfficiency
        )).growX();
        bars.row();
      }
      super.displayBars(bars);
      if(recipeCurrent == -1 || producer.current == null || consumer.current == null) return;
  
      ConsumeLiquidBase<?> cl = consumer.current.get(SglConsumeType.liquid);
  
      ProduceLiquids<?> pl = producer.current.get(SglProduceType.liquid);
      if(pl != null){
        bars.table(cl == null? Tex.buttonEdge1: Tex.pane, t -> t.left().add(Core.bundle.get("fragment.bars.product")).pad(4)).pad(0).height(38);
        bars.row();
        bars.table(t -> {
          t.defaults().grow().margin(0);
          t.table(Tex.pane2, liquid -> {
            liquid.defaults().growX().margin(0).pad(4).height(18);
            liquid.add(Core.bundle.get("misc.liquid")).color(Pal.gray);
            liquid.row();
            for(UncLiquidStack stack: pl.liquids){
              liquid.add(new Bar(
                  () -> stack.liquid.localizedName,
                  () -> stack.liquid.barColor != null? stack.liquid.barColor: stack.liquid.color,
                  () -> Math.min(liquids.get(stack.liquid) / block.liquidCapacity, 1f)
              ));
              liquid.row();
            }
          });
        }).height(46 + pl.liquids.length*26).padTop(2);
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
    public float consEfficiency(){
      return super.consEfficiency()*warmup();
    }

    @Override
    public BlockStatus status(){
      if(autoSelect && !canSelect && recipeCurrent == -1) return BlockStatus.noInput;
      return super.status();
    }

    @Override
    public boolean shouldConsume(){
      return super.shouldConsume() && productValid();
    }

    @Override
    public void updateTile() {
      if(updateRecipe && producer.current != null){
        if(producer.current.get(SglProduceType.item) != null) outputItems = new Seq<>(producer.current.get(SglProduceType.item).items).map(e -> e.item);
        if(producer.current.get(SglProduceType.liquid) != null) outputLiquids = new Seq<>(producer.current.get(SglProduceType.liquid).liquids).map(e -> e.liquid);
      }

      for (Item item : willDumpItems) {
        dump(item);
      }

      for (Liquid liquid : willDumpLiquids) {
        dumpLiquid(liquid);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public float getPowerProduction(){
      if(!outputsPower || producer.current == null || producer.current.get(SglProduceType.power) == null) return 0;
      powerProdEfficiency = Mathf.num(shouldConsume() && consumeValid())*consEfficiency()
          *((BaseProduce<NormalCrafterBuild>)producer.current.get(SglProduceType.power)).multiple(this);
      return producer.getPowerProduct()*consEfficiency();
    }
    
    @Override
    public void buildConfiguration(Table table){
      if(producers().size > 1){
        Table prescripts = new Table(Tex.buttonTrans);
        prescripts.defaults().grow().marginTop(0).marginBottom(0).marginRight(5).marginRight(5);
        prescripts.add(Core.bundle.get("fragment.buttons.selectPrescripts")).padLeft(5).padTop(5).padBottom(5);
        prescripts.row();

        TextureRegion icon;
        Table buttons = new Table();
        for(int i=0; i<producers().size; i++){
          int s = i;
          BaseProducers p = producers().get(i);
          BaseConsumers c = consumers().get(i);

          if(c.selectable.get() == BaseConsumers.Visibility.hidden) continue;

          icon = p.icon();

          ImageButton button = new ImageButton(icon, Styles.selecti);
          button.touchablility = () -> c.selectable.get().buttonValid;
          button.clicked(() -> configure(s));
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
    public double sense(LAccess sensor){
      if(sensor == LAccess.progress){
        progress();
      }

      return super.sense(sensor);
    }

    @Override
    public NormalCrafter block() {
      return NormalCrafter.this;
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
    public void craftTrigger(){
      craftEffect.at(getX(), getY(), craftEffectColor);
      if(craftTrigger != null) ((Cons<NormalCrafterBuild>)craftTrigger).get(this);
      if(craftedSound != Sounds.none) craftedSound.at(x, y, 1, craftedSoundVolume);
    }

    @Override
    public void onCraftingUpdate(){
      if(Mathf.chanceDelta(updateEffectChance)){
        updateEffect.at(getX() + Mathf.range(effectRange * 4f), getY() + Mathf.range(effectRange * 4), updateEffectColor);
      }

      if(crafting != null) ((Cons<NormalCrafterBuild>)crafting).get(this);
    }
  }
}