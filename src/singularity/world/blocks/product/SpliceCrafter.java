package singularity.world.blocks.product;

import arc.Core;
import arc.func.Func;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
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
import mindustry.world.consumers.ConsumePower;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.type.SglLiquidStack;
import singularity.world.blockComp.*;
import singularity.world.blocks.SglBlock;
import singularity.world.blocks.chains.ChainContainer;
import singularity.world.blocks.chains.ChainsEvents;
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
import universeCore.entityComps.blockComps.ConsumerBuildComp;
import universeCore.entityComps.blockComps.ProducerBuildComp;
import universeCore.util.UncLiquidStack;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.BaseConsumers;
import universeCore.world.consumers.UncConsumeLiquids;
import universeCore.world.producers.BaseProduce;
import universeCore.world.producers.BaseProducers;
import universeCore.world.producers.ProduceLiquids;

import java.util.ArrayList;
import java.util.List;

public class SpliceCrafter extends NormalCrafter implements SpliceBlockComp{
  public boolean interCorner = true;
  public float tempLiquidCapacity;
  
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
  
  public class SpliceCrafterBuild extends NormalCrafterBuild implements SpliceBuildComp{
    public ChainsModule chains;
    public int[] splice;
    public boolean handling, updateModule = true, initGas = true;
  
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
      
      if(hasItems) items = new SpliceItemModule(itemCapacity);
      if(hasLiquids) liquids = new SpliceLiquidModule(liquidCapacity);
      if(hasGases) gases = new SpliceGasesModule(this, true, tempLiquidCapacity);
      consumer = new SpliceConsumeModule(this, consumers, optionalCons);
      cons(consumer);
      producer = new SpliceProduceModule(this, producers);
      
      chains.listen(ChainsEvents.AddedBlockEvent.class, e -> {
        if(e.oldContainer != null && e.container != e.oldContainer){
          if(e.target.getBlock().hasItems) e.container.getVar(SpliceItemModule.class).add(e.oldContainer.getVar(SpliceItemModule.class));
          if(e.target.getBlock().hasLiquids) e.container.getVar(SpliceLiquidModule.class).add(e.oldContainer.getVar(SpliceLiquidModule.class));
          if(e.target.getBlock(SglBlock.class).hasGases) e.container.getVar(SpliceGasesModule.class).add(e.oldContainer.getVar(SpliceGasesModule.class));
          
          SpliceCrafterBuild statDisplay;
          if((statDisplay = e.container.getVar(SpliceCrafterBuild.class)) != e.target){
            if(statDisplay.y >= e.target.getBuilding().y && statDisplay.x <= e.target.getBuilding().x) e.container.putVar(e.target);
          }
          
          e.target.getBuilding(SpliceCrafterBuild.class).updateModule = true;
        }
      });
      
      chains.listen(ChainsEvents.ConstructFlowEvent.class, e -> {
        SpliceCrafterBuild statDisplay;
        if((statDisplay = e.container.getVar(SpliceCrafterBuild.class)) != e.target){
          if(statDisplay.y >= e.target.getBuilding().y && statDisplay.x <= e.target.getBuilding().x) e.container.putVar(e.target);
        }
        e.target.getBuilding(SpliceCrafterBuild.class).updateModule = true;
      });
      
      chains.listen(ChainsEvents.RemovedBlockEvent.class, e -> {
        SpliceItemModule items = e.target.chains().getVar(SpliceItemModule.class);
        SpliceLiquidModule liquids = e.target.chains().getVar(SpliceLiquidModule.class);
        SpliceGasesModule gases = e.target.chains().getVar(SpliceGasesModule.class);
  
        SpliceCrafter targetBlock = e.target.getBlock(SpliceCrafter.class);

        ObjectSet<ChainContainer> handled = new ObjectSet<>();
        int total = 0;

        for(ChainsBuildComp other : e.target.chainBuilds()){
          if(handled.add(other.chains().container)) total += other.chains().container.all.size;
        }

        for(ChainContainer otherContainer : handled){
          float present = (float) otherContainer.all.size/total;
          SpliceItemModule oItems = otherContainer.getVar(SpliceItemModule.class);
          SpliceLiquidModule oLiquids = otherContainer.getVar(SpliceLiquidModule.class);
          SpliceGasesModule oGases = otherContainer.getVar(SpliceGasesModule.class);
          
          if(targetBlock.hasItems){
            oItems.allCapacity = (int) ((items.allCapacity - e.target.getBlock().itemCapacity)*present);
            items.each((item, amount) -> {
              float pre = (float) amount/items.total();
              pre *= (float) items.total()/items.allCapacity;
              oItems.set(item, (int) ((amount - targetBlock.itemCapacity*pre)*present));
            });
          }
          
          if(targetBlock.hasLiquids){
            oLiquids.allCapacity = (liquids.allCapacity - targetBlock.tempLiquidCapacity)*present;
            liquids.each((liquid, amount) -> {
              float pre = amount/liquids.total();
              pre *= liquids.total()/liquids.allCapacity;
              oLiquids.set(liquid, (amount - targetBlock.liquidCapacity*pre)*present);
            });
          }
          
          if(targetBlock.hasGases){
            oGases.allCapacity = (gases.allCapacity - targetBlock.gasCapacity)*present;
            gases.each(stack -> {
              float pre = stack.amount/gases.total();
              pre *= gases.getPressure()/targetBlock.maxGasPressure;
              oGases.set(stack.gas, (stack.amount - targetBlock.maxGasPressure*targetBlock.gasCapacity*pre)*present);
            });
          }
        }
      });
      
      chains.listen(ChainsEvents.InitChainContainerEvent.class, e -> {
        SpliceCrafter b = e.target.getBlock(SpliceCrafter.class);
        SpliceCrafterBuild ent = e.target.getBuilding(SpliceCrafterBuild.class);
        
        e.newContainer.putVar(new SpliceConsumeModule((ConsumerBuildComp) e.target, consumers, optionalCons));
        e.newContainer.putVar(new SpliceProduceModule((ProducerBuildComp) e.target, producers));
        
        if(b.hasItems) e.newContainer.putVar(new SpliceItemModule(b.itemCapacity));
        if(b.hasLiquids) e.newContainer.putVar(new SpliceLiquidModule(b.tempLiquidCapacity));
        if(b.hasGases){
          e.newContainer.putVar(new SpliceGasesModule(e.target.getBuilding(GasBuildComp.class), ent.initGas, b.gasCapacity));
          if(ent.initGas) ent.initGas = false;
        }
        e.newContainer.putVar(e.target);
      });
  
      chains.newContainer();
      return this;
    }
  
    @Override
    public float consumeMultiplier(BaseConsume<?> cons){
      return super.consumeMultiplier(cons)*chains.container.all.size;
    }
  
    @Override
    public float productMultiplier(BaseProduce<?> prod){
      return super.productMultiplier(prod)*chains.container.all.size;
    }
  
    @Override
    public void setBars(Table table){
      if(recipeCurrent != -1 && producer.current != null && block.hasPower && block.outputsPower && producer.current.get(SglProduceType.power) != null){
        Func<Building, Bar> bar = (entity -> new Bar(
            () -> Core.bundle.format("bar.poweroutput",Strings.fixed(entity.getPowerProduction() * 60 * entity.timeScale(), 1)),
            () -> Pal.powerBar,
            () -> powerProdEfficiency
        ));
        table.add(bar.get(this)).growX();
        table.row();
      }
  
      //显示流体存储量
      if(hasLiquids) updateDisplayLiquid();
      if (!displayLiquids.isEmpty()){
        table.table(Tex.buttonTrans, t -> {
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
        table.row();
      }
  
      if(recipeCurrent == -1 || consumer.current == null) return;
  
      if(hasPower && consumes.hasPower()){
        ConsumePower cons = block().consumes.getPower();
        boolean buffered = cons.buffered;
        float capacity = cons.capacity;
        Func<Building, Bar> bar = (entity -> new Bar(() -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * capacity) ? "<ERROR>" : (int)(entity.power.status * capacity)) :
            Core.bundle.get("bar.power"), () -> Pal.powerBar, () -> Mathf.zero(cons.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status));
        table.add(bar.get(this)).growX();
        table.row();
      }
  
      UncConsumeLiquids<?> cl = consumer.current.get(SglConsumeType.liquid);
      SglConsumeGases<?> cg = consumer.current.get(SglConsumeType.gas);
      if(cl != null || cg != null){
        table.table(Tex.buttonEdge1, t -> t.left().add(Core.bundle.get("fragment.bars.consume")).pad(4)).pad(0).height(38).padTop(4);
        table.row();
        table.table(t -> {
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
  
      table.row();
      
      if(recipeCurrent == -1 || producer.current == null) return;
  
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
      if(updateModule){
        if(hasItems){
          SpliceItemModule tItems = chains.getVar(SpliceItemModule.class);
          if(!tItems.loaded){
            tItems.set((SpliceItemModule) items);
            tItems.loaded = true;
          }
          if(items != tItems) items = tItems;
        }
        if(hasLiquids){
          SpliceLiquidModule tLiquids = chains.getVar(SpliceLiquidModule.class);
          if(!tLiquids.loaded){
            tLiquids.set((SpliceLiquidModule) liquids);
            tLiquids.loaded = true;
          }
          if(liquids != tLiquids) liquids = tLiquids;
        }
        if(hasGases){
          SpliceGasesModule tGases = chains.getVar(SpliceGasesModule.class);
          if(!tGases.loaded){
            tGases.set((SpliceGasesModule) gases);
            tGases.loaded = true;
          }
          if(gases != tGases) gases = tGases;
        }
        
        SpliceConsumeModule tCons = chains.getVar(SpliceConsumeModule.class);
        if(!tCons.loaded){
          tCons.set((SpliceConsumeModule) consumer);
          tCons.loaded = true;
        }
        if(consumer != tCons){
          consumer = tCons;
          cons(tCons);
        }
        
        SpliceProduceModule tProd = chains.getVar(SpliceProduceModule.class);
        if(!tProd.loaded){
          tProd.set((SpliceProduceModule) producer);
          tProd.loaded = true;
        }
        if(producer != tProd) producer = tProd;
        
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
      return source.team == this.team && hasItems && !(source instanceof ChainsBuildComp && chains.container.all.contains((ChainsBuildComp) source)) && consumer.filter(SglConsumeType.item, item) && items.get(item) < items().allCapacity && status == SglBlockStatus.proper;
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.team == this.team && hasLiquids && !(source instanceof ChainsBuildComp && chains.container.all.contains((ChainsBuildComp) source)) && consumer.filter(SglConsumeType.liquid, liquid) && liquids.get(liquid) <= liquids().allCapacity - 0.0001f && status == SglBlockStatus.proper;
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
    public void onProximityAdded(){
      super.onProximityAdded();
      onChainsUpdate();
    }
  
    @Override
    public void onProximityRemoved(){
      super.onProximityRemoved();
      onChainsRemoved();
    }
  
    @Override
    public void drawStatus(){
      if(this.block.enableDrawStatus && this.block().consumers.size() > 0 && chains.getVar(SpliceCrafterBuild.class) == this){
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
    public ChainsModule chains(){
      return chains;
    }
    
  }
  
  public static class SpliceItemModule extends ItemModule{
    protected ObjectSet<ItemModule> added = new ObjectSet<>();
    public int allCapacity;
    public boolean loaded;
    public long lastFrameId;
    
    public SpliceItemModule(int capacity){
      allCapacity = capacity;
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
  
    public SpliceLiquidModule(float capacity){
      allCapacity = capacity;
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
    
    public SpliceGasesModule(GasBuildComp entity, boolean init, float capacity){
      super(entity, init);
      allCapacity = capacity;
    }
    
    public void set(SpliceGasesModule otherModule){
      otherModule.each(stack -> set(stack.gas, stack.amount));
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
  
    public SpliceConsumeModule(ConsumerBuildComp entity, ArrayList<BaseConsumers> cons, ArrayList<BaseConsumers> optional){
      super(entity, cons, optional);
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
    
    public SpliceProduceModule(ProducerBuildComp entity, List<BaseProducers> producers){
      super(entity, producers);
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
