package singularity.world.blocks.product;

import arc.Core;
import arc.func.Cons;
import arc.func.Floatf;
import arc.func.Func;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.world.consumers.SglConsumeType;
import singularity.world.modules.SglConsumeModule;
import singularity.world.modules.SglLiquidModule;
import singularity.world.modules.SglProductModule;
import singularity.world.products.SglProduceType;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.*;
import universecore.world.blocks.chains.ChainsContainer;
import universecore.world.blocks.modules.ChainsModule;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumeLiquidBase;
import universecore.world.producers.BaseProduce;
import universecore.world.producers.BaseProducers;
import universecore.world.producers.ProduceLiquids;

@Annotations.ImplEntries
public class SpliceCrafter extends NormalCrafter implements SpliceBlockComp {
  public int maxChainsWidth = 10;
  public int maxChainsHeight = 10;

  public Cons<SpliceCrafterBuild> structUpdated;

  public boolean interCorner = false;

  public boolean negativeSplice = false;

  public int tempItemCapacity;
  public float tempLiquidCapacity;

  public SpliceCrafter(String name){
    super(name);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void init(){
    super.init();
    tempItemCapacity = itemCapacity;
    tempLiquidCapacity = liquidCapacity;

    for(BaseConsumers consumer: consumers()){
      for(BaseConsume<? extends ConsumerBuildComp> cons: consumer.all()){
        Floatf old = cons.consMultiplier;
        cons.setMultiple(old == null? (SpliceCrafterBuild e) -> e.chains.container.all.size:
            (SpliceCrafterBuild e) -> old.get(e)*e.chains.container.all.size);
      }
    }
    for(BaseConsumers consumer: optionalCons()){
      for(BaseConsume<? extends ConsumerBuildComp> cons: consumer.all()){
        Floatf old = cons.consMultiplier;
        cons.setMultiple(old == null? (SpliceCrafterBuild e) -> e.chains.container.all.size:
            (SpliceCrafterBuild e) -> old.get(e)*e.chains.container.all.size);
      }
    }
    for(BaseProducers producer: producers()){
      for(BaseProduce<?> prod: producer.all()){
        Floatf old = prod.prodMultiplier;
        prod.setMultiple(old == null? (SpliceCrafterBuild e) -> e.chains.container.all.size:
            (SpliceCrafterBuild e) -> old.get(e)*e.chains.container.all.size);
      }
    }
  }

  @Override
  public boolean chainable(ChainsBlockComp other){
    return this == other;
  }

  @Annotations.ImplEntries
  public class SpliceCrafterBuild extends NormalCrafterBuild implements SpliceBuildComp {
    public ChainsModule chains;
    public SpliceCrafterBuild b = this;
    public boolean handling, updateModule = true, firstInit = true;
    public int splice;

    @Override
    public SpliceItemModule items(){
      return (SpliceItemModule)items;
    }
  
    @Override
    public SpliceLiquidModule liquids(){
      return (SpliceLiquidModule)liquids;
    }
  
    @Override
    public NormalCrafterBuild create(Block block, Team team){
      super.create(block, team);
      chains = new ChainsModule(this);
      
      if(hasItems) items = new SpliceItemModule(itemCapacity, true);
      if(hasLiquids) liquids = new SpliceLiquidModule(liquidCapacity, true);
      consumer = new SpliceConsumeModule(this);
      producer = new SpliceProduceModule(this);

      return this;
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      chains.newContainer();
      return this;
    }

    @Annotations.EntryBlocked
    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();
      updateModule = true;
    }

    @Override
    public void displayBars(Table bars){
      if(recipeCurrent != -1 && producer.current != null && block.hasPower && block.outputsPower && producer.current.get(SglProduceType.power) != null){
        Func<Building, Bar> bar = (entity -> new Bar(
            () -> Core.bundle.format("bar.poweroutput",Strings.fixed(entity.getPowerProduction() * 60 * entity.timeScale(), 1)),
            () -> Pal.powerBar,
            () -> powerProdEfficiency
        ));
        bars.add(bar.get(this)).growX();
        bars.row();
      }
  
      //显示流体存储量
      if(hasLiquids && displayLiquid) updateDisplayLiquid();
      if (!displayLiquids.isEmpty()){
        bars.table(Tex.buttonTrans, t -> {
          t.defaults().growX().height(18f).pad(4);
          t.top().add(liquidsStr).padTop(0);
          t.row();
          for (LiquidStack stack : displayLiquids) {
            Func<Building, Bar> bar = entity -> new Bar(
                () -> stack.liquid.localizedName,
                () -> stack.liquid.barColor != null ? stack.liquid.barColor : stack.liquid.color,
                () -> Math.min(entity.liquids.get(stack.liquid) / liquids().allCapacity, 1f)
            );
            t.add(bar.get(this)).growX();
            t.row();
          }
        }).height(26 * displayLiquids.size + 40);
        bars.row();
      }
  
      if(recipeCurrent == -1 || consumer.current == null) return;
  
      if(hasPower && consPower != null){
        boolean buffered = consPower.buffered;
        float capacity = consPower.capacity;
        Func<Building, Bar> bar = (entity -> new Bar(
            () -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * capacity) ? "<ERROR>" : (int)(entity.power.status * capacity)): Core.bundle.get("bar.power"),
            () -> Pal.powerBar,
            () -> Mathf.zero(consPower.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status));
        bars.add(bar.get(this)).growX();
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
            for(LiquidStack stack: cl.consLiquids){
              Func<Building, Bar> bar = (entity -> new Bar(
                  () -> stack.liquid.localizedName,
                  () -> stack.liquid.barColor != null? stack.liquid.barColor: stack.liquid.color,
                  () -> Math.min(entity.liquids.get(stack.liquid) / liquids().allCapacity, 1f)
              ));
              liquid.add(bar.get(this));
              liquid.row();
            }
          });
        }).height(46 + cl.consLiquids.length*26).padBottom(0).padTop(2);
      }
  
      bars.row();
      
      if(recipeCurrent == -1 || producer.current == null) return;
  
      ProduceLiquids<?> pl = producer.current.get(SglProduceType.liquid);
      if(pl != null){
        bars.table(Tex.buttonEdge1, t -> t.left().add(Core.bundle.get("fragment.bars.product")).pad(4)).pad(0).height(38);
        bars.row();
        bars.table(t -> {
          t.defaults().grow().margin(0);
          t.table(Tex.pane2, liquid -> {
            liquid.defaults().growX().margin(0).pad(4).height(18);
            liquid.add(Core.bundle.get("misc.liquid")).color(Pal.gray);
            liquid.row();
            for(LiquidStack stack: pl.liquids){
              Func<Building, Bar> bar = (entity -> new Bar(
                  () -> stack.liquid.localizedName,
                  () -> stack.liquid.barColor != null? stack.liquid.barColor: stack.liquid.color,
                  () -> Math.min(entity.liquids.get(stack.liquid) / liquids().allCapacity, 1f)
              ));
              liquid.add(bar.get(this));
              liquid.row();
            }
          });
        }).height(46 + pl.liquids.length*26).padTop(2);
      }
    }

    @Override
    public void update(){
      if(hasItems) itemCapacity = items().allCapacity;
      if(hasLiquids) liquidCapacity = liquids().allCapacity;
      super.update();
      itemCapacity = tempItemCapacity;
      liquidCapacity = tempLiquidCapacity;
    }

    @Override
    public void updateTile(){
      chains.container.update();
      if(updateModule){
        if(hasItems){
          SpliceItemModule tItems = chains.getVar("items");
          if(!tItems.loaded){
            tItems.set((SpliceItemModule) items);
            tItems.loaded = true;
          }
          if(items != tItems) items = tItems;
        }
        if(hasLiquids){
          SpliceLiquidModule tLiquids = chains.getVar("liquids");
          if(!tLiquids.loaded){
            tLiquids.set((SpliceLiquidModule) liquids);
            tLiquids.loaded = true;
          }
          if(liquids != tLiquids) liquids = tLiquids;
        }
        
        SpliceConsumeModule tCons = chains.getVar("consumer");
        if(!tCons.loaded){
          tCons.set((SpliceConsumeModule) consumer);
          tCons.loaded = true;
        }
        if(consumer != tCons){
          consumer = tCons;
        }
        b = chains.getVar("curr");
        
        SpliceProduceModule tProd = chains.getVar("producer");
        if(!tProd.loaded){
          tProd.set((SpliceProduceModule) producer);
          tProd.loaded = true;
        }
        if(producer != tProd) producer = tProd;

        splice = getSplice();

        updateModule = false;
      }

      super.updateTile();
      
      if(producer.entity != this) producer.doDump(this);

      handling = false;
    }
  
    @Override
    public Building getLiquidDestination(Building from, Liquid liquid){
      liquidCapacity = liquids().allCapacity;
      handling = true;
      return super.getLiquidDestination(from, liquid);
    }
  
    @Override
    public boolean acceptItem(Building source, Item item){
      return source.interactable(this.team) && hasItems && !(source instanceof ChainsBuildComp && chains.container.all.contains((ChainsBuildComp) source))
          && filter().filter(b, SglConsumeType.item, item, acceptAll(SglConsumeType.item)) && items.get(item) < items().allCapacity;
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.interactable(this.team) && hasLiquids && !(source instanceof ChainsBuildComp && chains.container.all.contains((ChainsBuildComp) source))
          && filter().filter(b, SglConsumeType.liquid, liquid, acceptAll(SglConsumeType.liquid)) && liquids.get(liquid) <= liquids().allCapacity - 0.0001f;
    }

    @Override
    public void draw(){
      if(hasItems) itemCapacity = items().allCapacity;
      if(hasLiquids) liquidCapacity = liquids().allCapacity;
      super.draw();
      itemCapacity = tempItemCapacity;
      liquidCapacity = tempLiquidCapacity;
    }

    @Override
    public void drawStatus(){
      if(this.block.enableDrawStatus && this.block().consumers().size > 0 && chains.getVar("build") == this){
        float multiplier = block.size > 1 || chains.container.all.size > 1 ? 1.0F : 0.64F;
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
    public void containerCreated(ChainsContainer old){
      chains.container.putVar("consumer", new SpliceConsumeModule(this));
      chains.container.putVar("curr", this);
      chains.container.putVar("producer", new SpliceProduceModule(this));

      if(hasItems) chains.container.putVar("items", new SpliceItemModule(itemCapacity, firstInit));
      if(hasLiquids) chains.container.putVar("liquids", new SpliceLiquidModule(tempLiquidCapacity, firstInit));

      chains.container.putVar("build", this);
      if(firstInit) firstInit = false;
    }

    @Override
    public void chainsAdded(ChainsContainer old){
      if(old == chains.container) return;
      if(getBlock().hasItems) chains.container.<SpliceItemModule>getVar("items").add(old.<SpliceItemModule>getVar("items"));
      if(getBlock().hasLiquids) chains.container.<SpliceLiquidModule>getVar("liquids").add(old.getVar("liquids"));

      SpliceCrafterBuild statDisplay;
      if((statDisplay = chains.container.getVar("build")) != this){
        if(statDisplay.y >= getBuilding().y && statDisplay.x <= getBuilding().x) chains.container.putVar("build", this);
      }

      updateModule = true;
    }

    @Override
    public void chainsRemoved(Seq<ChainsBuildComp> children){
      SpliceItemModule items = chains.getVar("items");
      SpliceLiquidModule liquids = chains.getVar("liquids");

      SpliceCrafter targetBlock = getBlock(SpliceCrafter.class);

      ObjectSet<ChainsContainer> handled = new ObjectSet<>();
      int total = 0;

      for(ChainsBuildComp child : children){
        if(handled.add(child.chains().container)) total += child.chains().container.all.size;
      }

      for(ChainsContainer otherContainer : handled){
        float present = (float) otherContainer.all.size/total;
        SpliceItemModule oItems = otherContainer.getVar("items");
        SpliceLiquidModule oLiquids = otherContainer.getVar("liquids");

        if(targetBlock.hasItems){
          oItems.allCapacity = (int) ((items.allCapacity - getBlock().itemCapacity)*present);
          oItems.clear();
          float totalPre = (float) items.total()/items.allCapacity;
          items.each((item, amount) -> {
            float pre = (float) amount/items.total();
            oItems.set(item, (int) ((items.total() - targetBlock.itemCapacity*totalPre)*present*pre));
          });
        }

        if(targetBlock.hasLiquids){
          oLiquids.allCapacity = (liquids.allCapacity - targetBlock.tempLiquidCapacity)*present;
          oLiquids.clear();
          float totalPre = liquids.total()/liquids.allCapacity;
          liquids.each((liquid, amount) -> {
            float pre = amount/liquids.total();
            oLiquids.set(liquid, ((liquids.total() - targetBlock.liquidCapacity*totalPre)*present*pre));
          });
        }
      }
    }

    @Override
    public void chainsFlowed(ChainsContainer old){
      SpliceCrafterBuild statDisplay;
      if((statDisplay = chains.container.getVar("build")) != this){
        if(statDisplay.y >= y && statDisplay.x <= getBuilding().x) chains.container.putVar("build", this);
      }
      updateModule = true;
    }

    @Override
    public void onChainsUpdated(){
      if(structUpdated != null) structUpdated.get(this);
    }

    @Override
    public void splice(int arr){
      splice = arr;
    }

    @Override
    public int splice(){
      return splice;
    }
  }
  
  public static class SpliceItemModule extends ItemModule{
    protected ObjectSet<ItemModule> added = new ObjectSet<>();
    public int allCapacity;
    public boolean loaded;
    public long lastFrameId;
    
    public SpliceItemModule(int capacity, boolean firstLoad){
      allCapacity = capacity;
      loaded = !firstLoad;
    }
  
    public void set(SpliceItemModule otherModule){
      super.set(otherModule);
    }
    
    public void add(SpliceItemModule otherModule){
      if(added.add(otherModule)){
        super.add(otherModule);
        allCapacity += otherModule.allCapacity;
      }
    }

    @Override
    public void updateFlow(){
      if(lastFrameId == Core.graphics.getFrameId()) return;
      lastFrameId = Core.graphics.getFrameId();

      super.updateFlow();
      added.clear();
    }
  }
  
  public static class SpliceLiquidModule extends SglLiquidModule{
    protected ObjectSet<LiquidModule> added = new ObjectSet<>();
    public float allCapacity;
    public boolean loaded;
    public long lastFrameId;
  
    public SpliceLiquidModule(float capacity, boolean firstLoad){
      allCapacity = capacity;
      loaded = !firstLoad;
    }
    
    public void set(SpliceLiquidModule otherModule){
      otherModule.each(this::set);
    }
    
    public void add(SpliceLiquidModule otherModule){
      if(added.add(otherModule)){
        otherModule.each(this::add);
        allCapacity += otherModule.allCapacity;
      }
    }
    
    public void set(Liquid liquid, float amount){
      float delta = get(liquid) - amount;
      add(liquid, -delta);
    }
    
    @Override
    public void updateFlow(){
      if(lastFrameId == Core.graphics.getFrameId()) return;
      lastFrameId = Core.graphics.getFrameId();
      
      super.updateFlow();
      added.clear();
    }
  }
  
  public static class SpliceConsumeModule extends SglConsumeModule{
    public boolean loaded;
    public long lastFrameId;
  
    public SpliceConsumeModule(ConsumerBuildComp entity){
      super(entity);
    }

    public void set(SpliceConsumeModule module){}
  
    @Override
    public void update(){
      if(lastFrameId == Core.graphics.getFrameId()) return;
      lastFrameId = Core.graphics.getFrameId();
      
      super.update();
    }
  }
  
  public static class SpliceProduceModule extends SglProductModule{
    public boolean loaded;
    public long lastFrameId;
    
    public SpliceProduceModule(ProducerBuildComp entity){
      super(entity);
    }

    public void set(SpliceProduceModule module){}
  
    @Override
    public void update(){
      if(lastFrameId == Core.graphics.getFrameId()) return;
      lastFrameId = Core.graphics.getFrameId();
    
      super.update();
    }
  }
}
