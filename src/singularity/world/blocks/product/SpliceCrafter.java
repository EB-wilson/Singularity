package singularity.world.blocks.product;

import arc.Core;
import arc.func.Cons;
import arc.func.Func;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Strings;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.type.SglLiquidStack;
import singularity.world.blocks.SglBlock;
import singularity.world.blocks.chains.ChainsContainer;
import singularity.world.blocks.chains.ChainsEvents;
import singularity.world.components.*;
import singularity.world.consumers.SglConsumeGases;
import singularity.world.consumers.SglConsumeType;
import singularity.world.draw.DrawSpliceBlock;
import singularity.world.meta.SglBlockStatus;
import singularity.world.modules.ChainsModule;
import singularity.world.modules.GasesModule;
import singularity.world.modules.SglConsumeModule;
import singularity.world.modules.SglProductModule;
import singularity.world.products.ProduceGases;
import singularity.world.products.SglProduceType;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.components.blockcomp.ProducerBuildComp;
import universecore.util.UncLiquidStack;
import universecore.world.consumers.UncConsumeLiquids;
import universecore.world.producers.ProduceLiquids;

import java.util.LinkedHashMap;

@Annotations.ImplEntries
public class SpliceCrafter extends NormalCrafter implements SpliceBlockComp{
  public int maxChainsWidth = 10;
  public int maxChainsHeight = 10;

  public boolean interCorner = true;
  public float tempLiquidCapacity;

  protected ObjectMap<ChainsEvents.ChainsTrigger, Seq<Runnable>> chainsListeners = new ObjectMap<>();
  protected ObjectMap<Class<? extends ChainsEvents.ChainsEvent>, LinkedHashMap<String, Cons<ChainsEvents.ChainsEvent>>> globalChainsListeners = new ObjectMap<>();
  
  public SpliceCrafter(String name){
    super(name);
    
    draw = new DrawSpliceBlock<>(this);
  }

  @Override
  public void init(){
    super.init();
    tempLiquidCapacity = liquidCapacity;
  }
  
  @Override
  public void setBars(){
    super.setBars();
    if(hasGases){
      bars.remove("gasPressure");
      bars.add("gasPressure", (SpliceCrafterBuild entity) -> new Bar(
          () -> Core.bundle.get("fragment.bars.gasPressure") + ":" + Strings.autoFixed(entity.smoothPressure*100, 0) + "kPa",
          () -> Pal.accent,
          () -> Math.min(entity.smoothPressure / maxGasPressure, 1)));
    }
  }
  
  @Override
  @SuppressWarnings("all")
  public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
    ((DrawSpliceBlock) draw).drawRequest(req, list, interCorner);
  }
  
  @Override
  public boolean chainable(ChainsBlockComp other){
    return this == other;
  }

  @Annotations.ImplEntries
  public class SpliceCrafterBuild extends NormalCrafterBuild implements SpliceBuildComp{
    public ChainsModule chains;
    public int[] splice;
    public boolean handling, updateModule = true, firstInit = true;
  
    @Override
    public SpliceItemModule items(){
      return (SpliceItemModule)items;
    }
  
    @Override
    public SpliceLiquidModule liquids(){
      return (SpliceLiquidModule)liquids;
    }
  
    @Override
    public SpliceGasesModule gases(){
      return (SpliceGasesModule)gases;
    }
  
    @Override
    public NormalCrafterBuild create(Block block, Team team){
      super.create(block, team);
      chains = new ChainsModule(this);
      
      if(hasItems) items = new SpliceItemModule(itemCapacity, true);
      if(hasLiquids) liquids = new SpliceLiquidModule(liquidCapacity, true);
      if(hasGases) gases = new SpliceGasesModule(this, true, tempLiquidCapacity);
      consumer = new SpliceConsumeModule(this);
      cons(consumer);
      producer = new SpliceProduceModule(this);

      return this;
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      chains.newContainer();
      return this;
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
      if(hasLiquids) updateDisplayLiquid();
      if (!displayLiquids.isEmpty()){
        bars.table(Tex.buttonTrans, t -> {
          t.defaults().growX().height(18f).pad(4);
          t.top().add(otherLiquidStr).padTop(0);
          t.row();
          for (SglLiquidStack stack : displayLiquids) {
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
  
      if(hasPower && consumes.hasPower()){
        ConsumePower cons = block().consumes.getPower();
        boolean buffered = cons.buffered;
        float capacity = cons.capacity;
        Func<Building, Bar> bar = (entity -> new Bar(() -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * capacity) ? "<ERROR>" : (int)(entity.power.status * capacity)) :
            Core.bundle.get("bar.power"), () -> Pal.powerBar, () -> Mathf.zero(cons.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status));
        bars.add(bar.get(this)).growX();
        bars.row();
      }
  
      UncConsumeLiquids<?> cl = consumer.current.get(SglConsumeType.liquid);
      SglConsumeGases<?> cg = consumer.current.get(SglConsumeType.gas);
      if(cl != null || cg != null){
        bars.table(Tex.buttonEdge1, t -> t.left().add(Core.bundle.get("fragment.bars.consume")).pad(4)).pad(0).height(38).padTop(4);
        bars.row();
        bars.table(t -> {
          t.defaults().grow().margin(0);
          if(cl != null){
            t.table(Tex.pane2, liquid -> {
              liquid.defaults().growX().margin(0).pad(4).height(18);
              liquid.left().add(Core.bundle.get("misc.liquid")).color(Pal.gray);
              liquid.row();
              for(UncLiquidStack stack: cl.liquids){
                Func<Building, Bar> bar = (entity -> new Bar(
                    () -> stack.liquid.localizedName,
                    () -> stack.liquid.barColor != null? stack.liquid.barColor: stack.liquid.color,
                    () -> Math.min(entity.liquids.get(stack.liquid) / liquids().allCapacity, 1f)
                ));
                liquid.add(bar.get(this));
                liquid.row();
              }
            });
          }
      
          if(cg != null){
            t.table(Tex.pane2, gases -> {
              gases.defaults().growX().margin(0).pad(4).height(18);
              gases.left().add(Core.bundle.get("misc.gas")).color(Pal.gray);
              gases.row();
              for(GasStack stack: cg.gases){
                Func<Building, Bar> bar = (ent -> {
                  GasBuildComp entity = (GasBuildComp) ent;
                  return new Bar(
                      () -> stack.gas.localizedName,
                      () -> stack.gas.color,
                      () -> Math.min((entity.gases().get(stack.gas) / gasCapacity) * (entity.gases().get(stack.gas) / entity.gases().total() > 0? entity.gases().total(): 1f), 1f)
                  );
                });
                gases.add(bar.get(this));
                gases.row();
              }
            });
          }
        }).height(46 + Math.max((cl != null? cl.liquids.length: 0), (cg != null? cg.gases.length: 0))*26).padBottom(0).padTop(2);
      }
  
      bars.row();
      
      if(recipeCurrent == -1 || producer.current == null) return;
  
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
                    () -> Math.min(entity.liquids.get(stack.liquid) / liquids().allCapacity, 1f)
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
                      () -> Math.min(entity.gases().get(stack.gas) / gases().allCapacity, 1f)
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
    public void updateTile(){
      liquidCapacity = liquids().allCapacity;
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
        if(hasGases){
          SpliceGasesModule tGases = chains.getVar("gases");
          if(!tGases.loaded){
            tGases.set((SpliceGasesModule) gases);
            tGases.loaded = true;
          }
          if(gases != tGases) gases = tGases;
        }
        
        SpliceConsumeModule tCons = chains.getVar("consumer");
        if(!tCons.loaded){
          tCons.set((SpliceConsumeModule) consumer);
          tCons.loaded = true;
        }
        if(consumer != tCons){
          consumer = tCons;
          cons(tCons);
        }
        
        SpliceProduceModule tProd = chains.getVar("producer");
        if(!tProd.loaded){
          tProd.set((SpliceProduceModule) producer);
          tProd.loaded = true;
        }
        if(producer != tProd) producer = tProd;

        splice = getRegionBits(tile, interCorner);

        updateModule = false;
      }
  
      super.updateTile();
      
      if(producer.entity != this) producer.doDump(this);
      
      liquidCapacity = tempLiquidCapacity;
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
      return source.interactable(this.team) && hasItems && !(source instanceof ChainsBuildComp && chains.container.all.contains((ChainsBuildComp) source)) && consumer.filter(SglConsumeType.item, item, acceptAll(SglConsumeType.item)) && items.get(item) < items().allCapacity && status == SglBlockStatus.proper;
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.interactable(this.team) && hasLiquids && !(source instanceof ChainsBuildComp && chains.container.all.contains((ChainsBuildComp) source)) && consumer.filter(SglConsumeType.liquid, liquid, acceptAll(SglConsumeType.liquid)) && liquids.get(liquid) <= liquids().allCapacity - 0.0001f && status == SglBlockStatus.proper;
    }
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return !(source instanceof ChainsBuildComp && chains.container.all.contains((ChainsBuildComp) source)) && super.acceptGas(source, gas);
    }
  
    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();
      splice = getRegionBits(tile, interCorner);
    }
  
    @Override
    public void drawStatus(){
      if(this.block.enableDrawStatus && this.block().consumers.size() > 0 && chains.getVar("build") == this){
        float multiplier = block.size > 1 || chains.container.all.size > 1 ? 1.0F : 0.64F;
        float brcx = this.tile.drawx() + (float)(this.block.size * 8)/2.0F - 8*multiplier/2;
        float brcy = this.tile.drawy() - (float)(this.block.size * 8)/2.0F + 8*multiplier/2;
        Draw.z(71.0F);
        Draw.color(Pal.gray);
        Fill.square(brcx, brcy, 2.5F*multiplier, 45.0F);
        Draw.color(status == SglBlockStatus.proper? status().color: Color.valueOf("#000000"));
        Fill.square(brcx, brcy, 1.5F*multiplier, 45.0F);
        Draw.color();
      }
    }
  
    @Override
    public int[] spliceData(){
      return splice;
    }

    @Override
    public void containerCreated(ChainsContainer old){
      chains.container.putVar("consumer", new SpliceConsumeModule(this));
      chains.container.putVar("producer", new SpliceProduceModule(this));

      if(hasItems) chains.container.putVar("items", new SpliceItemModule(itemCapacity, firstInit));
      if(hasLiquids) chains.container.putVar("liquids", new SpliceLiquidModule(tempLiquidCapacity, firstInit));
      if(hasGases) chains.container.putVar("gases", new SpliceGasesModule(this, firstInit, gasCapacity));

      chains.container.putVar("build", this);
      if(firstInit) firstInit = false;
    }

    @Override
    public void chainsAdded(ChainsContainer old){
      if(old == chains.container) return;
      if(getBlock().hasItems) chains.container.<SpliceItemModule>getVar("items").add(old.<SpliceItemModule>getVar("items"));
      if(getBlock().hasLiquids) chains.container.<SpliceLiquidModule>getVar("liquids").add(old.getVar("liquids"));
      if(getBlock(SglBlock.class).hasGases) chains.container.<SpliceGasesModule>getVar("gases").add(old.<SpliceGasesModule>getVar("gases"));

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
      SpliceGasesModule gases = chains.getVar("gases");

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
        SpliceGasesModule oGases = otherContainer.getVar("gases");

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

        if(targetBlock.hasGases){
          oGases.allCapacity = (gases.allCapacity - targetBlock.gasCapacity)*present;
          oGases.clear();
          float totalPre = gases.getPressure()/targetBlock.maxGasPressure;
          gases.each((gas, amount) -> {
            float pre = amount/gases.total();
            oGases.set(gas, (gases.total() - targetBlock.maxGasPressure*targetBlock.gasCapacity*totalPre)*present*pre);
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
    public void update(boolean showFlow){
      if(lastFrameId == Core.graphics.getFrameId()) return;
      lastFrameId = Core.graphics.getFrameId();
      
      super.update(showFlow);
      added.clear();
    }
  }
  
  public static class SpliceLiquidModule extends LiquidModule{
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
    public void update(boolean showFlow){
      if(lastFrameId == Core.graphics.getFrameId()) return;
      lastFrameId = Core.graphics.getFrameId();
      
      super.update(showFlow);
      added.clear();
    }
  }
  
  public static class SpliceGasesModule extends GasesModule{
    protected ObjectSet<GasesModule> added = new ObjectSet<>();
    public float allCapacity;
    public boolean loaded;
    public long lastFrameId;
    
    public SpliceGasesModule(GasBuildComp entity, boolean firstLoad, float capacity){
      super(entity, firstLoad);
      loaded = !firstLoad;
      allCapacity = capacity;
    }
    
    public void set(SpliceGasesModule otherModule){
      otherModule.each(this::set);
    }
    
    public void add(SpliceGasesModule otherModule){
      if(added.add(otherModule)){
        super.add(otherModule);
        allCapacity += otherModule.allCapacity;
      }
    }
    
    @Override
    public float getPressure(){
      return total/allCapacity;
    }
    
    @Override
    public void update(boolean showFlow, boolean comp){
      if(lastFrameId == Core.graphics.getFrameId()) return;
      lastFrameId = Core.graphics.getFrameId();
      
      super.update(showFlow, comp);
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
