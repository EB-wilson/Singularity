package singularity.world.blocks;

import arc.Core;
import arc.func.Boolp;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Floatp;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.core.Renderer;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.ui.fragments.PlacementFragment;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.meta.*;
import singularity.type.SglLiquidStack;
import singularity.world.blocks.nuclear.NuclearPipeNode;
import singularity.world.components.NuclearEnergyBlockComp;
import singularity.world.components.NuclearEnergyBuildComp;
import singularity.world.consumers.SglConsumeType;
import singularity.world.consumers.SglConsumers;
import singularity.world.modules.NuclearEnergyModule;
import singularity.world.modules.SglConsumeModule;
import singularity.world.modules.SglLiquidModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ConsumerBlockComp;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.util.DataPackable;
import universecore.util.UncLiquidStack;
import universecore.util.handler.FieldHandler;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumeLiquidBase;
import universecore.world.consumers.ConsumeType;

import static mindustry.Vars.*;

/**此mod的基础方块类型，对block添加了完善的consume系统，并拥有中子能的基础模块*/
@Annotations.ImplEntries
public class SglBlock extends Block implements ConsumerBlockComp, NuclearEnergyBlockComp{
  public boolean autoSelect = false;
  public boolean canSelect = true;
  
  public boolean outputItems;

  public final Seq<BaseConsumers> consumers = new Seq<>();

  public DrawBlock draw = new DrawDefault();

  /**这是一个指针，用于标记当前编辑的consume*/
  public SglConsumers consume;
  /**独立的物品栏，为true所有物品具有独立的容量，否则共用物品空间*/
  public boolean independenceInventory = true;
  /**独立的液体储罐，为true所有液体具有独立的容量，否则共用液体空间*/
  public boolean independenceLiquidTank = true;
  /**控制在多种可选输入都可用时是全部可用还是其中优先级最高的一种使用*/
  public boolean oneOfOptionCons = true;
  
  /**方块是否为核能设备*/
  public boolean hasEnergy;

  public Cons<SglBuilding> initialed;
  public Cons<SglBuilding> updating;

  /**核能阻值，在运送核能时运输速度会减去这个数值*/
  public float resident = 0.1f;
  /**方块是否输出核能量*/
  public boolean outputEnergy = false;
  /**方块是否需求核能量*/
  public boolean consumeEnergy = false;
  /**是否为核能缓冲器，为真时该方块的核势能将固定为基准势能*/
  public boolean energyBuffered = false;
  /**基准核势能，当能压小于此值时不接受核能传入*/
  public float basicPotentialEnergy = 0f;
  /**核能容量。此变量将决定方块的最大核势能*/
  public float energyCapacity = 256;
  /**此方块接受的最大势能差，可设为-1将根据容量自动设置*/
  public float maxEnergyPressure = -1;
  
  public String liquidsStr = Core.bundle.get("fragment.bars.liquids");
  public String recipeIndfo = Core.bundle.get("fragment.buttons.selectPrescripts");

  public SglBlock(String name) {
    super(name);
    consumesPower = false;
    appliedConfig();
    config(byte[].class, (SglBuilding e, byte[] code) -> {
      parseConfigObjects(e, DataPackable.readObject(code, e));
    });
  }
  
  public void appliedConfig(){
    config(Integer.class, (SglBuilding e, Integer i) -> {
      if(consumers().size > 1){
        e.recipeSelected = true;
        e.reset();
        if(e.recipeCurrent == i || i == - 1){
          e.recipeCurrent = -1;
          e.recipeSelected = false;
        }
        else e.recipeCurrent = i;
      }
    });
    configClear((SglBuilding e) -> {
      e.recipeSelected = false;
      e.recipeCurrent = 0;
    });
  }
  
  public void parseConfigObjects(SglBuilding e, Object obj){}
  
  @Override
  public BaseConsumers newConsume(){
    consume = new SglConsumers(false);
    this.consumers().add(consume);
    return consume;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public <T extends ConsumerBuildComp> BaseConsumers newOptionalConsume(Cons2<T, BaseConsumers> validDef, Cons2<Stats, BaseConsumers> displayDef){
    consume = new SglConsumers(true) {{
        this.optionalDef = (Cons2<ConsumerBuildComp, BaseConsumers>) validDef;
        this.display = displayDef;
    }};
    this.optionalCons().add(consume);
    return consume;
  }
  
  @Override
  public void init() {
    if(consumers().size > 1){
      configurable = true;
      saveConfig = true;
    }

    Seq<BaseConsumers> consume = new Seq<>();
    if(consumers().size > 0) consume.addAll(consumers());
    if(optionalCons().size > 0) consume.addAll(optionalCons());
    for(BaseConsumers cons: consume){
      if(cons.get(SglConsumeType.item) != null){
        hasItems = true;
        if(cons.craftTime == 0) cons.time(90f);
      }
      hasLiquids |= cons.get(SglConsumeType.liquid) != null;
      hasPower |= consumesPower |= cons.get(SglConsumeType.power) != null;
      hasEnergy |= consumeEnergy |= cons.get(SglConsumeType.energy) != null;
    }
    
    super.init();
  }
  
  @Override
  public void drawPotentialLinks(int x, int y){
    super.drawPotentialLinks(x, y);
    
    if((consumeEnergy || outputEnergy) && hasEnergy){
      Tile tile = world.tile(x, y);
      if(tile != null){
        NuclearPipeNode.getNodeLinks(tile, this, player.team()).each(e -> {
          if(!(e.getBlock() instanceof NuclearPipeNode node)) return;

          Draw.color(node.linkColor, Renderer.laserOpacity * 0.5f);
          node.drawLink(tile.worldx() + offset, tile.worldy() + offset, size, e.getBuilding().tile.drawx(), e.getBuilding().tile.drawy(), e.getBlock().size);
          Drawf.square(e.getBuilding().x, e.getBuilding().y, e.getBlock().size * tilesize / 2f + 2f, Pal.place);
        });
      }
    }
  }
  
  @Override
  public void load(){
    super.load();
    draw.load(this);
  }
  
  @Override
  public void setStats() {
    stats.add(Stat.size, "@x@", size, size);
    stats.add(Stat.health, health, StatUnit.none);
    if(canBeBuilt()){
      stats.add(Stat.buildTime, buildCost / 60, StatUnit.seconds);
      stats.add(Stat.buildCost, StatValues.items(false, requirements));
    }

    if(instantTransfer){
      stats.add(Stat.maxConsecutive, 2, StatUnit.none);
    }
  
    if (this.hasLiquids) this.stats.add(Stat.liquidCapacity, this.liquidCapacity, StatUnit.liquidUnits);
  
    if (this.hasItems && this.itemCapacity > 0) this.stats.add(Stat.itemCapacity, (float)this.itemCapacity, StatUnit.items);
  }

  @Override
  public boolean outputsItems(){
    return hasItems && outputItems;
  }

  @Override
  public void setBars(){
    addBar("health", entity -> new Bar("stat.health", Pal.health, entity::healthf).blink(Color.white));
  }

  @Override
  public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
    draw.drawPlan(this, plan, list);
  }

  @Override
  public void getRegionsToOutline(Seq<TextureRegion> out){
    draw.getRegionsToOutline(this, out);
  }

  @Override
  public TextureRegion[] icons(){
    return draw.finalIcons(this);
  }

  @Annotations.ImplEntries
  public class SglBuilding extends Building implements ConsumerBuildComp, NuclearEnergyBuildComp{
    private static final FieldHandler<PlacementFragment> fieldHandler = new FieldHandler<>(PlacementFragment.class);

    public SglConsumeModule consumer;
    public NuclearEnergyModule energy;

    public boolean recipeSelected;

    protected final ObjectMap<Object, Object> vars = new ObjectMap<>();

    protected final Seq<SglLiquidStack> displayLiquids = new Seq<>();

    @Annotations.FieldKey("consumeCurrent") public int recipeCurrent = -1;
    public int lastRecipe;
    public boolean updateRecipe;
    
    public int select;

    @SuppressWarnings("unchecked")
    public <T> T getVar(String name){
      return (T)vars.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVar(Class<T> type){
      return (T)vars.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVar(String name, T def){
      return (T)vars.get(name, () -> def);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVar(Class<T> type, T def){
      return (T)vars.get(type, () -> def);
    }

    public Object setVar(Object obj){
      return vars.put(obj.getClass(), obj);
    }

    public Object setVar(String field, Object obj){
      return vars.put(field, obj);
    }

    @Override
    public Building create(Block block, Team team) {
      super.create(block, team);

      liquids = new SglLiquidModule();
      
      if(consumers().size == 1) recipeCurrent = 0;
      
      consumer = new SglConsumeModule(this);
      
      if(hasEnergy){
        energy = new NuclearEnergyModule(this, energyBuffered);
        energy.setNet();
      }
      return this;
    }

    @Override
    public SglLiquidModule liquids(){
      return (SglLiquidModule) liquids;
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      if(initialed != null){
        initialed.get(this);
      }
      return this;
    }

    @Override
    public void onControlSelect(Unit player){
      super.onControlSelect(player);
      FieldHandler.setValueDefault(Vars.ui.hudfrag.blockfrag, "lastDisplayState", null);
    }

    @Override
    public void placed(){
      super.placed();
      if(net.client()) return;
  
      if((consumeEnergy || outputEnergy) && hasEnergy){
        NuclearPipeNode.getNodeLinks(tile, block(), player.team()).each(e -> {
          if(!(e.getBlock() instanceof NuclearPipeNode)) return;
          
          if(!e.energy().linked.contains(pos())) e.getBuilding().configureAny(pos());
        });
      }
    }

    @Override
    public boolean shouldConsume(){
      return super.shouldConsume() && ConsumerBuildComp.super.shouldConsume();
    }

    @Override
    public float efficiency(){
      return consEfficiency();
    }
    
    public void dumpLiquid(){
      liquids.each((l, n) -> dumpLiquid(l));
    }
  
    @Override
    public void displayConsumption(Table table){
      if(consumer != null) table.update(() -> {
        table.clear();
        table.left();
        consumer.build(table);
      });
    }

    @Override
    public void drawStatus(){
      if(this.block.enableDrawStatus && this.block().consumers().size > 0) {
        float multiplier = block.size > 1 ? 1.0F : 0.64F;
        float brcx = this.tile.drawx() + (float)(this.block.size * 8)/2.0F - 8*multiplier/2;
        float brcy = this.tile.drawy() - (float)(this.block.size * 8)/2.0F + 8*multiplier/2;
        Draw.z(71.0F);
        Draw.color(Pal.gray);
        Fill.square(brcx, brcy, 2.5F*multiplier, 45.0F);
        Draw.color(status().color);
        Fill.square(brcx, brcy, 1.5F*multiplier, 45.0F);
        Draw.color();
      }
    }

    @Override
    public boolean consumeValid() {
      return enabled && ConsumerBuildComp.super.consumeValid();
    }

    @Override
    public BlockStatus status(){
      return consumer.status();
    }

    @Override
    public void consume(){
      consumer.trigger();
    }
  
    public boolean updateValid(){
      return true;
    }

    @Override
    public float consEfficiency() {
      return consumer.hasConsume()? consumer.consEfficiency: 1;
    }

    @Override
    public void update(){
      updateRecipe = false;
      if(!recipeSelected && autoSelect && consumer.hasConsume() && (consumer.current == null || !consumer.valid())){
        int f = -1;
        for(BaseConsumers ignored : consumers()){
          int n = select%consumers().size;
          if(consumer.valid(n) && consumers().get(n).selectable.get() == BaseConsumers.Visibility.usable){
            f = n;
            break;
          }
          select = (select + 1)%consumers().size;
        }
    
        if(recipeCurrent != f && f >= 0){
          recipeCurrent = f;
          onUpdateCurrent();
        }
      }
      
      if(lastRecipe != recipeCurrent) updateRecipe = true;
      lastRecipe = recipeCurrent;

      if(updateRecipe) fieldHandler.setValue(ui.hudfrag.blockfrag, "wasHovered", false);

      super.update();

      if(updating != null) updating.get(this);
    }

    public void onUpdateCurrent(){
      consumer.setCurrent();
    }
  
    public void updateDisplayLiquid(){
      displayLiquids.clear();
      Seq<Liquid> temp = new Seq<>();
      temp.clear();
      if(recipeCurrent >= 0 && consumer.current != null){
        if(consumer.current.get(SglConsumeType.liquid) != null){
          for(UncLiquidStack stack: consumer.current.get(SglConsumeType.liquid).consLiquids){
            temp.add(stack.liquid);
          }
        }
      }
      liquids.each((key, val) -> {
        if(!temp.contains(key) && val > 0.1f) displayLiquids.add(new SglLiquidStack(key, val));
      });
    }
    
    public void reset(){
      if(items != null) items.clear();
      if(liquids != null) liquids.clear();
    }
    
    public Object config(){
      return recipeCurrent;
    }
    
    @Override
    public void display(Table table){
      super.display(table);
      displayEnergy(table);
    }
    
    @Override
    public void displayBars(Table bars){
      super.displayBars(bars);

      //显示流体存储量
      if(hasLiquids) updateDisplayLiquid();
      if (!displayLiquids.isEmpty()){
        bars.table(Tex.buttonTrans, t -> {
          t.defaults().growX().height(18f).pad(4);
          t.top().add(liquidsStr).padTop(0);
          t.row();
          for (SglLiquidStack stack : displayLiquids) {
            t.add(new Bar(
                () -> stack.liquid.localizedName,
                () -> stack.liquid.barColor != null ? stack.liquid.barColor : stack.liquid.color,
                () -> Math.min(liquids.get(stack.liquid) / block().liquidCapacity, 1f)
            )).growX();
            t.row();
          }
        }).height(26 * displayLiquids.size + 40);
        bars.row();
      }

      if(recipeCurrent == -1 || consumer.current == null) return;

      if(hasPower && consPower != null){
        Boolp buffered = () -> consPower.buffered;
        Floatp capacity = () -> consPower.capacity;
        bars.add(new Bar(
            () -> buffered.get() ? Core.bundle.format("bar.poweramount", Float.isNaN(power.status*capacity.get())?
                "<ERROR>": (int)(power.status*capacity.get())): Core.bundle.get("bar.power"),
            () -> Pal.powerBar,
            () -> Mathf.zero(consPower.requestedPower(this)) && power.graph.getPowerProduced() + power.graph.getBatteryStored() > 0f? 1f: power.status)).growX();
        bars.row();
      }

      ConsumeLiquidBase<?> cl = consumer.current.get(SglConsumeType.liquid);
      if(cl != null){
        bars.table(Tex.buttonEdge1, t -> t.left().add(Core.bundle.get("fragment.bars.consume")).pad(4)).pad(0).height(38).padTop(4);
        bars.row();
        bars.table(t -> {
          t.defaults().grow().margin(0);
          t.table(Tex.pane2, liquid -> {
            liquid.defaults().growX().margin(0).pad(4).height(18);
            liquid.left().add(Core.bundle.get("misc.liquid")).color(Pal.gray);
            liquid.row();
            for(UncLiquidStack stack: cl.consLiquids){
              liquid.add(new Bar(
                  () -> stack.liquid.localizedName,
                  () -> stack.liquid.barColor != null? stack.liquid.barColor: stack.liquid.color,
                  () -> Math.min(liquids.get(stack.liquid) / block().liquidCapacity, 1f)
              ));
              liquid.row();
            }
          });
        }).height(46 + cl.consLiquids.length*26).padBottom(0).padTop(2);
      }

      bars.row();
    }
  
    @Override
    public float getMoveResident(NuclearEnergyBuildComp destination){
      float result = 0;
      for(NuclearEnergyBuildComp next: energy.energyNet.getPath(this, destination)){
        result += next.getResident();
      }
      return result;
    }
  
    @Override
    public void onOverpressure(float potentialEnergy){
      //TODO:把默认爆炸写了
    }
    
    /**具有输出物品的方块返回可能输出的物品，没有则返回null*/
    public Seq<Item> outputItems(){
      return null;
    }
  
    /**具有输出液体的方块返回可能输出的液体，没有则返回null*/
    public Seq<Liquid> outputLiquids(){
      return null;
    }
    
    public boolean acceptAll(ConsumeType<?> type){
      return autoSelect && (!canSelect || !recipeSelected);
    }
    
    @Override
    public boolean acceptItem(Building source, Item item){
      return source.interactable(this.team) && hasItems
          && (!(consumer.hasConsume() || consumer.hasOptional()) || consumer.filter(SglConsumeType.item, item, acceptAll(SglConsumeType.item)))
          && (independenceInventory? items.get(item): items.total()) < block().itemCapacity;
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.interactable(this.team) && hasLiquids
          && (!(consumer.hasConsume() || consumer.hasOptional()) || consumer.filter(SglConsumeType.liquid, liquid, acceptAll(SglConsumeType.liquid)))
          && (independenceLiquidTank? liquids.get(liquid): ((SglLiquidModule)liquids).total()) <= block().liquidCapacity - 0.0001f;
    }
  
    @Override
    public boolean acceptEnergy(NuclearEnergyBuildComp source){
      return source.getBuilding().interactable(team) && energy.getEnergy() < getNuclearBlock().energyCapacity()
          && source.getEnergyPressure(this) > basicPotentialEnergy;
    }

    @Override
    public void draw(){
      draw.draw(this);
      drawStatus();
    }
  
    @Override
    public void drawLight(){
      super.drawLight();
      draw.drawLight(this);
    }
    
    @Override
    public SglBlock block(){
      return SglBlock.this;
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.i(recipeCurrent);
      if(consumer != null) consumer.write(write);
      if(energy != null) energy.write(write);
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      recipeCurrent = read.i();
      if(consumer != null) consumer.read(read);
      if(energy != null) energy.read(read);
    }
  }
}
