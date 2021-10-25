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
import arc.struct.IntSeq;
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
import singularity.world.power.BaseGenerator;
import singularity.world.power.GeneratorType;
import singularity.world.products.ProduceGases;
import singularity.world.products.Producers;
import singularity.world.products.SglProduceType;
import universeCore.entityComps.blockComps.ProducerBlockComp;
import universeCore.entityComps.blockComps.ProducerBuildComp;
import universeCore.util.UncLiquidStack;
import universeCore.world.consumers.UncConsumeLiquids;
import universeCore.world.producers.BaseProduce;
import universeCore.world.producers.BaseProducers;
import universeCore.world.producers.ProduceItems;
import universeCore.world.producers.ProduceLiquids;

import java.util.ArrayList;

/**常规的工厂类方块，具有强大的consume-produce制造系统的近乎全能的制造类方块*/
public class NormalCrafter extends SglBlock implements ProducerBlockComp{
  public final ArrayList<BaseProducers> producers = new ArrayList<>();
  
  /**方块的能量生产策略*/
  public GeneratorType generatorType = GeneratorType.normalGenerator;
  public float updateEffectChance = 0.05f;
  public Effect updateEffect = Fx.none;
  public Effect craftEffect = Fx.none;
  
  public boolean autoSelect;
  
  /**同样的，这也是一个指针，指向当前编辑的produce*/
  public Producers produce;
  
  public final NormalCrafter self = this;
  
  /**在显示多液体输出配置时的液体选择贴图*/
  public TextureRegion liquidSelector;
  
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
  public void appliedConfig() {
    config(IntSeq.class, (NormalCrafterBuild tile, IntSeq data) -> {
      for(int i= 0; i<4; i++){
        if(data.get(i) != -1) {
          tile.selectLiquid[i] = Vars.content.liquid(data.get(i));
        }
      }
      tile.recipeCurrent = ((Number)data.get(4)).intValue();
    });
    configClear((NormalCrafterBuild tile) -> {
      tile.selectLiquid = new Liquid[4];
      tile.recipeCurrent = -1;
    });
  }
  
  @Override
  public Producers newProduce(){
    produce = new Producers();
    this.producers().add(produce);
    return produce;
  }

  @Override
  public void init(){
    if(producers.size() > 1) configurable = true;
    if(producers.size() > 0) for(BaseProducers prod: producers){
      hasItems |= prod.get(SglProduceType.item) != null;
      hasLiquids |= outputsLiquid |= prod.get(SglProduceType.liquid) != null;
      hasPower |= outputsPower |= prod.get(SglProduceType.power) != null && prod.get(SglProduceType.power).powerProduction != 0;
      hasGases |= outputGases |= prod.get(SglProduceType.gas) != null;
      configurable |=  prod.get(SglProduceType.liquid) != null && prod.get(SglProduceType.liquid).liquids.length > 1;
    }
    int b = producers.size();
    int a = consumers.size();
    /*控制produce添加/移除配方以使配方同步*/
    while(a > b){
      Producers p = new Producers();
      p.item(Items.copper, 1);
      producers.add(p);
      b++;
    }
    while(a < b){
      b--;
      producers.remove(b);
    }
    super.init();
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
      for(int i=0; i<consumers.size(); i++){
        for(BaseProduce<?> p: producers.get(i).all()){
          p.display(recipe.stats[i]);
        }
      }
      stats.add(SglStat.inputs, table -> {
        table.row();
        table.add(recipe);
      });
    }
    else if(producers.size() == 1){
      for(BaseProduce<?> prod: producers.get(0).all()){
        prod.display(stats);
      }
    }
    if(outputsPower) stats.add(SglStat.generatorType, generatorType.localized());
  }

  @Override
  public TextureRegion[] icons(){
    return draw.icons();
  }

  public class NormalCrafterBuild extends SglBuilding implements ProducerBuildComp{
    public SglProductModule producer;
    public Liquid[] selectLiquid = new Liquid[4];
    public boolean liquidSelecting = false;
    
    public BaseGenerator generator;

    public float progress;
    public float totalProgress;
    public float warmup;
    public float productionEfficiency = 0f;
  
    @Override
    public NormalCrafterBuild create(Block block, Team team) {
      super.create(block, team);
      producer = new SglProductModule(this, producers);
      if(outputsPower)generator = generatorType.applied(this);
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
    public Object config(){
      int[] selectLiquidOfId = new int[4];
      for(int i = 0; i<4; i++){
        selectLiquidOfId[i] = selectLiquid[i] != null? selectLiquid[i].id: -1;
      }
      IntSeq out = new IntSeq(selectLiquidOfId);
      out.add(((Number)super.config()).intValue());
      return out;
    }
    
    @Override
    public void reset(){
      super.reset();
      progress = 0;
      productionEfficiency = 0;
    }
  
    @Override
    public void handleGas(GasBuildComp source, Gas gas, float amount){
      super.handleGas(source, gas, amount);
      //Log.info("handling, "+ gas + ", " + source + ", " + amount);
    }
  
    @Override
    public void updateDisplayLiquid() {
      if(!block.hasLiquids) return;
      displayLiquids.clear();
      Seq<Liquid> temp = new Seq<>();
      temp.clear();
      if(recipeCurrent >= 0 && consumer.current != null){
        if(consumer.current.get(SglConsumeType.liquid) != null) for(UncLiquidStack stack : consumer.current.get(SglConsumeType.liquid).liquids) {
          temp.add(stack.liquid);
        }
      }
      if(recipeCurrent >= 0 && producer.current != null) {
        if(producer.current.get(SglProduceType.liquid) != null) for(UncLiquidStack stack : producer.current.get(SglProduceType.liquid).liquids) {
          temp.add(stack.liquid);
        }
      }
      liquids.each((key, val) -> {
        if(!temp.contains(key) && val > 0.1f) displayLiquids.add(new SglLiquidStack(key, val));
      });
    }
  
    @Override
    public void setBars(Table table){
      if(recipeCurrent != -1 && producer.current != null && block.hasPower && block.outputsPower && producer.current.get(SglProduceType.power) != null){
        Func<Building, Bar> bar = (entity -> new Bar(
          () -> Core.bundle.format("bar.poweroutput",Strings.fixed(entity.getPowerProduction() * 60 * entity.timeScale(), 1)),
          () -> Pal.powerBar,
          () -> productionEfficiency
        ));
        table.add(bar.get(this)).growX();
        table.row();
      }
      super.setBars(table);
      if(recipeCurrent == -1 || producer.current == null) return;
  
      UncConsumeLiquids cl = consumer.current.get(SglConsumeType.liquid);
      SglConsumeGases cg = consumer.current.get(SglConsumeType.gas);
  
      ProduceLiquids pl = producer.current.get(SglProduceType.liquid);
      ProduceGases pg = producer.current.get(SglProduceType.gas);
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
      return new Seq<>(producer.current.get(SglProduceType.item).items).map(e -> e.item);
    }
  
    @Override
    public Seq<Liquid> outputLiquids(){
      if(recipeCurrent == -1) return null;
      return new Seq<>(producer.current.get(SglProduceType.liquid).liquids).map(e -> e.liquid);
    }
  
    @Override
    public boolean shouldConsume(){
      return producer.valid() && super.shouldConsume();
    }

    @Override
    public void updateTile() {
      if(autoSelect && consumer.hasConsume()){
        for(int i=0; i<consumers.size(); i++){
          if(consumer.valid(i)){
            recipeCurrent = i;
            break;
          }
        }
      }
      
      //Log.info("updating,data: recipeCurrent:" + recipeCurrent + ", cons valid:" + consValid() + ", prod valid:" + producer.valid);
      /*当未选择配方时不进行更新*/
      if(recipeCurrent == -1) return;
      producer.update();
      if(block.outputsPower) generator.update();
      super.updateTile();
      
      if(consValid()){
        progress += getProgressIncrease(consumer.current.craftTime);
        totalProgress += warmup*delta();
        warmup = Mathf.lerpDelta(warmup, 1, 0.02f);
        if(Mathf.chanceDelta(updateEffectChance)){
          updateEffect.at(getX() + Mathf.range(size * 4f), getY() + Mathf.range(size * 4));
        }
      }
      else{
        warmup = Mathf.lerpDelta(warmup, 0, 0.02f);
      }
      
      if(progress >= 1){
        craftEffect.at(getX(), getY());
        progress = 0;
        consume();
        produce();
        if(outputsPower) generator.trigger();
      }
      ProduceLiquids prod = producer.current.get(SglProduceType.liquid);
      liquidSelecting = prod != null && prod.liquids.length > 1 && prod.liquids.length <= 4;
    }
    
    @Override
    public float getPowerProduction(){
      if(!outputsPower) return 0;
      return producer.current.get(SglProduceType.power).powerProduction * productionEfficiency;
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
      if(status == SglBlockStatus.broken) return;
      ProduceLiquids prod = recipeCurrent != -1? producer.current.get(SglProduceType.liquid): null;
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
          ProduceItems produceItems = p.get(SglProduceType.item);
          ProduceLiquids produceLiquids = p.get(SglProduceType.liquid);
          
          icon = p.icon != null? p.icon: produceItems.items != null?
          produceItems.items[0].item.uiIcon: produceLiquids.liquids != null?
          produceLiquids.liquids[0].liquid.uiIcon: null;
          ImageButton button = new ImageButton(icon, Styles.selecti);
          button.clicked(() -> {
            reset();
            if(recipeCurrent == s){
              recipeCurrent = -1;
              return;
            }
            recipeCurrent = s;
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
      if(generator != null)generator.draw();
      drawSelectRecipe(this);
      drawStatus();
      Draw.blend();
    }
    
    public void drawSelectRecipe(NormalCrafterBuild entity){
      if(entity.block().displaySelectPrescripts && entity.recipeCurrent != -1){
        BaseProducers current = producer.current;
        ProduceItems produceItems = current.get(SglProduceType.item);
        ProduceLiquids produceLiquids = current.get(SglProduceType.liquid);
        Color color = current.color != null? current.color: produceItems.items != null? produceItems.items[0].item.color: produceLiquids.liquids != null? produceLiquids.liquids[0].liquid.color: Items.copper.color;
        Draw.color(color);
        Draw.rect(entity.block().prescriptSelector, entity.x, entity.y);
        Draw.color();
      }
    }
    
    @Override
    public void write(Writes write) {
      super.write(write);
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