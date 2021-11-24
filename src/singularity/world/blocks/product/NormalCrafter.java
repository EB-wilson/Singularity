package singularity.world.blocks.product;

import arc.Core;
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
import mindustry.Vars;
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
import singularity.Singularity;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.type.SglLiquidStack;
import singularity.ui.dialogs.LiquidSelecting;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blocks.SglBlock;
import singularity.world.consumers.SglConsumeGases;
import singularity.world.consumers.SglConsumeType;
import singularity.world.draw.DrawFactory;
import singularity.world.meta.SglBlockStatus;
import singularity.world.meta.SglStat;
import singularity.world.modules.SglProductModule;
import singularity.world.products.ProduceGases;
import singularity.world.products.Producers;
import singularity.world.products.SglProduceType;
import universeCore.entityComps.blockComps.ProducerBlockComp;
import universeCore.entityComps.blockComps.ProducerBuildComp;
import universeCore.util.UncLiquidStack;
import universeCore.world.blockModule.BaseProductModule;
import universeCore.world.consumers.BaseConsumers;
import universeCore.world.consumers.UncConsumeLiquids;
import universeCore.world.producers.BaseProducers;
import universeCore.world.producers.ProduceItems;
import universeCore.world.producers.ProduceLiquids;

import java.util.ArrayList;

/**常规的工厂类方块，具有强大的consume-produce制造系统的近乎全能的制造类方块*/
public class NormalCrafter extends SglBlock implements ProducerBlockComp{
  public final ArrayList<BaseProducers> producers = new ArrayList<>();
  
  public float updateEffectChance = 0.05f;
  public Effect updateEffect = Fx.none;
  public Effect craftEffect = Fx.none;
  
  public boolean autoSelect = false;
  public boolean canSelect = true;
  public boolean shouldConfig;
  
  /**同样的，这也是一个指针，指向当前编辑的produce*/
  public Producers produce;
  
  public final NormalCrafter self = this;
  
  /**在显示多液体输出配置时的液体选择贴图*/
  public TextureRegion liquidSelector;
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
  public ArrayList<BaseProducers> producers(){
    return producers;
  }
  
  @Override
  public Producers newProduce(){
    produce = new Producers();
    this.producers().add(produce);
    return produce;
  }

  @Override
  public void init(){
    if(producers.size() > 0) for(BaseProducers prod: producers){
      hasItems |= prod.get(SglProduceType.item) != null;
      hasLiquids |= outputsLiquid |= prod.get(SglProduceType.liquid) != null;
      hasPower |= outputsPower |= prod.get(SglProduceType.power) != null && prod.get(SglProduceType.power).powerProduction != 0;
      hasGases |= outputGases |= prod.get(SglProduceType.gas) != null;
      hasEnergy |= outputEnergy |= prod.get(SglProduceType.energy) != null;
    }
    
    super.init();
  
    initProduct();
  
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
    setProducerStats(stats);
    if(producers.size() > 1){
      stats.add(SglStat.autoSelect, autoSelect);
      stats.add(SglStat.controllable, canSelect);
    }
  }

  @Override
  public TextureRegion[] icons(){
    return draw.icons();
  }

  public class NormalCrafterBuild extends SglBuilding implements ProducerBuildComp{
    private final Seq<Liquid> tempLiquid = new Seq<>();
    
    public SglProductModule producer;
    public Liquid[] selectLiquid = new Liquid[4];
    public boolean liquidSelecting = false;
    
    public Seq<Item> outputItems;
    public Seq<Liquid> outputLiquids;
    public Seq<Gas> outputGases;

    public float progress;
    public float totalProgress;
    public float warmup;
    
    public int select;
    
    public float powerProdEfficiency;
  
    @Override
    public NormalCrafterBuild create(Block block, Team team) {
      super.create(block, team);
      consumer.acceptAll = autoSelect;
      producer = new SglProductModule(this, producers);
      return this;
    }
    
    public void produce(){
      producer.trigger();
    }
  
    @Override
    public int produceCurrent(){
      return recipeCurrent;
    }
  
    @Override
    public BaseProductModule producer(){
      return producer;
    }
  
    @Override
    public void reset(){
      super.reset();
      progress = 0;
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
  
    @Override
    public void setBars(Table table){
      if(recipeCurrent != -1 && producer.current != null && block.hasPower && block.outputsPower && producer.current.get(SglProduceType.power) != null){
        float productPower = powerProdEfficiency*producer.current.get(SglProduceType.power).powerProduction;
        Func<Building, Bar> bar = (entity -> new Bar(
          () -> Core.bundle.format("bar.poweroutput",Strings.fixed(productPower * 60 * entity.timeScale(), 1)),
          () -> Pal.powerBar,
          () -> powerProdEfficiency
        ));
        table.add(bar.get(this)).growX();
        table.row();
      }
      super.setBars(table);
      if(recipeCurrent == -1 || producer.current == null) return;
  
      UncConsumeLiquids<?> cl = consumer.current.get(SglConsumeType.liquid);
      SglConsumeGases<?> cg = consumer.current.get(SglConsumeType.gas);
  
      ProduceLiquids<?> pl = producer.current.get(SglProduceType.liquid);
      ProduceGases<?> pg = producer.current.get(SglProduceType.gas);
      if(pl != null || pg != null){
        table.table(cl == null && cg == null? Tex.buttonEdge1: Tex.pane, t -> t.left().add(Core.bundle.get("fragment.bars.product")).pad(4)).pad(0).height(38);
        table.row();
        table.table(t -> {
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
    public float getProgressIncrease(float baseTime){
      return 1/baseTime*consDelta(consumer.current);
    }
  
    @Override
    public void updateTile() {
      if(! recipeSelected && autoSelect && consumer.hasConsume()
          && (consumer.current == null || !consumer.valid())){
        recipeCurrent = -1;
        int f = -1;
        for(BaseConsumers ignored : consumers){
          int n = select%consumers.size();
          if(consumer.valid(n)){
            f = n;
            break;
          }
          select = (select + 1)%consumers.size();
        }
        if(recipeCurrent != f && f >= 0){
          recipeCurrent = f;
          consumer.setCurrent();
          producer.setCurrent();
        }
      }
  
      producer.update();
      //Log.info("updating,data: recipeCurrent:" + recipeCurrent + ", cons valid:" + consValid() + ", prod valid:" + producer.valid);
      /*当未选择配方时不进行更新*/
      if(recipeCurrent == -1 || producer.current == null) return;
      super.updateTile();
      
      if(consValid()){
        progress += getProgressIncrease(consumer.current.craftTime);
        warmup = Mathf.lerpDelta(warmup, 1, warmupSpeed);
        if(Mathf.chanceDelta(updateEffectChance)){
          updateEffect.at(getX() + Mathf.range(size * 4f), getY() + Mathf.range(size * 4));
        }
      }
      else{
        warmup = Mathf.lerpDelta(warmup, 0, stopSpeed);
      }
      totalProgress += warmup*edelta();
      
      if(progress >= 1){
        craftEffect.at(getX(), getY());
        progress = 0;
        consume();
        produce();
      }
      ProduceLiquids<?> prod = producer.current.get(SglProduceType.liquid);
      liquidSelecting = prod != null && prod.liquids.length > 1 && prod.liquids.length <= 4;
  
      if(updateRecipe){
        if(producer.current.get(SglProduceType.item) != null) outputItems = new Seq<>(producer.current.get(SglProduceType.item).items).map(e -> e.item);
        if(producer.current.get(SglProduceType.liquid) != null) outputLiquids = new Seq<>(producer.current.get(SglProduceType.liquid).liquids).map(e -> e.liquid);
        if(producer.current.get(SglProduceType.gas) != null) outputGases = new Seq<>(producer.current.get(SglProduceType.gas).gases).map(e -> e.gas);
      }
    }
    
    @Override
    public float getPowerProduction(){
      if(!outputsPower) return 0;
      return producer.current.get(SglProduceType.power).powerProduction*(powerProdEfficiency = (Mathf.num(shouldConsume() && consValid())*productMultiplier(producer.current.get(SglProduceType.power))*efficiency()));
    }
  
    @Override
    public float efficiency(){
      return super.efficiency()*warmup;
    }
  
    @Override
    public void dumpLiquid(Liquid liquid){
      if(!liquidSelecting){
        super.dumpLiquid(liquid);
        return;
      }
      for(int direct=0; direct<4; direct++){
        if(selectLiquid[direct] == liquid){
          for(int number=0; number < block.size; number++){
            Building other = getNearby(direct, number);
            if(other != null){
              other = other.getLiquidDestination(this, liquid);
              if(other.team == team && other.block.hasLiquids && canDumpLiquid(other, liquid) && other.liquids != null){
                float ofract = other.liquids.get(liquid) / other.block.liquidCapacity;
                float fract = liquids.get(liquid) / block.liquidCapacity;
                if(ofract < fract){
                  transferLiquid(other, (fract - ofract) * block.liquidCapacity / 2, liquid);
                }
              }
            }
          }
        }
      }
    }
    
    @Override
    public void buildConfiguration(Table table){
      if(status == SglBlockStatus.broken || !canSelect) return;
      ProduceLiquids<?> prod = recipeCurrent != -1? producer.current.get(SglProduceType.liquid): null;
      if(liquidSelecting && prod != null){
        table.add(new LiquidSelecting(this, prod.liquids)).size(50, 50);
      }
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
          ProduceItems<?> produceItems = p.get(SglProduceType.item);
          ProduceLiquids<?> produceLiquids = p.get(SglProduceType.liquid);
          ProduceGases<?> produceGases = p.get(SglProduceType.gas);
          
          icon = p.icon != null? p.icon: produceItems != null && produceItems.items != null?
          produceItems.items[0].item.uiIcon: produceLiquids != null && produceLiquids.liquids != null?
          produceLiquids.liquids[0].liquid.uiIcon: produceGases != null && produceGases.gases != null?
          produceGases.gases[0].gas.uiIcon: null;
          ImageButton button = new ImageButton(icon, Styles.selecti);
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
      return self;
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
      write.f(progress);
      write.f(totalProgress);
      write.f(warmup);
      for(int i=0; i<4; i++){
        if(selectLiquid[i] == null){
          write.i(-1);
        }
        else{
          write.i(selectLiquid[i].id);
        }
      }
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      recipeSelected = read.bool();
      progress = read.f();
      totalProgress = read.f();
      warmup = read.f();
      for(int i=0; i<4; i++){
        int id = read.i();
        selectLiquid[i] = id != -1? Vars.content.liquid(id): null;
      }
    }
  }
}