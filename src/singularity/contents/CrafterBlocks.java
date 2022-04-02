package singularity.contents;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.util.Interval;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.ContentList;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Effect;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.BlockStatus;
import singularity.graphic.SglDraw;
import singularity.type.Gas;
import singularity.world.SglFx;
import singularity.world.blocks.function.Destructor;
import singularity.world.blocks.product.*;
import singularity.world.consumers.SglConsumeGases;
import singularity.world.consumers.SglConsumeType;
import singularity.world.consumers.SglConsumers;
import singularity.world.draw.*;
import singularity.world.lightnings.LightningContainer;
import singularity.world.lightnings.generator.CircleGenerator;
import singularity.world.lightnings.generator.PointToPointGenerator;
import singularity.world.meta.SglStat;
import singularity.world.products.SglProduceType;
import universecore.util.UncLiquidStack;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.UncConsumeItems;
import universecore.world.consumers.UncConsumeLiquids;
import universecore.world.particles.Particle;
import universecore.world.producers.ProduceLiquids;
import universecore.world.producers.ProduceType;

import static mindustry.Vars.state;
import static singularity.Singularity.getModAtlas;

public class CrafterBlocks implements ContentList{
  /**裂变编织器*/
  public static Block fission_weaver,
      /**绿藻池*/
      culturing_barn,
      /**育菌箱*/
      incubator,
      /**电解机*/
      electrolytor,
      /**干馏塔*/
      retort_column,
      /**炼油塔*/
      petroleum_separator,
      /**激光解离机*/
      laser_resolver,
      /**反应釜*/
      reaction_kettle,
      /**洗矿机*/
      ore_washer,
      /**结晶器*/
      crystallizer,
      /**FEX相位混合器*/
      FEX_phase_mixer,
      /**燃料封装机*/
      fuel_packager,
      /**强化合金冶炼厂*/
      strengthening_alloy_smelter,
      /**混合凝胶工厂*/
      gel_mixer,
      /**纯化分离机*/
      purifier,
      /**热能离心机*/
      thermal_centrifuge,
      /**晶格构建器*/
      lattice_constructor,
      /**FEX充能座*/
      FEX_crystal_charger,
      /**矩阵切割机*/
      matrix_cutter,
      /**聚合引力发生器*/
      polymer_gravitational_generator,
      /**质量生成器*/
      quality_generator,
      /**析构器*/
      destructor,
      /**物质逆化器*/
      substance_inverter,
      /**强子重构仪*/
      hadron_reconstructor;
  
  public void load(){
    fission_weaver = new NormalCrafter("fission_weaver"){{
      requirements(Category.crafting, ItemStack.with(SglItems.crystal_FEX, 50, Items.phaseFabric, 60, SglItems.strengthening_alloy, 50, Items.plastanium, 45, Items.silicon, 70));
      size = 4;
      oneOfOptionCons = true;
      itemCapacity = 12;
      
      newConsume();
      consume.time(90);
      consume.power(2.5f);
      consume.items(ItemStack.with(Items.silicon, 4, SglItems.uranium_238, 1));
      consume.valid = e -> e.consData(Integer.class, 0) > 0;
      newProduce();
      produce.item(Items.phaseFabric, 4);
      
      craftEffect = Fx.smeltsmoke;
  
      Cons<Item> recipe = item -> {
        newOptionalConsume((e, c) -> {
          e.consData(2);
        }, (s, c) -> {
          s.add(SglStat.effect, t -> t.add(Core.bundle.get("misc.doConsValid")));
        }).acceptOverdrive = false;
        consume.item(item, 1);
        consume.time(180);
        consume.power(0.4f);
      };
      recipe.get(SglItems.uranium_235);
      recipe.get(SglItems.plutonium_239);
      
      buildType = () -> new NormalCrafterBuild(){
        @Override
        public void updateTile(){
          super.updateTile();
          if(consData(Integer.class, 0) > 0) consData(consData(Integer.class) - 1);
        }
  
        @Override
        public BlockStatus status(){
          BlockStatus status = super.status();
          if(status == BlockStatus.noInput && consData(Integer.class, 0) > 0) return BlockStatus.noOutput;
          return status;
        }
      };
      
      draw = new DrawFactory<>(this){{
        drawerType = e -> new DrawFactoryDrawer(e){
          float rotation;
          float warmup;
          
          @Override
          public void draw(){
            warmup = Mathf.lerp(warmup, e.consData(Integer.class, 0) > 0? 1: 0, 0.15f);
            Draw.rect(top, entity.x, entity.y, rotation += warmup*e.edelta());
            Lines.stroke(2);
            Draw.color(Pal.accent);
            Draw.alpha(e.warmup);
            Lines.lineAngleCenter(
                entity.x + Mathf.sin(entity.totalProgress, 6, (float) Vars.tilesize/3*block.size),
                entity.y,
                90,
                (float) block.size*Vars.tilesize/2
            );
            Draw.color();
            Draw.rect(region, entity.x, entity.y);
          }
        };
      }};
    }};
  
    culturing_barn = new SpliceCrafter("culturing_barn"){
      {
        requirements(Category.production, ItemStack.with());
        interCorner = false;
        
        newConsume();
        consume.liquid(Liquids.water, 0.02f);
        newProduce();
        produce.gas(Gases.O2, 0.005f);
        produce.liquid(SglLiquids.algae_mud, 0.001f);
        
        ambientSound = Sounds.none;
        
        draw = new DrawSpliceBlock<SpliceCrafterBuild>(this){
          TextureRegion liquid;
          
          @Override
          public void load(){
            super.load();
            liquid = Core.atlas.find(block.name + "_liquid");
          }
          
          {
            drawDef = e -> {
              Drawf.liquid(liquid, e.x, e.y, e.liquids.get(Liquids.water)/e.liquids().allCapacity, Liquids.water.color);
            };
          }
        };
        
        buildType = () -> new SpliceCrafterBuild(){
          float efficiency;
          
          @Override
          public float efficiency(){
            return super.efficiency()*efficiency;
          }
    
          @Override
          public void updateTile(){
            super.updateTile();
            
            efficiency = enabled ?
                Mathf.maxZero(Attribute.light.env() +
                    (state.rules.lighting ?
                        1f - state.rules.ambientLight.a :
                        1f
                    )) : 0f;
          }
        };
      }
  
      @Override
      public void setBars(){
        super.setBars();
        bars.add("efficiency", (SglBuilding entity) ->
          new Bar(() ->
            Core.bundle.format("bar.efficiency", (int)(entity.efficiency() * 100)),
            () -> Pal.lightOrange,
            entity::efficiency));
      }
    };
    
    incubator = new SglAttributeCrafter("incubator"){{
      requirements(Category.production, ItemStack.with(Items.plastanium, 85, Items.titanium, 90, SglItems.aerogel, 80, Items.copper, 90));
      size = 3;
      liquidCapacity = 20f;
      
      newConsume();
      consume.time(100);
      consume.power(2.2f);
      consume.liquid(Liquids.water, 0.4f);
      consume.gas(Gases.spore_cloud, 0.06f);
      newProduce();
      produce.item(Items.sporePod, 2);
      
      setAttrBooster(Attribute.spores, 0.86f);
      setAttrBooster(Attribute.heat, 1.8f, 3f);
      
      draw = new SglDrawCultivator<>(this);
    }};
  
    electrolytor = new NormalCrafter("electrolytor"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 60, Items.copper, 40, Items.silicon, 45, Items.metaglass, 60, Items.plastanium, 35));
      size = 2;
      
      newConsume();
      consume.liquid(Liquids.water, 0.2f);
      consume.power(2.5f);
      newProduce().color = Liquids.water.color;
      produce.gases(Gases.O2, 0.1f, Gases.H2, 0.2f);
      
      newConsume();
      consume.item(Items.sporePod, 1);
      consume.liquid(Liquids.water, 0.1f);
      consume.power(2);
      consume.time(60);
      newProduce().color = Items.sporePod.color;
      produce.gas(Gases.spore_cloud, 0.18f);
      
      newConsume();
      consume.liquid(SglLiquids.algae_mud, 0.2f);
      consume.time(120);
      consume.power(2.4f);
      newProduce().color = SglLiquids.algae_mud.color;
      produce.item(SglItems.chlorella_block, 1);
      
      draw = new SglDrawCultivator<>(this){
        final Color trans = new Color(0, 0, 0, 0), temp = new Color();
        
        @Override
        public Color liquidColor(NormalCrafterBuild entity){
          return entity.recipeCurrent == -1? trans: temp.set(Liquids.water.color).lerp(entity.producer.current.color,
              entity.recipeCurrent <= 1? (float)entity.items.get(Items.sporePod)/itemCapacity: entity.liquids.get(SglLiquids.algae_mud)/liquidCapacity);
        }
  
        @Override
        public Color liquidColorLight(NormalCrafterBuild entity){
          temp.set(liquidColor(entity));
          return temp.lerp(Color.white, 0.24f);
        }
  
        @Override
        public float liquidAlpha(NormalCrafterBuild entity){
          return entity.liquids.smoothAmount()/liquidCapacity;
        }
      };
    }};
    
    retort_column = new NormalCrafter("retort_column"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 70, Items.graphite, 75, Items.copper, 90, Items.metaglass, 90, Items.plastanium, 50));
      size = 3;
      itemCapacity = 12;
      liquidCapacity = 16;
      
      newConsume();
      consume.time(90f);
      consume.power(2f);
      consume.item(Items.coal, 3);
      newProduce();
      produce.items(ItemStack.with(
          Items.pyratite, 1,
          SglItems.coke, 1
      ));
      produce.liquid(SglLiquids.mixed_tar, 0.1f);
      produce.gas(Gases.CH4, 0.2f);
      
      craftEffect = Fx.smeltsmoke;
      
      draw = new SglDrawSmelter<>(this);
    }};
    
    petroleum_separator = new NormalCrafter("petroleum_separator"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 80, Items.silicon, 80, Items.lead, 90, Items.plastanium, 70, Items.metaglass, 60));
      size = 3;
      liquidCapacity = 24;
      
      newConsume();
      consume.power(1.5f);
      consume.liquid(Liquids.oil, 0.4f);
      newProduce();
      produce.liquids(UncLiquidStack.with(
          SglLiquids.mixed_tar, 0.2,
          SglLiquids.fuel_oil, 0.2
      ));
  
      craftEffect = Fx.formsmoke;
      updateEffect = Fx.plasticburn;
    }};
    
    laser_resolver = new NormalCrafter("laser_resolver"){{
      requirements(Category.crafting, ItemStack.with(SglItems.crystal_FEX, 45, SglItems.strengthening_alloy, 70, Items.silicon, 90, Items.phaseFabric, 65, Items.metaglass, 120));
      size = 3;
      itemCapacity = 20;
      warmupSpeed = 0.01f;
      
      newConsume();
      consume.time(30f);
      consume.power(3.2f);
      consume.item(SglItems.nuclear_waste, 1);
      newProduce().color = SglItems.nuclear_waste.color;
      produce.items(ItemStack.with(
          SglItems.salt_iridium, 2,
          Items.lead, 7,
          Items.thorium, 3)
      ).random();
      
      newConsume();
      consume.time(30f);
      consume.item(Items.scrap, 2);
      consume.liquid(Liquids.slag, 0.1f);
      consume.power(3.5f);
      newProduce().color = Items.scrap.color;
      produce.items(ItemStack.with(
          Items.thorium, 3,
          Items.titanium, 4,
          Items.lead, 5,
          Items.copper, 3
      )).random();
      
      newConsume();
      consume.time(45f);
      consume.item(SglItems.crush_ore, 2);
      consume.power(2.8f);
      newProduce().color = SglItems.crush_ore.color;
      produce.items(ItemStack.with(
          Items.titanium, 2,
          Items.thorium, 1,
          Items.lead, 3,
          Items.copper, 5
      )).random();
      
      draw = new DrawFactory<>(this){
        public TextureRegion laser;
        
        final Color trans = new Color(0, 0, 0, 0);
        
        @Override
        public void load(){
          super.load();
          laser = getModAtlas("laser_resolver_laser");
        }
        
        {
          drawerType = e -> new DrawFactoryDrawer(e){
            float rotation;
            
            @Override
            public void draw(){
              UncConsumeItems<?> ci = e.consumer.current != null? e.consumer.current.get(SglConsumeType.item): null;
              Draw.rect(bottom, entity.x(), entity.y());
              Drawf.liquid(liquid, entity.x(), entity.y(),
                  ci != null? (float) entity.items.get(ci.items[0].item)/entity.block.itemCapacity: 0,
                  e.producer.current != null? e.producer.current.color: trans);
              Draw.color();
              Draw.rect(region, entity.x(), entity.y());
              Draw.alpha(entity.warmup);
              Draw.rect(laser, entity.x(), entity.y(), rotation += entity.warmup*entity.edelta()*1.5f);
              Draw.color();
              Draw.rect(rotator, entity.x(), entity.y(), rotation);
              Draw.rect(top, entity.x(), entity.y());
              Draw.color();
            }
          };
        }
      };
    }};
    
    reaction_kettle = new ReactionKettle("reaction_kettle"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 70, Items.lead, 60, Items.plastanium, 70, Items.copper, 100, Items.graphite, 40));
      size = 3;
      health = 600;
    
      itemCapacity = 30;
      liquidCapacity = 20;
      gasCapacity = 10;
    
      maxGasPressure = 25;
    
      draw = new SglDrawBlock<ReactionKettleBuild>(this){
        TextureRegion top;
      
        @Override
        public void load(){
          super.load();
          top = Core.atlas.find(block.name + "_top");
        }
      
        {
          drawDef = ent -> {
            Draw.rect(region, ent.x(), ent.y());
            Draw.color(Pal.accent);
            Draw.alpha(ent.pressure()/ent.getGasBlock().maxGasPressure());
            Draw.rect(top, ent.x(), ent.y());
            Draw.alpha(ent.absTemperature()/((ReactionKettle)ent.block).maxTemperature);
            Draw.rect(top, ent.x(), ent.y(), 90);
          };
        }
      };
    }};
    
    ore_washer = new NormalCrafter("ore_washer"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 60, Items.graphite, 40, Items.lead, 45, Items.metaglass, 60));
      size = 2;
      itemCapacity = 20;
      liquidCapacity = 24f;
      
      newConsume();
      consume.time(120f);
      consume.liquids(UncLiquidStack.with(Liquids.water, 0.4f, SglLiquids.rock_bitumen, 0.2f));
      consume.power(1.8f);
      newProduce();
      produce.liquid(SglLiquids.FEX_liquid, 0.1f);
      produce.items(ItemStack.with(Items.sand, 4, SglItems.crush_ore, 2, SglItems.crush_uranium_ore, 1)).random();
      
      craftEffect = Fx.pulverizeMedium;
      
      draw = new DrawFactory<>(this){
        TextureRegion point;
  
        @Override
        public void load(){
          super.load();
          point = getModAtlas("ore_washer_point");
        }
  
        {
          drawDef = entity -> {
            Draw.rect(region, entity.x(), entity.y());
            Draw.color(Liquids.water.color);
            Draw.alpha(entity.liquids.get(Liquids.water)/entity.block.liquidCapacity);
            Draw.rect(liquid, entity.x(), entity.y());
            Draw.color();
            Drawf.spinSprite(rotator, entity.x(), entity.y(), entity.totalProgress*4.5f);
            Draw.rect(top, entity.x(), entity.y());
            BaseConsume<?> cons = ((SglConsumers) (entity.consumer.current)).first();
            Color topColor;
            float alpha = 0;
            if(cons instanceof SglConsumeGases){
              Gas gas = ((SglConsumeGases<?>) cons).gases[0].gas;
              topColor = gas.color;
              alpha = entity.gases.getPressure()/entity.getGasBlock().maxGasPressure();
            }else if(cons instanceof UncConsumeLiquids){
              Liquid liquid = ((UncConsumeLiquids<?>) cons).liquids[0].liquid;
              if(liquid == Liquids.water) liquid = ((UncConsumeLiquids<?>) cons).liquids[1].liquid;
              topColor = liquid.color;
              alpha = entity.liquids.get(liquid)/entity.block.liquidCapacity;
            }else if(cons instanceof UncConsumeItems){
              Item item = ((UncConsumeItems<?>) cons).items[0].item;
              topColor = item.color;
              alpha = (float) entity.items.get(item)/entity.block.itemCapacity;
            }else topColor = null;
  
            Draw.color(topColor != null ? topColor : new Color(0, 0, 0, 0));
            Draw.alpha(alpha);
            Draw.rect(point, entity.x(), entity.y());
          };
        }
      };
    }};
  
    crystallizer = new NormalCrafter("crystallizer"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 35, Items.silicon, 45, Items.copper, 40, Items.metaglass, 50));
      size = 2;
      liquidCapacity = 16;
      
      newConsume();
      consume.time(240f);
      consume.item(SglItems.strengthening_alloy, 1);
      consume.liquid(SglLiquids.FEX_liquid, 0.4f);
      consume.power(2.8f);
      newProduce();
      produce.item(SglItems.crystal_FEX, 1);

      draw = new SglDrawCultivator<>(this){
        {
          plantColor = Color.valueOf("#C73A3A");
          plantColorLight = Color.valueOf("#E57D7D");
        }
  
        @Override
        public float liquidAlpha(NormalCrafterBuild entity){
          return entity.liquids.smoothAmount()/entity.block.liquidCapacity;
        }
      };
    }};
  
    FEX_phase_mixer = new NormalCrafter("FEX_phase_mixer"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 90, Items.phaseFabric, 85, Items.silicon, 80));
      size = 2;
      liquidCapacity = 12;
      
      newConsume();
      consume.time(90);
      consume.item(Items.phaseFabric, 1);
      consume.liquid(SglLiquids.FEX_liquid, 0.2f);
      consume.power(1.9f);
      newProduce();
      produce.liquid(SglLiquids.phase_FEX_liquid, 0.2f);
      
      draw = new DrawFactory<>(this){{
        drawDef = e -> {
          Draw.rect(bottom, e.x, e.y);
          Draw.color(SglLiquids.FEX_liquid.color, SglLiquids.phase_FEX_liquid.color, e.liquids.get(SglLiquids.phase_FEX_liquid)/liquidCapacity);
          Draw.alpha(Math.max(e.liquids.get(SglLiquids.FEX_liquid), e.liquids.get(SglLiquids.phase_FEX_liquid)));
          Draw.rect(liquid, e.x, e.y);
          Draw.color();
          Draw.rect(region, e.x, e.y);
          Draw.rect(top, e.x, e.y);
        };
      }};
    }};
  
    fuel_packager = new NormalCrafter("fuel_packager"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 45, Items.phaseFabric, 40, Items.silicon, 45, Items.graphite, 30));
      size = 2;
      autoSelect = true;
      
      newConsume();
      consume.time(120);
      consume.items(ItemStack.with(SglItems.uranium_235, 2, SglItems.strengthening_alloy, 1));
      consume.power(1.5f);
      newProduce();
      produce.item(SglItems.concentration_uranium_235, 1);
      newConsume();
      consume.time(120);
      consume.items(ItemStack.with(SglItems.plutonium_239, 2, SglItems.strengthening_alloy, 1));
      consume.power(1.5f);
      newProduce();
      produce.item(SglItems.concentration_plutonium_239, 1);
      
      craftEffect = Fx.smeltsmoke;
      
      draw = new SglDrawBlock<NormalCrafterBuild>(this){
        TextureRegion frame, top;
        
        @Override
        public void load(){
          super.load();
          frame = Core.atlas.find(name + "_frame");
          top = Core.atlas.find(name + "_top");
        }
        
        {
          drawDef = entity -> {
            Draw.rect(region, entity.x, entity.y);
            if(entity.items.get(SglItems.strengthening_alloy) > 0 || entity.progress > 0.4f)
              Draw.rect(frame, entity.x, entity.y);
            if(entity.consumer.current != null) Draw.color(entity.consumer.current.get(SglConsumeType.item).items[0].item.color);
            Draw.alpha(entity.progress);
            Draw.rect(top, entity.x, entity.y);
          };
        }
      };
    }};
  
    strengthening_alloy_smelter = new NormalCrafter("strengthening_alloy_smelter"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 60, Items.thorium, 70, SglItems.aerogel, 60, Items.lead, 80, Items.silicon, 60));
      size = 3;
      itemCapacity = 20;
      
      newConsume();
      consume.time(60);
      consume.power(3.5f);
      consume.items(ItemStack.with(SglItems.coke, 1, Items.titanium, 2, Items.thorium, 1));
      consume.liquid(SglLiquids.mixed_chemical_gel, 0.2f);
      newProduce();
      produce.item(SglItems.strengthening_alloy, 1);
      
      craftEffect = Fx.smeltsmoke;
      
      draw = new SglDrawSmelter<>(this){
        {
          {
            flameRadius = 3.2f;
            flameRadiusIn = 2.4f;
          }

          drawerType = e -> new SglDrawSmelterDrawer(e){
            @Override
            public void draw(){
              Draw.rect(bottom, e.x, e.y);
              Draw.color(SglLiquids.mixed_chemical_gel.color);
              Draw.alpha(e.liquids.get(SglLiquids.mixed_chemical_gel)/e.block.liquidCapacity);
              Draw.rect(liquid, e.x, e.y);
              Draw.color();
              super.draw();
            }
          };
        }
      };
    }};
    
    gel_mixer = new NormalCrafter("gel_mixer"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 90, Items.lead, 100, Items.thorium, 75, Items.graphite, 60, Items.metaglass, 120));
      size = 3;
      liquidCapacity = 40f;
      
      newConsume();
      consume.time(120f);
      consume.power(1.8f);
      consume.items(ItemStack.with(Items.pyratite, 1));
      consume.gas(Gases.O2, 0.4f);
      consume.liquids(UncLiquidStack.with(SglLiquids.mixed_tar, 0.2f, Liquids.water, 0.4f));
      newProduce();
      produce.liquid(SglLiquids.mixed_chemical_gel, 0.4f);
      
      updateEffect = Fx.plasticburn;
      updateEffectChance = 0.035f;
      craftEffect = SglFx.steamBreakOut;
      
      draw = new DrawFactory<>(this){
        public TextureRegion liquidCenter, liquidSide, piston;
        
        @Override
        public void load(){
          super.load();
          liquidCenter = Core.atlas.find(block.name + "_center");
          liquidSide = Core.atlas.find(block.name + "_side");
          piston = Core.atlas.find(block.name + "_piston");
        }
        
        @Override
        public TextureRegion[] icons(){
          return new TextureRegion[]{
              bottom,
              region
          };
        }
        
        {
          drawDef = entity -> {
            if(entity.recipeCurrent == -1 || entity.consumer.current == null || entity.producer.current == null) return;
            UncConsumeItems<?> ci = entity.consumer.current.get(SglConsumeType.item);
            UncConsumeLiquids<?> cl = entity.consumer.current.get(SglConsumeType.liquid);
            SglConsumeGases<?> cg = entity.consumer.current.get(SglConsumeType.gas);
            ProduceLiquids<?> pl = entity.producer.current.get(SglProduceType.liquid);
            Draw.rect(bottom, entity.x(), entity.y());
            Draw.color(Liquids.water.color);
            Draw.alpha(entity.liquids.get(Liquids.water)/entity.block.liquidCapacity);
            Draw.rect(liquid, entity.x, entity.y);
            Drawf.spinSprite(rotator, entity.x(), entity.y(), 90f + entity.totalProgress*2);
            
            float pistonMove = -Mathf.absin(entity.totalProgress + Mathf.halfPi, 6f, 2.5f);
            int sclx, scly;
            for(int dir=0; dir<4; dir++){
              UnlockableContent o = dir < ci.items.length? ci.items[dir].item:
                  dir-ci.items.length < cl.liquids.length? cl.liquids[dir-ci.items.length].liquid:
                  dir-(ci.items.length + cl.liquids.length) < cg.gases.length? cg.gases[dir-(ci.items.length + cl.liquids.length)].gas: null;
              if(o == null) continue;
              Draw.color(o instanceof Item ? ((Item)o).color: o instanceof Gas? ((Gas)o).color: ((Liquid)o).color);
              Draw.alpha(o instanceof Item ? (float)entity.items.get(o.id)/(float)entity.block().itemCapacity: o instanceof Liquid? entity.liquids.get((Liquid)o)/entity.block().liquidCapacity: entity.pressure() / entity.getGasBlock().maxGasPressure());
              Draw.rect(liquidSide, entity.x(), entity.y(), dir*90f);
              Draw.color();
              
              sclx = Geometry.d8(dir*2+1).x;
              scly = Geometry.d8(dir*2+1).y;
              Draw.rect(piston, entity.x + pistonMove*sclx, entity.y + pistonMove*scly, dir*90);
            }
            Draw.color(pl.liquids[0].liquid.color);
            Draw.alpha(entity.warmup*0.75f);
            Draw.rect(liquidCenter, entity.x(), entity.y());
            Draw.color();
            Draw.rect(region, entity.x(), entity.y());
          };
        }
      };
    }};
  
    purifier = new NormalCrafter("purifier"){{
      size = 3;
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 70, SglItems.crystal_FEX_power, 75, Items.surgeAlloy, 80, Items.phaseFabric, 65, Items.graphite, 50));
      
      newConsume();
      consume.time(120f);
      consume.item(SglItems.uranium_cake, 1);
      consume.power(2.2f);
      newProduce();
      produce.item(SglItems.salt_uranium, 3);
      
      newConsume();
      consume.time(120f);
      consume.item(SglItems.chlorella_block, 3);
      consume.power(2.4f);
      newProduce();
      produce.item(SglItems.chlorella, 1);
      
      craftEffect = Fx.formsmoke;
      updateEffect = Fx.plasticburn;
    
      draw = new DrawFrame<NormalCrafterBuild>(this){
        @Override
        public int framesControl(int index, NormalCrafterBuild e){
          if(index == 1){
            return (int)(13*(((Time.time % 30)/30)%(13f/90))/(13f/90));
          }
          else return 0;
        }
      
        @Override
        public float alphaControl(int index, NormalCrafterBuild e){
          if(index == 1){
            return e.warmup;
          }
          else return 1;
        }
      
        @Override
        public TextureRegion[] icons(){
          return new TextureRegion[]{frames[0][0]};
        }
      
        @Override
        public void load() {
          super.load();
        
          TextureRegion[] laser = new TextureRegion[13];
          for(int i=0; i<13; i++){
            laser[i] = getModAtlas("purifier_" + i);
          }
          frames = new TextureRegion[][]{
              new TextureRegion[]{getModAtlas("purifier")},
              laser
          };
        }
      };
    }};
    
    thermal_centrifuge = new NormalCrafter("thermal_centrifuge"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 100, SglItems.aerogel, 80, Items.copper, 120, Items.silicon, 70, Items.plastanium, 75));
      size = 3;
      itemCapacity = 28;
      
      warmupSpeed = 0.006f;
      
      newConsume();
      consume.time(180);
      consume.item(SglItems.salt_uranium, 7);
      consume.power(3.8f);
      newProduce();
      produce.items(ItemStack.with(SglItems.uranium_238, 3, SglItems.uranium_235, 1));
      
      newConsume();
      consume.time(120);
      consume.liquid(SglLiquids.iridium_gel, 0.2f);
      consume.power(3);
      newProduce();
      produce.item(SglItems.iridium, 1);
      
      newConsume();
      consume.time(120);
      consume.item(SglItems.crush_ore, 9);
      consume.power(2.8f);
      newProduce();
      produce.items(ItemStack.with(Items.sand, 3, Items.titanium, 1, Items.lead, 2, Items.thorium, 1));
      
      craftEffect = Fx.smeltsmoke;
      updateEffect = Fx.plasticburn;
      
      draw = new DrawFactory<>(this){
        TextureRegion rim, topRotator;
        
        @Override
        public void load(){
          super.load();
          rim = Core.atlas.find(block.name + "_rim");
          topRotator = Core.atlas.find(block.name + "_toprotator");
        }
        
        @Override
        public TextureRegion[] icons(){
          return new TextureRegion[]{
              bottom,
              rim,
              region,
              rotator,
              topRotator
          };
        }
        
        {
          drawDef = entity -> {
            Draw.rect(bottom, entity.x(), entity.y());
            Draw.color(Liquids.slag.color);
            Draw.alpha(entity.warmup);
            Draw.rect(liquid, entity.x(), entity.y());
            Draw.color();
            Drawf.spinSprite(rim, entity.x(), entity.y(), entity.totalProgress*0.8f);
            Draw.rect(region, entity.x(), entity.y());
            Drawf.spinSprite(rotator, entity.x(), entity.y(), entity.totalProgress*1.8f);
            Draw.rect(topRotator, entity.x(), entity.y(), -entity.totalProgress*1.2f);
            if(entity.consumer.current != null){
              BaseConsumers cons = entity.consumer.current;
              Color color;
              float alpha;
              if(cons.get(SglConsumeType.item) != null){
                Item item = cons.get(SglConsumeType.item).items[0].item;
                color = item.color;
                alpha = (float)entity.items.get(item)/itemCapacity;
              }
              else{
                Liquid liquid = cons.get(SglConsumeType.liquid).liquids[0].liquid;
                color = liquid.color;
                alpha = entity.liquids.get(liquid)/liquidCapacity;
              }
              Draw.color(color);
              Draw.alpha(alpha);
              Draw.rect(top, entity.x(), entity.y(), - entity.totalProgress*1.2f);
            }
          };
        }
      };
    }};
  
    lattice_constructor = new NormalCrafter("lattice_constructor"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 80, SglItems.crystal_FEX_power, 60, SglItems.crystal_FEX, 75, Items.phaseFabric, 80));
      size = 3;
      
      newConsume();
      consume.time(90);
      consume.liquid(SglLiquids.phase_FEX_liquid, 0.6f);
      consume.item(SglItems.strengthening_alloy, 1);
      consume.energy(1.25f);
      newProduce();
      produce.item(SglItems.crystal_FEX, 2);
  
      craftEffect = SglFx.FEXsmoke;
  
      draw = new DrawFactory<>(this){
        public TextureRegion framework, crystal, wave;
    
        @Override
        public void load(){
          super.load();
          wave = Core.atlas.find(name + "_wave");
          framework = Core.atlas.find(name + "_framework");
          crystal = getModAtlas("FEX_crystal");
        }
    
        {
          drawerType = e -> new DrawFactoryDrawer(e){
            final float[] alphas = {2.9f, 2.2f, 1.5f};
        
            @Override
            public void draw(){
              Draw.rect(bottom, e.x, e.y);
          
              if(e.progress > 0.3 || e.items.has(SglItems.strengthening_alloy)) Draw.rect(framework, e.x, e.y);
          
              Draw.alpha(e.progress);
              Draw.rect(crystal, e.x, e.y);
          
              Draw.alpha(e.warmup);
              Lines.lineAngleCenter(
                  e.x + Mathf.sin(e.totalProgress, 6, (float) Vars.tilesize/3*block.size),
                  e.y,
                  90,
                  (float) block.size*Vars.tilesize/2
              );
              Draw.color();
              Draw.rect(region, e.x, e.y);
          
              Draw.z(Layer.effect);
              for(int dist=2; dist>=0; dist--){
                Draw.color(Color.valueOf("FF756F"));
                Draw.alpha((alphas[dist] <= 1? alphas[dist]: alphas[dist] <= 1.5? 1: 0)*e.warmup);
                if(e.warmup > 0){
                  if(alphas[dist] < 0.4) alphas[dist] += 0.6;
                  for(int i=0; i<4; i++){
                    Draw.rect(wave,
                        e.x + dist*Geometry.d4(i).x*3 + 5*(Integer.compare(Geometry.d4(i).x, 0)),
                        e.y + dist*Geometry.d4(i).y*3 + 5*(Integer.compare(Geometry.d4(i).y, 0)),
                        (i+1)*90);
                  }
                  alphas[dist] -= 0.02*e.edelta();
                }
                else{
                  alphas[dist] = 1.5f + 0.7f*(2-dist);
                }
              }
            }
          };
        }
      };
    }};
  
    FEX_crystal_charger = new NormalCrafter("FEX_crystal_charger"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 70, SglItems.crystal_FEX, 60, Items.metaglass, 65, Items.phaseFabric, 70, Items.plastanium, 85));
      size = 3;
      
      newConsume();
      consume.time(90f);
      consume.item(SglItems.crystal_FEX, 1);
      consume.energy(2f);
      newProduce();
      produce.item(SglItems.crystal_FEX_power, 1);
      
      craftEffect = SglFx.crystalConstructed;
      
      draw = new DrawFactory<>(this){
        TextureRegion laser, crystal, crystalPower;
  
        @Override
        public void load(){
          super.load();
          laser = Core.atlas.find(name + "_laser");
          crystal = getModAtlas("FEX_crystal");
          crystalPower = getModAtlas("FEX_crystal_power");
        }
        
        {
          drawerType = e -> new DrawFactoryDrawer(e){
            float rotation;
            
            @Override
            public void draw(){
              Draw.rect(region, entity.x, entity.y);
              Draw.rect(rotator, entity.x, entity.y, (rotation += e.warmup*e.edelta()*1.8f) + 45);
              Draw.rect(rotator, entity.x, entity.y, -rotation - 45);
              Draw.z(Layer.effect);
              if(e.items.has(SglItems.crystal_FEX) || e.progress > 0.4f){
                Draw.rect(crystal, e.x, e.y);
                Draw.alpha(e.progress);
                Draw.rect(crystalPower, e.x, e.y);
                Draw.color();
              }
              Draw.color(Color.white, Color.blue.cpy().lerp(Color.white, 0.4f), 0.45f*(float) Math.random());
              Draw.alpha(e.warmup);
              Draw.rect(laser, entity.x, entity.y, rotation + 45);
              Draw.color(Color.white, Color.red.cpy().lerp(Color.white, 0.4f), 0.45f*(float) Math.random());
              Draw.alpha(e.warmup);
              Draw.rect(laser, entity.x, entity.y, -rotation - 45);
              Draw.z(Layer.effect + 1);
            }
          };
        }
      };
    }};
  
    matrix_cutter = new NormalCrafter("matrix_cutter"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 80, SglItems.crystal_FEX_power, 75, Items.metaglass, 80, Items.phaseFabric, 90, Items.surgeAlloy, 120));
      size = 4;
      
      newConsume();
      consume.time(120);
      consume.energy(4.85f);
      consume.items(ItemStack.with(SglItems.crystal_FEX_power, 1, SglItems.strengthening_alloy, 2));
      consume.liquid(SglLiquids.phase_FEX_liquid, 0.2f);
      newProduce();
      produce.item(SglItems.matrix_alloy, 1);
      
      craftEffect = Fx.smeltsmoke;
      
      draw = new DrawFactory<>(this){
        TextureRegion alloy, laserEmitter;
  
        @Override
        public void load(){
          super.load();
          alloy = Core.atlas.find(name + "_alloy");
          laserEmitter = Core.atlas.find(name + "_laser_emitter");
        }
        
        {
          drawerType = e -> new DrawFactoryDrawer(e){
            float timeRow;
            float dx, dy;
            
            @Override
            public void draw(){
              Draw.rect(region, e.x, e.y);
              timeRow += e.warmup*e.edelta();
              dx = 4*Mathf.sin(timeRow/6);
              dy = 4*Mathf.sin(timeRow/4);
              
              Draw.rect(laserEmitter, e.x, e.y + dy);
              Draw.rect(laserEmitter, e.x + dx, e.y, 90);
              Draw.color(Color.valueOf("FF756F"));
              Draw.alpha(e.warmup);
              Lines.stroke(0.6f);
              SglDraw.startBloom(31);
              Lines.line(e.x - 6, e.y + dy, e.x + 6, e.y + dy);
              Lines.line(e.x + dx, e.y + 6, e.x + dx, e.y - 6);
              SglDraw.endBloom();
              Draw.reset();
              if(e.items.get(SglItems.strengthening_alloy) >= 3) Draw.rect(alloy, e.x, e.y);
              Draw.rect(top, e.x, e.y);
            }
          };
        }
      };
    }};
  
    polymer_gravitational_generator = new NormalCrafter("polymer_gravitational_generator"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 180, SglItems.matrix_alloy, 900, SglItems.crystal_FEX_power, 100, SglItems.crystal_FEX, 120, SglItems.iridium, 80, SglItems.aerogel, 100, Items.surgeAlloy, 80, Items.phaseFabric, 90));
      size = 5;
      itemCapacity = 20;
      
      newConsume();
      consume.energy(5f);
      consume.items(ItemStack.with(
          SglItems.crystal_FEX_power, 2,
          SglItems.matrix_alloy, 2,
          SglItems.aerogel, 3,
          SglItems.iridium, 2
      ));
      consume.time(240);
      newProduce();
      produce.item(SglItems.degenerate_neutron_polymer, 1);
      
      craftEffect = SglFx.polymerConstructed;
      
      draw = new DrawFactory<>(this){
        {
          drawerType = e -> new DrawFactoryDrawer(e){
            float rotation;
            final Interval timer = new Interval();

            @Override
            public void draw(){
              Draw.rect(bottom, e.x, e.y);
              Draw.color(Color.valueOf("#D1D19F"));
              Draw.alpha(e.warmup);
              Draw.rect(liquid, e.x, e.y);
              Draw.color();
              Draw.rect(rotator, e.x, e.y, rotation += e.warmup*e.edelta()*1.75f);
              Draw.rect(rotator, e.x, e.y, -rotation);
              Draw.rect(region, e.x, e.y);

              drawForceField();
            }

            void drawForceField(){
              Draw.z(Layer.effect);
              Draw.color(Pal.reactorPurple);
              Draw.alpha(e.warmup);
              Lines.stroke(1.5f);
              Lines.square(e.x, e.y, 34, 45 + rotation/1.5f);
              Lines.square(e.x, e.y, 36, -rotation/1.5f);
              Lines.stroke(0.4f);
              Lines.square(e.x, e.y, 3 + Mathf.random(-0.15f, 0.15f));
              Lines.square(e.x, e.y, 4 + Mathf.random(-0.15f, 0.15f), 45);

              if(e.warmup > 0.01 && timer.get(30)){
                SglFx.forceField.at(e.x, e.y, (45 + rotation/1.5f)%360, Pal.reactorPurple, new float[]{1, e.warmup});
                Time.run(15, () -> SglFx.forceField.at(e.x, e.y, (-rotation/1.5f)%360, Pal.reactorPurple, new float[]{1, e.warmup}));
              }
            }
          };
        }
      };
    }};

    quality_generator = new MediumCrafter("quality_generator"){{
      requirements(Category.crafting, ItemStack.with());
      size = 4;

      energyCapacity = 8192;
      mediumCapacity = 32;

      newConsume();
      consume.energy(32f);
      newProduce();
      produce.medium(0.6f);
    }};

    substance_inverter = new MediumCrafter("substance_inverter"){{
      requirements(Category.crafting, ItemStack.with());
      size = 5;

      newConsume();
      consume.item(SglItems.degenerate_neutron_polymer, 1);
      consume.energy(5);
      consume.medium(3);
      consume.time(120);
      newProduce();
      produce.item(SglItems.anti_metter, 1);

      craftEffect = SglFx.explodeImpWave;

      clipSize = 150;

      initialed = e -> {
        e.setVar("lightningDrawer", new LightningContainer(){{
          generator = new CircleGenerator(){{
            originX = e.x;
            originY = e.y;

            radius = 13.5f;
            minInterval = 1f;
            maxInterval = 2.6f;
            maxSpread = 2.25f;
          }};
          maxWidth = minWidth = 0.8f;
          lifeTime = 24;
        }});

        LightningContainer con;
        e.setVar("lightnings", con = new LightningContainer(){{
          generator = new PointToPointGenerator(){{
            originX = e.x;
            originY = e.y;

            lerp = f -> 1 - f*f;

            maxSpread = 6.5f;
            minInterval = 5.5f;
            maxInterval = 8.5f;
          }};
        }});
        e.setVar("lightningGenerator", con.generator);
      };

      crafting = (NormalCrafterBuild e) -> {
        if(SglDraw.clipDrawable(e.x, e.y, clipSize) && Mathf.chanceDelta(e.warmup*0.1f)) e.<LightningContainer>getVar("lightningDrawer").create();
        if(Mathf.chanceDelta(e.warmup*0.04f)) SglFx.randomLightning.at(e.x, e.y, 0, Pal.reactorPurple);
      };

      craftTrigger = e -> {
        if(!SglDraw.clipDrawable(e.x, e.y, clipSize)) return;
        int a = Mathf.random(1, 3);
        for(int i = 0; i < a; i++){
          Tmp.v1.rnd(Mathf.random(65, 100));
          PointToPointGenerator gen = e.getVar("lightningGenerator");
          gen.targetX = e.x + Tmp.v1.x;
          gen.targetY = e.y + Tmp.v1.y;

          int amount = Mathf.random(3, 5);
          for(int i1 = 0; i1 < amount; i1++){
            e.<LightningContainer>getVar("lightnings").create();
          }

          if(Mathf.chance(0.25f)){
            SglFx.explodeImpWave.at(gen.targetX, gen.targetY);
            Angles.randLenVectors(System.nanoTime(), Mathf.random(4, 7), 2, 3.5f, (x, y) -> Particle.create(gen.targetX, gen.targetY, x, y, Mathf.random(3.25f, 4f)));
          }
          else{
            SglFx.spreadLightning.at(gen.targetX, gen.targetY, Pal.reactorPurple);
          }
        }
        Effect.shake(5.5f, 20f, e.x, e.y);
      };

      draw = new DrawFactory<>(this){{
        drawDef = e -> {
          Draw.rect(bottom, e.x, e.y);
          Draw.color(Pal.reactorPurple);
          Draw.alpha(e.warmup);
          SglDraw.startBloom(31);
          e.<LightningContainer>getVar("lightningDrawer").draw();
          SglDraw.endBloom();
          Draw.color();
          Draw.rect(region, e.x, e.y);
          Draw.z(Layer.effect);
          Draw.color(Pal.reactorPurple);
          e.<LightningContainer>getVar("lightnings").draw();

          float offsetH = Mathf.absin(0.6f, 4);
          float offsetW = Mathf.absin(0.4f, 12);

          SglDraw.drawLightEdge(
              e.x, e.y,
              (145 + offsetW)*e.warmup, 4*e.warmup,
              (35 + offsetH)*e.warmup, 2.25f*e.warmup,
              0
          );

          Draw.alpha(0.4f*e.warmup);
          Lines.stroke(5f + 3*e.warmup);
          Lines.circle(e.x, e.y, 73*e.warmup);
          Draw.alpha(0.6f*e.warmup);
          Lines.stroke( + 5*e.warmup);
          Lines.circle(e.x, e.y, 40*e.warmup);
          Draw.alpha(0.7f*e.warmup);
          Lines.stroke(2f);
          Lines.circle(e.x, e.y, 17*e.warmup);

          Draw.z(Layer.effect - 10);
          Draw.alpha(0.55f);
          SglDraw.drawLightEdge(
              e.x, e.y,
              (180 + offsetW)*e.warmup, 4*e.warmup,
              (60 + offsetH)*e.warmup, 2.25f*e.warmup,
              0
          );
        };
      }};
    }};

    destructor = new Destructor("destructor"){{
      requirements(Category.effect, ItemStack.with());
      size = 5;
      recipeIndfo = Core.bundle.get("infos.destructItems");

      draw = new SglDrawPlasma<>(this, 4);
    }};
  
    hadron_reconstructor = new AtomSchematicCrafter("hadron_reconstructor"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 180, SglItems.iridium, 120, SglItems.crystal_FEX_power, 120, SglItems.matrix_alloy, 90, SglItems.aerogel, 120, Items.surgeAlloy, 90));
      size = 4;
      itemCapacity = 24;
      
      craftEffect = SglFx.hadronReconstruct;
      
      draw = new SglDrawPlasma<>(this, 4){{
        plasma1 = Pal.reactorPurple;
        plasma2 = Pal.reactorPurple2;
        
        drawDef = e -> {
          Draw.rect(bottom, e.x, e.y);
          drawPlasma(e);
          Draw.alpha(e.progress);
          if(e.producer.current != null) Draw.rect(e.producer.current.get(ProduceType.item).items[0].item.uiIcon, e.x, e.y, 4, 4);
          Draw.color();
          Draw.rect(region, e.x, e.y);
        };
      }};
    }};
  }
}
