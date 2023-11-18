package singularity.world.blocks.product;

import arc.Core;
import arc.audio.Sound;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Floatf;
import arc.func.Floatp;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.Rand;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.Scaling;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Sounds;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.ui.Bar;
import mindustry.ui.ItemDisplay;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.meta.*;
import singularity.Sgl;
import singularity.contents.SglItems;
import singularity.graphic.SglDrawConst;
import singularity.ui.StatUtils;
import singularity.world.blocks.SglBlock;
import singularity.world.consumers.SglConsumeType;
import singularity.world.meta.SglStat;
import singularity.world.modules.SglProductModule;
import singularity.world.products.Producers;
import singularity.world.products.SglProduceType;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.components.blockcomp.FactoryBlockComp;
import universecore.components.blockcomp.FactoryBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumePower;
import universecore.world.consumers.ConsumeType;
import universecore.world.producers.BaseProduce;
import universecore.world.producers.BaseProducers;
import universecore.world.producers.ProduceLiquids;
import universecore.world.producers.ProduceType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**常规的工厂类方块，具有强大的consume-produce制造系统的近乎全能的制造类方块*/
@Annotations.ImplEntries
public class NormalCrafter extends SglBlock implements FactoryBlockComp{
  protected Rand rand = new Rand();

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

  public OrderedMap<BaseConsumers, Byproduct> byproducts = new OrderedMap<>();
  public OrderedMap<BaseConsumers, BaseProducers> optionalProducts = new OrderedMap<>();

  public ObjectFloatMap<BaseConsumers> boosts = new ObjectFloatMap<>();

  /**常规的工厂类方块，具有强大的consume-produce制造系统的近乎全能的制造类方块*/
  public NormalCrafter(String name) {
    super(name);
    update = true;
    solid = true;
    sync = true;
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

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void newOptionalProduct(){
    Producers prod = produce = new Producers();
    newOptionalConsume((NormalCrafterBuild e, BaseConsumers c) -> {
      for (BaseProduce baseProduce : prod.all()) {
        if (baseProduce.valid(e)) baseProduce.update(e);
        baseProduce.dump(e);
      }
    }, (s, c) -> {
      prod.display(s);
    });
    prod.cons = consume;
    consume.setConsTrigger((NormalCrafterBuild e) -> {
      for (BaseProduce baseProduce : prod.all()) {
        if (baseProduce.valid(e)) baseProduce.produce(e);
      }
    });

    optionalProducts.put(consume, prod);
  }

  public static class Byproduct{
    public Item item;
    public float chance;
    public int base;

    public Byproduct(Item item, float chance, int base){
      this.item = item;
      this.chance = chance;
      this.base = base;
    }
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
    byproducts.put(consume, new Byproduct(item, chance, base));

    consume.addSelfAccess(ConsumeType.item, item);
    consume.setConsTrigger((NormalCrafterBuild e) -> {
      float chanceV = chance + base;
      while(chanceV >= 1){
        if(e.acceptItem(e, item)) e.offload(item);
        chanceV--;
      }

      if(rand.chance(chanceV)) if(e.acceptItem(e, item)) e.offload(item);
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
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public BaseConsumers newBooster(float boost){
    Floatf<NormalCrafterBuild>[] fun = new Floatf[1];
    BaseConsumers res = newOptionalConsume((NormalCrafterBuild e, BaseConsumers c) -> {
      e.currBoost = fun[0];
      e.mark = 2;
    }, (s, c) -> {
      s.add(Stat.boostEffect, t -> {
        t.table(req -> {
          req.left().defaults().left().padLeft(3);
          for (BaseConsume<? extends ConsumerBuildComp> co : c.all()) {
            co.buildIcons(req);
          }
        }).left().padRight(40);
        t.add(Core.bundle.get("misc.efficiency") + Strings.autoFixed(boost*100, 1) + "%").growX().right();
      });
    });
    boosts.put(res, boost);

    fun[0] = e -> {
      float mul = 1;
      for (BaseConsume cons : res.all()) {
        mul *= cons.efficiency(e);
      }
      return boost*mul*Mathf.clamp(e.consumer.consEfficiency)*e.consumer.getOptionalEff(res);
    };

    consume.customDisplayOnly = true;
    consume.optionalAlwaysValid = false;

    return res;
  }

  @Override
  public void setBars() {
    super.setBars();
    addBar("efficiency", (NormalCrafterBuild e) -> new Bar(
        () -> Core.bundle.get("misc.efficiency") + ": " + Mathf.round(e.workEfficiency()*100) + "%",
        () -> Pal.accent,
        e::workEfficiency
    ));
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

    for (ObjectMap.Entry<BaseConsumers, BaseProducers> product : optionalProducts) {
      for (BaseProduce<?> baseProduce : product.value.all()) {
        baseProduce.parent.cons = product.key;
      }
    }
    
    super.init();
  
    if(producers().size > 1 && canSelect) configurable = true;
    if(shouldConfig) configurable = true;
  }

  @Override
  public void setStats() {
    super.setStats();
    if(producers().size > 1){
      stats.add(SglStat.autoSelect, autoSelect);
      stats.add(SglStat.controllable, canSelect);
    }

    stats.add(SglStat.recipes, t -> {
      t.left().row();
      t.add(Core.bundle.get("infos.touchShowDetails")).color(Color.gray).left();
      t.row();
      for (int i = 0; i < consumers.size; i++) {
        BaseConsumers cons = consumers.get(i);
        BaseProducers prod = producers().get(i);

        Table details = new Table();
        FactoryBlockComp.buildRecipe(details, cons, prod);

        Table simple = new Table(ta -> {
          ta.left().defaults().left();

          if (cons.showTime){
            ta.stack(
                new Table(o -> {
                  o.left();
                  o.add(new Image(SglDrawConst.time)).size(32f).scaling(Scaling.fit);
                }),
                new Table(o -> {
                  o.left().bottom();
                  o.add(Strings.autoFixed(cons.craftTime/60, 1) + StatUnit.seconds.localized()).style(Styles.outlineLabel);
                  o.pack();
                })
            );
            ta.add(" > ");
          }

          buildRecipeSimple(cons, prod, ta);
        });

        AtomicBoolean isSim = new AtomicBoolean(false);
        AtomicReference<Runnable> rebuild = new AtomicReference<>();

        t.table(SglDrawConst.grayUI, ta -> {
          rebuild.set(() -> {
            ta.clearChildren();
            ta.left().add(isSim.get()? details: simple);

            isSim.set(!isSim.get());
          });
          rebuild.get().run();

          ta.touchable = Touchable.enabled;
        }).margin(8).left().growX().fillY().pad(3).get().clicked(() -> {
          rebuild.get().run();
        });
        t.row();
      }
    });
  }

  private static void buildRecipeSimple(BaseConsumers cons, BaseProducers prod, Table ta) {
    boolean first = true;
    for (BaseConsume<? extends ConsumerBuildComp> consume : cons.all()) {
      if (!consume.hasIcons()) continue;

      if (!first) ta.add("+").fillX().pad(4);
      ta.table(c -> {
        c.defaults().padLeft(3).fill();

        consume.buildIcons(c);
      }).fill();

      first = false;
    }

    ta.image(Icon.right).padLeft(8).padRight(8).size(30);

    first = true;
    for (BaseProduce<? extends ConsumerBuildComp> produce : prod.all()) {
      if (!produce.hasIcons()) continue;

      if (!first) ta.add("+").fillX().pad(4);
      ta.table(c -> {
        c.defaults().padLeft(3).fill();

        produce.buildIcons(c);
      }).fill();

      first = false;
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

    public Floatf<NormalCrafterBuild> currBoost = e -> 1;
    public float real;
    public int mark;

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
          for(LiquidStack stack: consumer.current.get(SglConsumeType.liquid).consLiquids){
            tempLiquid.add(stack.liquid);
          }
        }
      }
      if(recipeCurrent >= 0 && producer.current != null) {
        if(producer.current.get(SglProduceType.liquid) != null){
          for(LiquidStack stack : producer.current.get(SglProduceType.liquid).liquids) {
            tempLiquid.add(stack.liquid);
          }
        }
      }
      liquids.each((key, val) -> {
        if(! tempLiquid.contains(key) && val > 0.1f) displayLiquids.add(new LiquidStack(key, val));
      });
    }

    @Override
    public void displayBars(Table bars){
      super.displayBars(bars);

      if(recipeCurrent == -1 || producer.current == null || consumer.current == null) return;

      Table tab = new Table();
      buildProducerBars(tab);

      if (tab.hasChildren()) {
        bars.row();
        bars.add(Iconc.upload + Core.bundle.get("fragment.bars.product")).left().padBottom(0);

        SnapshotSeq<Element> el = tab.getChildren();
        Element[] items = el.begin();
        for (int i = 0, b = el.size; i < b; i++) {
          bars.row();
          bars.add(items[i]);
        }
        el.end();

        bars.row();
        bars.image().color(Color.darkGray).growX().colspan(2).height(4).padTop(3).padBottom(3).padLeft(-14).padRight(-14);
      }
    }

    @Override
    public float activeSoundVolume() {
      return loopSoundVolume*workEfficiency();
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
    public float consEfficiency() {
      float eff = super.consEfficiency();

      return real*eff*warmup();
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
      real = Mathf.lerpDelta(real, currBoost.get(this), 0.05f);

      if (mark > 0 && --mark <= 0){
        currBoost = e -> 1;
      }

      if(updateRecipe && producer.current != null){
        if(producer.current.get(SglProduceType.item) != null) outputItems = new Seq<>(producer.current.get(SglProduceType.item).items).map(e -> e.item);
        if(producer.current.get(SglProduceType.liquid) != null) outputLiquids = new Seq<>(producer.current.get(SglProduceType.liquid).liquids).map(e -> e.liquid);
      }

      for (Byproduct byproduct : byproducts.values()) {
        dump(byproduct.item);
      }
    }
    
    @Override
    public void buildConfiguration(Table table){
      if(producers().size > 1 && canSelect){
        table.table(Tex.buttonTrans, prescripts -> {
          prescripts.defaults().grow().marginTop(0).marginBottom(0).marginRight(5).marginRight(5);

          prescripts.add(Core.bundle.get("fragment.buttons.selectPrescripts")).padLeft(5).padTop(5).padBottom(5);
          prescripts.row();

          prescripts.pane(buttons -> {
            for (int i = 0; i < producers().size; i++) {
              int s = i;
              BaseProducers p = producers().get(i);
              BaseConsumers c = consumers().get(i);

              if (c.selectable.get() == BaseConsumers.Visibility.hidden) continue;

              buttons.left().button(t -> {
                t.left().defaults().left();
                    buildRecipeSimple(c, p, t);
                  }, Styles.underlineb, () -> configure(s))
                  .touchable(() -> c.selectable.get().buttonValid)
                  .update(b -> b.setChecked(recipeCurrent == s))
                  .fillY().growX().left().margin(5).marginTop(8).marginBottom(8).pad(4)
                  .get().addListener(new Tooltip(t -> t.table(Tex.paneLeft, detail -> FactoryBlockComp.buildRecipe(detail, c, p))));

              buttons.row();
            }
          }).fill().maxHeight(280);
        });

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