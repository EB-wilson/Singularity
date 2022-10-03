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
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.draw.*;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.BlockStatus;
import singularity.Singularity;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.type.SglLiquidStack;
import singularity.world.SglFx;
import singularity.world.blocks.function.Destructor;
import singularity.world.blocks.product.*;
import singularity.world.consumers.SglConsumeType;
import singularity.world.consumers.SglConsumers;
import singularity.world.draw.*;
import singularity.world.lightnings.LightningContainer;
import singularity.world.lightnings.generator.CircleGenerator;
import singularity.world.lightnings.generator.PointToPointGenerator;
import singularity.world.meta.SglStat;
import singularity.world.particles.SglParticleModels;
import singularity.world.products.SglProduceType;
import universecore.components.blockcomp.FactoryBuildComp;
import universecore.util.UncLiquidStack;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.UncConsumeItems;
import universecore.world.consumers.UncConsumeLiquids;
import universecore.world.producers.ProduceLiquids;
import universecore.world.producers.ProduceType;

import static mindustry.Vars.state;
import static singularity.graphic.SglDrawConst.transColor;

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
      consume.consValidCondition((NormalCrafterBuild e) -> e.getVar(Integer.class, 0) > 0);
      newProduce();
      produce.item(Items.phaseFabric, 4);
      
      craftEffect = Fx.smeltsmoke;
  
      Cons<Item> recipe = item -> {
        newOptionalConsume((NormalCrafterBuild e, BaseConsumers c) -> {
          e.setVar(2);
        }, (s, c) -> {
          s.add(SglStat.effect, t -> t.add(Core.bundle.get("misc.doConsValid")));
        }).overdriveValid(false);
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
          if(getVar(Integer.class, 0) > 0) setVar(getVar(Integer.class) - 1);
        }
  
        @Override
        public BlockStatus status(){
          BlockStatus status = super.status();
          if(status == BlockStatus.noInput && getVar(Integer.class, 0) > 0) return BlockStatus.noOutput;
          return status;
        }
      };

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawWeave(){
            @Override
            public void load(Block block){
              weave = Core.atlas.find(block.name + "_top");
            }
          },
          new DrawDefault()
      );
    }};
  
    culturing_barn = new SpliceCrafter("culturing_barn"){
      {
        requirements(Category.production, ItemStack.with());
        hasLiquids = true;

        newConsume();
        consume.liquid(Liquids.water, 0.02f);
        newProduce();
        produce.liquid(SglLiquids.oxygen, 0.005f);
        produce.liquid(SglLiquids.algae_mud, 0.001f);
        
        ambientSound = Sounds.none;

        draw = new DrawMulti(
            new DrawBottom(),
            new DrawLiquidRegion(Liquids.water){{
              suffix = "_liquid";
            }},
            new DrawSpliceBlock(){{
              interCorner = false;
            }}
        );
        
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
        addBar("efficiency", (SglBuilding entity) ->
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
      consume.liquid(SglLiquids.spore_cloud, 0.06f);
      newProduce();
      produce.item(Items.sporePod, 2);
      
      setAttrBooster(Attribute.spores, 0.86f);
      setAttrBooster(Attribute.heat, 1.8f, 3f);
      
      draw = new DrawMulti(
          new DrawBottom(),
          new DrawCultivator(){
            @Override
            public void load(Block block){
              middle = Core.atlas.find(block.name + "_middle");
            }
          },
          new DrawDefault(),
          new DrawRegion("_top")
      );
    }};
  
    electrolytor = new NormalCrafter("electrolytor"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 60, Items.copper, 40, Items.silicon, 45, Items.metaglass, 60, Items.plastanium, 35));
      size = 2;
      
      newConsume();
      consume.liquid(Liquids.water, 0.2f);
      consume.power(2.5f);
      newProduce().color = Liquids.water.color;
      produce.liquids(SglLiquidStack.with(SglLiquids.oxygen, 0.1f, Liquids.hydrogen, 0.2f));
      
      newConsume();
      consume.item(Items.sporePod, 1);
      consume.liquid(Liquids.water, 0.1f);
      consume.power(2);
      consume.time(60);
      newProduce().color = Items.sporePod.color;
      produce.liquid(SglLiquids.spore_cloud, 0.18f);
      
      newConsume();
      consume.liquid(SglLiquids.algae_mud, 0.2f);
      consume.time(120);
      consume.power(2.4f);
      newProduce().color = SglLiquids.algae_mud.color;
      produce.item(SglItems.chlorella_block, 1);

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawDyColorCultivator<NormalCrafterBuild>(){{
            plantColor = e -> e.producer().current != null? e.producer().current.color: Color.white;
            plantColorLight = e -> plantColor.get(e).cpy().lerp(Color.white, 0.24f);
            alpha = e -> e.liquids.currentAmount()/liquidCapacity;
          }},
          new DrawDefault()
      );
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
      produce.liquids(SglLiquidStack.with(SglLiquids.mixed_tar, 0.1f, SglLiquids.methane, 0.2f));
      
      craftEffect = Fx.smeltsmoke;
      
      draw = new DrawMulti(
          new DrawDefault(),
          new DrawFlame(){
            @Override
            public void load(Block block){
              top = Core.atlas.find(block.name + "_top");
              block.clipSize = Math.max(block.clipSize, (lightRadius + lightSinMag) * 2f * block.size);
            }
          }
      );
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

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawRegionDynamic<NormalCrafterBuild>("_liquid"){{
            alpha = e -> {
              UncConsumeItems<?> ci = e.consumer.current != null? e.consumer.current.get(SglConsumeType.item): null;
              return ci != null? (float) e.items.get(ci.items[0].item)/e.block.itemCapacity: 0;
            };
            color = e -> e.producer.current != null? e.producer.current.color: Color.white;
            spinSprite = true;
          }},
          new DrawDefault(),
          new DrawRegionDynamic<NormalCrafterBuild>("_laser"){{
            rotation = e -> e.totalProgress()*1.5f;
            alpha = FactoryBuildComp::workEfficiency;
          }},
          new DrawRegion("_rotator"){{
            rotateSpeed = 1.5f;
            spinSprite = true;
          }},
          new DrawRegion("_top")
      );
    }};
    
    ore_washer = new NormalCrafter("ore_washer"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 60, Items.graphite, 40, Items.lead, 45, Items.metaglass, 60));
      size = 2;
      hasLiquids = true;
      itemCapacity = 20;
      liquidCapacity = 24f;
      
      newConsume();
      consume.time(120f);
      consume.liquids(UncLiquidStack.with(Liquids.water, 0.4f, SglLiquids.rock_bitumen, 0.2f));
      consume.power(1.8f);
      newProduce();
      produce.liquid(SglLiquids.FEX_liquid, 0.1f);
      produce.items(ItemStack.with(Items.sand, 4, SglItems.crush_ore, 1, SglItems.crush_uranium_ore, 1)).random();
      
      craftEffect = Fx.pulverizeMedium;

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawLiquidRegion(Liquids.water){{
            suffix = "_liquid";
          }},
          new DrawRegion("_rotator"){{
            rotateSpeed= 4.5f;
            spinSprite = true;
          }},
          new DrawRegion("_top"),
          new DrawRegionDynamic<NormalCrafterBuild>("_point"){{
            color = e -> {
              BaseConsume<?> cons = e.consumer.current == null? null: ((SglConsumers) (e.consumer.current)).first();
              if(cons instanceof UncConsumeLiquids){
                Liquid liquid = ((UncConsumeLiquids<?>) cons).liquids[0].liquid;
                if(liquid == Liquids.water) liquid = ((UncConsumeLiquids<?>) cons).liquids[1].liquid;
                return liquid.color;
              }else if(cons instanceof UncConsumeItems){
                Item item = ((UncConsumeItems<?>) cons).items[0].item;
                return item.color;
              }else return Color.white;
            };
            alpha = e -> {
              BaseConsume<?> cons = e.consumer.current == null? null: ((SglConsumers) (e.consumer.current)).first();
              if(cons instanceof UncConsumeLiquids){
                Liquid liquid = ((UncConsumeLiquids<?>) cons).liquids[0].liquid;
                if(liquid == Liquids.water) liquid = ((UncConsumeLiquids<?>) cons).liquids[1].liquid;
                return e.liquids.get(liquid)/e.block.liquidCapacity;
              }else if(cons instanceof UncConsumeItems){
                Item item = ((UncConsumeItems<?>) cons).items[0].item;
                return (float) e.items.get(item)/e.block.itemCapacity;
              }else return 0;
            };
          }}
      );
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

      draw = new DrawMulti(
          new DrawCultivator(){
            {
              plantColor = Color.valueOf("#C73A3A");
              plantColorLight = Color.valueOf("#E57D7D");
            }

            @Override
            public void load(Block block){
              middle = Core.atlas.find(block.name + "_middle");
            }
          },
          new DrawDefault()
      );
    }};
  
    FEX_phase_mixer = new NormalCrafter("FEX_phase_mixer"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 90, Items.phaseFabric, 85, Items.silicon, 80));
      size = 2;
      hasLiquids = true;
      liquidCapacity = 12;
      
      newConsume();
      consume.time(90);
      consume.item(Items.phaseFabric, 1);
      consume.liquid(SglLiquids.FEX_liquid, 0.2f);
      consume.power(1.9f);
      newProduce();
      produce.liquid(SglLiquids.phase_FEX_liquid, 0.2f);
      
      draw = new DrawMulti(
          new DrawBottom(),
          new DrawLiquidTile(SglLiquids.FEX_liquid),
          new DrawLiquidTile(SglLiquids.phase_FEX_liquid){{drawLiquidLight = true;}},
          new DrawDefault()
      );
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

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawRegionDynamic<NormalCrafterBuild>("_flue"){{
            alpha = e -> e.items.get(SglItems.strengthening_alloy) > 0 || e.progress() > 0.4f? 1: 0;
          }},
          new DrawRegionDynamic<NormalCrafterBuild>("_top"){{
            alpha = FactoryBuildComp::progress;
            color = e -> e.producer.current != null? e.producer.current.color: transColor;
          }}
      );
    }};
  
    strengthening_alloy_smelter = new NormalCrafter("strengthening_alloy_smelter"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 60, Items.thorium, 70, SglItems.aerogel, 60, Items.lead, 80, Items.silicon, 60));
      size = 3;
      hasLiquids = true;
      itemCapacity = 20;
      
      newConsume();
      consume.time(60);
      consume.power(3.5f);
      consume.items(ItemStack.with(SglItems.coke, 1, Items.titanium, 2, Items.thorium, 1));
      consume.liquid(SglLiquids.mixed_chemical_gel, 0.2f);
      newProduce();
      produce.item(SglItems.strengthening_alloy, 1);
      
      craftEffect = Fx.smeltsmoke;

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawLiquidRegion(SglLiquids.mixed_chemical_gel){{
            suffix = "_liquid";
          }},
          new DrawDefault(),
          new DrawFlame(){
            @Override
            public void load(Block block){
              top = Core.atlas.find(block.name + "_top");
              block.clipSize = Math.max(block.clipSize, (lightRadius + lightSinMag) * 2f * block.size);
            }
          }
      );
    }};
    
    gel_mixer = new NormalCrafter("gel_mixer"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 90, Items.lead, 100, Items.thorium, 75, Items.graphite, 60, Items.metaglass, 120));
      size = 3;
      hasLiquids = true;
      liquidCapacity = 40f;
      
      newConsume();
      consume.time(120f);
      consume.power(1.8f);
      consume.items(ItemStack.with(Items.pyratite, 1));
      consume.liquid(SglLiquids.oxygen, 0.4f);
      consume.liquids(UncLiquidStack.with(SglLiquids.mixed_tar, 0.2f, Liquids.water, 0.4f));
      newProduce();
      produce.liquid(SglLiquids.mixed_chemical_gel, 0.4f);
      
      updateEffect = Fx.plasticburn;
      updateEffectChance = 0.035f;
      craftEffect = SglFx.steamBreakOut;

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawLiquidRegion(Liquids.water){{
            suffix = "_liquid";
          }},
          new DrawRegion("_rotator"){{
            rotateSpeed = 2;
          }},
          new DrawPistons(){{
            sinScl = 6f;
            sinMag = 2.5f;
            sinOffset = Mathf.halfPi;
          }},
          new DrawRegionDynamic<NormalCrafterBuild>("_center"){{
            alpha = e -> e.workEfficiency()*0.75f;
            color = e -> {
              ProduceLiquids<?> pl = e.producer.current == null? null: e.producer.current.get(SglProduceType.liquid);
              return pl == null? transColor: pl.liquids[0].liquid.color;
            };
          }},
          new DrawDefault()
      );
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
    
      draw = new DrawMulti(
          new DrawBottom(),
          new DrawFrame<NormalCrafterBuild>(){{
            frames = 13;
            cursor = e -> (int)(13*(((Time.time % 30)/30)%(13f/90))/(13f/90));
            alpha = FactoryBuildComp::workEfficiency;
          }},
          new DrawDefault()
      );
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

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawRegionDynamic<NormalCrafterBuild>("_liquid"){{
            color = e -> Liquids.slag.color;
            alpha = FactoryBuildComp::workEfficiency;
          }},
          new DrawRegion("_rim"){{
            rotateSpeed = 0.8f;
            spinSprite = true;
          }},
          new DrawDefault(),
          new DrawRegion("_rotator"){{
            rotateSpeed = 1.8f;
            spinSprite = true;
          }},
          new DrawRegion("_toprotator"){{
            rotateSpeed = -1.2f;
          }},
          new DrawRegionDynamic<NormalCrafterBuild>("_top"){{
            rotation = e -> -e.totalProgress()*1.2f;
            color = e -> {
              BaseConsume<?> cons = e.consumer.current == null? null: ((SglConsumers) (e.consumer.current)).first();
              if(cons instanceof UncConsumeLiquids){
                Liquid liquid = ((UncConsumeLiquids<?>) cons).liquids[0].liquid;
                if(liquid == Liquids.water) liquid = ((UncConsumeLiquids<?>) cons).liquids[1].liquid;
                return liquid.color;
              }else if(cons instanceof UncConsumeItems){
                Item item = ((UncConsumeItems<?>) cons).items[0].item;
                return item.color;
              }else return Color.white;
            };
            alpha = e -> {
              BaseConsume<?> cons = e.consumer.current == null? null: ((SglConsumers) (e.consumer.current)).first();
              if(cons instanceof UncConsumeLiquids){
                Liquid liquid = ((UncConsumeLiquids<?>) cons).liquids[0].liquid;
                if(liquid == Liquids.water) liquid = ((UncConsumeLiquids<?>) cons).liquids[1].liquid;
                return e.liquids.get(liquid)/e.block.liquidCapacity;
              }else if(cons instanceof UncConsumeItems){
                Item item = ((UncConsumeItems<?>) cons).items[0].item;
                return (float) e.items.get(item)/e.block.itemCapacity;
              }else return 0;
            };
          }}
      );
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

      initialed = e -> e.setVar("drawAlphas", new float[]{2.9f, 2.2f, 1.5f});

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawRegionDynamic<NormalCrafterBuild>("_framework"){{
            alpha = e -> e.items.has(SglItems.strengthening_alloy) || e.progress() > 0.4f? 1: 0;
          }},
          new DrawRegionDynamic<NormalCrafterBuild>(){
            {
              alpha = FactoryBuildComp::progress;
            }

            @Override
            public void load(Block block){
              region = Singularity.getModAtlas("FEX_crystal");
            }
          },
          new DrawBlock(){
            @Override
            public void draw(Building build){
              NormalCrafterBuild e = (NormalCrafterBuild) build;
              Draw.alpha(e.workEfficiency());
              Lines.lineAngleCenter(
                  e.x + Mathf.sin(e.totalProgress(), 6, (float) Vars.tilesize/3*size),
                  e.y,
                  90,
                  (float) size*Vars.tilesize/2
              );
              Draw.color();
            }
          },
          new DrawDefault(),
          new DrawBlock(){
            TextureRegion wave;

            @Override
            public void load(Block block){
              wave = Core.atlas.find(name + "_wave");
            }

            @Override
            public void draw(Building build){
              NormalCrafterBuild e = (NormalCrafterBuild) build;
              float[] alphas = e.getVar("drawAlphas");

              Draw.z(Layer.effect);
              for(int dist=2; dist>=0; dist--){
                Draw.color(Color.valueOf("FF756F"));
                Draw.alpha((alphas[dist] <= 1? alphas[dist]: alphas[dist] <= 1.5? 1: 0)*e.workEfficiency());
                if(e.workEfficiency() > 0){
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
          }
      );
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

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawRegionDynamic<NormalCrafterBuild>(){
            {
              alpha = e -> e.items.has(SglItems.crystal_FEX) || e.progress() > 0.4f? 1: 0;
            }

            @Override
            public void load(Block block){
              region = Singularity.getModAtlas("FEX_crystal");
            }
          },
          new DrawRegionDynamic<NormalCrafterBuild>(){
            {
              layer = Layer.effect;
              alpha = e -> e.items.has(SglItems.crystal_FEX) || e.progress() > 0.4f? e.progress(): 0;
            }

            @Override
            public void load(Block block){
              region = Singularity.getModAtlas("FEX_crystal_power");
            }
          }
      );
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

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawRegionDynamic<NormalCrafterBuild>("_alloy"){{
            alpha = e -> e.items.get(SglItems.strengthening_alloy) >= 2? 1: 0;
          }},
          new DrawBlock(){
            @Override
            public void draw(Building build){
              NormalCrafterBuild e = (NormalCrafterBuild) build;
              float dx = 4*Mathf.sin(build.totalProgress()*1.5f);

              Draw.color(Color.valueOf("FF756F"));
              Draw.alpha(e.workEfficiency());
              Lines.stroke(0.8f);
              SglDraw.startBloom(31);
              Lines.line(e.x + dx, e.y + 6, e.x + dx, e.y - 6);
              SglDraw.endBloom();
              Draw.reset();
            }
          },
          new DrawDefault()
      );
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

      initialed = e -> e.setVar("timer", new Interval());

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawRegionDynamic<NormalCrafterBuild>("_liquid"){{
            color = e -> SglDrawConst.ion;
            alpha = FactoryBuildComp::workEfficiency;
          }},
          new DrawRegion("_rotator"){{
            rotateSpeed = 1.75f;
          }},
          new DrawRegion("_rotator"){{
            rotateSpeed = -1.75f;
          }},
          new DrawDefault(),
          new DrawBlock(){
            @Override
            public void draw(Building build){
              NormalCrafterBuild e = (NormalCrafterBuild) build;
              Draw.z(Layer.effect);
              Draw.color(Pal.reactorPurple);
              Draw.alpha(e.workEfficiency());
              Lines.stroke(1.5f);
              float rotation = e.totalProgress()*1.75f;
              Lines.square(e.x, e.y, 34, 45 + rotation/1.5f);
              Lines.square(e.x, e.y, 36, -rotation/1.5f);
              Lines.stroke(0.4f);
              Lines.square(e.x, e.y, 3 + Mathf.random(-0.15f, 0.15f));
              Lines.square(e.x, e.y, 4 + Mathf.random(-0.15f, 0.15f), 45);

              if(e.workEfficiency() > 0.01 && e.<Interval>getVar("timer").get(30)){
                SglFx.forceField.at(e.x, e.y, (45 + rotation/1.5f)%360, Pal.reactorPurple, new float[]{1, e.workEfficiency()});
                Time.run(15, () -> SglFx.forceField.at(e.x, e.y, (-rotation/1.5f)%360, Pal.reactorPurple, new float[]{1, e.workEfficiency()}));
              }
            }
          }
      );
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

      energyCapacity = 1024f;

      newConsume();
      consume.item(SglItems.degenerate_neutron_polymer, 1);
      consume.energy(5);
      consume.medium(2.25f);
      consume.time(120);
      newProduce();
      produce.item(SglItems.anti_metter, 1);

      craftEffect = SglFx.explodeImpWaveBig;

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
        if(SglDraw.clipDrawable(e.x, e.y, clipSize) && Mathf.chanceDelta(e.workEfficiency()*0.1f)) e.<LightningContainer>getVar("lightningDrawer").create();
        if(Mathf.chanceDelta(e.workEfficiency()*0.04f)) SglFx.randomLightning.at(e.x, e.y, 0, Pal.reactorPurple);
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
            Angles.randLenVectors(System.nanoTime(), Mathf.random(4, 7), 2, 3.5f,
                (x, y) -> SglParticleModels.nuclearParticle.create(gen.targetX, gen.targetY, x, y, Mathf.random(3.25f, 4f)));
          }
          else{
            SglFx.spreadLightning.at(gen.targetX, gen.targetY, Pal.reactorPurple);
          }
        }
        Effect.shake(5.5f, 20f, e.x, e.y);
      };

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawBlock(){
            @Override
            public void draw(Building build){
              NormalCrafterBuild e = (NormalCrafterBuild) build;
              Draw.color(Pal.reactorPurple);
              Draw.alpha(e.workEfficiency());
              SglDraw.startBloom(31);
              e.<LightningContainer>getVar("lightningDrawer").draw();
              SglDraw.endBloom();
              Draw.color();
            }
          },
          new DrawDefault(),
          new DrawBlock(){
            @Override
            public void draw(Building build){
              NormalCrafterBuild e = (NormalCrafterBuild) build;
              Draw.z(Layer.effect);
              Draw.color(Pal.reactorPurple);
              e.<LightningContainer>getVar("lightnings").draw();

              float offsetH = Mathf.absin(0.6f, 4);
              float offsetW = Mathf.absin(0.4f, 12);

              SglDraw.drawLightEdge(
                  e.x, e.y,
                  (35 + offsetH)*e.workEfficiency(), 2.25f*e.workEfficiency(),
                  (145 + offsetW)*e.workEfficiency(), 4*e.workEfficiency()
              );

              Draw.z(Layer.bullet - 10);
              Draw.alpha(0.3f*e.workEfficiency());
              SglDraw.gradientCircle(e.x, e.y, 72*e.workEfficiency(), 6 + 6*e.workEfficiency(), transColor);
              Draw.alpha(0.35f*e.workEfficiency());
              SglDraw.gradientCircle(e.x, e.y, 41*e.workEfficiency(), -7*e.workEfficiency(), transColor);
              Draw.alpha(0.65f*e.workEfficiency());
              SglDraw.gradientCircle(e.x, e.y, 18*e.workEfficiency(), -3*e.workEfficiency(), transColor);
              Draw.alpha(1);
              SglDraw.drawLightEdge(
                  e.x, e.y,
                  (60 + offsetH)*e.workEfficiency(), 2.25f*e.workEfficiency(), 0, 0.55f,
              (180 + offsetW)*e.workEfficiency(), 4*e.workEfficiency(), 0, 0.55f
              );
            }
          }
      );
    }};

    destructor = new Destructor("destructor"){{
      requirements(Category.effect, ItemStack.with());
      size = 5;
      recipeIndfo = Core.bundle.get("infos.destructItems");

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawPlasma(){{
            suffix = "_plasma_";
            plasma1 = SglDrawConst.matrixNet;
            plasma2 = SglDrawConst.matrixNetDark;
          }},
          new DrawDefault()
      );
    }};
  
    hadron_reconstructor = new AtomSchematicCrafter("hadron_reconstructor"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 180, SglItems.iridium, 120, SglItems.crystal_FEX_power, 120, SglItems.matrix_alloy, 90, SglItems.aerogel, 120, Items.surgeAlloy, 90));
      size = 4;
      itemCapacity = 24;
      
      craftEffect = SglFx.hadronReconstruct;

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawPlasma(){{
            suffix = "_plasma_";
            plasma1 = Pal.reactorPurple;
            plasma2 = Pal.reactorPurple2;
          }},
          new DrawBlock(){
            @Override
            public void draw(Building build){
              NormalCrafterBuild e = (NormalCrafterBuild) build;
              Draw.alpha(e.progress());
              if(e.producer.current != null) Draw.rect(e.producer.current.get(ProduceType.item).items[0].item.uiIcon, e.x, e.y, 4, 4);
              Draw.color();
            }
          },
          new DrawDefault()
      );
    }};
  }
}
