package singularity.contents;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.draw.*;
import mindustry.world.meta.Stat;
import singularity.Sgl;
import singularity.Singularity;
import singularity.graphic.SglDraw;
import singularity.world.SglFx;
import singularity.world.blocks.drills.ExtendMiner;
import singularity.world.blocks.drills.ExtendableDrill;
import singularity.world.blocks.drills.MatrixMiner;
import singularity.world.blocks.drills.MatrixMinerEdge;
import singularity.world.blocks.product.FloorCrafter;
import singularity.world.blocks.product.SglAttributeCrafter;
import singularity.world.consumers.SglConsumeFloor;
import singularity.world.draw.DrawBottom;
import singularity.world.draw.DrawDirSpliceBlock;
import singularity.world.draw.DrawExpandPlasma;
import singularity.world.meta.SglAttribute;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumeType;

import static mindustry.type.ItemStack.with;

public class ProductBlocks implements ContentList {
  /**岩层钻井机*/
  public static Block rock_drill,
  /**岩石粉碎机*/
  rock_crusher,
  /**潮汐钻头*/
  tidal_drill,
  /**力场延展仓*/
  force_field_extender,
  /**矩阵矿床*/
  matrix_miner,
  /**矿床框架*/
  matrix_miner_node;

  @Override
  public void load() {
    rock_drill = new SglAttributeCrafter("rock_drill"){{
      requirements(Category.production, with(Items.titanium, 45, Items.lead, 30, Items.copper, 30));
      size = 2;
      liquidCapacity = 24;
      oneOfOptionCons = true;
      health = 180;

      updateEffect = Fx.pulverizeSmall;
      craftEffect = Fx.mine;
      craftEffectColor = Pal.lightishGray;

      warmupSpeed = 0.005f;

      hasLiquids = true;

      autoSelect = true;

      setAttrBooster(SglAttribute.bitumen, 1.12f);
      
      newConsume();
      consume.time(90);
      consume.liquid(Liquids.water, 0.2f);
      consume.power(1.75f);
      newProduce();
      produce.item(SglItems.rock_bitumen, 1);

      newConsume();
      consume.time(60);
      consume.liquid(Liquids.cryofluid, 0.2f);
      consume.power(1.75f);
      newProduce();
      produce.item(SglItems.rock_bitumen, 2);

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawLiquidRegion(Liquids.water){{
            suffix = "_liquid";
          }},
          new DrawRegion("_rotator"){{
            rotateSpeed = 1.5f;
            spinSprite = true;
          }},
          new DrawDefault(),
          new DrawRegion("_top")
      );
    }};

    rock_crusher = new FloorCrafter("rock_crusher"){{
      requirements(Category.production, ItemStack.with(
          SglItems.strengthening_alloy, 40,
          SglItems.aerogel, 55,
          Items.silicon, 60,
          Items.titanium, 50,
          Items.graphite, 60
      ));
      size = 3;

      warmupSpeed = 0.004f;
      updateEffect = Fx.pulverizeSmall;
      craftEffect = Fx.mine;
      craftEffectColor = Items.sand.color;

      oneOfOptionCons = false;

      itemCapacity = 25;
      liquidCapacity = 30;

      willDumpItems.add(SglItems.alkali_stone);

      newConsume();
      consume.time(30f);
      consume.power(2.2f);
      consume.add(new SglConsumeFloor<FloorCrafterBuild>(
          Blocks.stone, 1.2f/9f,
          Blocks.craters, 0.8f/9f,
          Blocks.dacite, 0.8f/9f,
          Blocks.shale, 1f/9f,
          Blocks.salt, 1f/9f
      ){{baseEfficiency = 0;}});
      consume.addSelfAccess(ConsumeType.item, SglItems.alkali_stone);
      newProduce();
      produce.item(Items.sand, 1);

      newOptionalConsume((FloorCrafterBuild e, BaseConsumers c) -> {}, (s, c) -> {
        s.add(Stat.output, SglItems.alkali_stone);
      });
      consume.setConsTrigger((FloorCrafterBuild e) -> {
        if (e.acceptItem(e, SglItems.alkali_stone)) e.handleItem(e, SglItems.alkali_stone);
      });
      consume.time(45f);
      consume.add(new SglConsumeFloor<FloorCrafterBuild>(
          Blocks.stone, 0.4f/9f,
          Blocks.craters, 0.5f/9f,
          Blocks.salt, 2f/9f
      ){{baseEfficiency = 0;}});
      consume.optionalAlwaysValid = false;

      newBooster(1.8f);
      consume.liquid(Liquids.water, 0.12f);

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawDefault(),
          new DrawBlock() {
            TextureRegion rim;
            final Color heatColor = Color.valueOf("ff5512");

            @Override
            public void draw(Building build) {
              NormalCrafterBuild e = (NormalCrafterBuild) build;

              Draw.color(heatColor);
              Draw.alpha(e.workEfficiency()*0.6f*(1f - 0.3f + Mathf.absin(Time.time, 3f, 0.3f)));
              Draw.blend(Blending.additive);
              Draw.rect(rim, e.x, e.y);
              Draw.blend();
              Draw.color();
            }

            @Override
            public void load(Block block) {
              rim = Core.atlas.find(block.name + "_rim");
            }
          },
          new DrawRegion("_rotator"){{
            rotateSpeed = 2.8f;
            spinSprite = true;
          }},
          new DrawRegion("_top")
      );
    }};

    tidal_drill = new ExtendableDrill("tidal_drill"){{
      requirements(Category.production, ItemStack.with(
          SglItems.degenerate_neutron_polymer, 50,
          SglItems.strengthening_alloy, 120,
          SglItems.aerogel, 90,
          SglItems.crystal_FEX_power, 75,
          SglItems.iridium, 40,
          Items.phaseFabric, 60
      ));
      size = 4;

      itemCapacity = 50;
      liquidCapacity = 30;

      bitHardness = 10;
      drillTime = 180;

      newConsume();
      consume.energy(1.25f);

      newBooster(4.2f);
      consume.liquid(SglLiquids.phase_FEX_liquid, 0.15f);
      newBooster(3.1f);
      consume.liquid(SglLiquids.FEX_liquid, 0.12f);

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawExpandPlasma(){{
            plasmas = 2;
            plasma1 = Pal.reactorPurple;
            plasma2 = Pal.reactorPurple2;
          }},
          new DrawDefault(),
          new DrawBlock(){
            @Override
            public void draw(Building build){
              if(Sgl.config.animateLevel < 3){
                if(Sgl.config.animateLevel == 2){
                  float z = Draw.z();
                  Draw.z(Layer.bullet);
                  Draw.color(Pal.reactorPurple);
                  SglDraw.drawLightEdge(
                      build.x, build.y,
                      8 + 8*build.warmup(), 2.5f*build.warmup(),
                      8 + 8*build.warmup(), 2.5f*build.warmup()
                  );
                  Draw.z(z);
                  Draw.color();
                }

                return;
              }

              ExtendableDrillBuild e = (ExtendableDrillBuild) build;
              float z = Draw.z();
              Draw.z(Layer.bullet);
              Draw.color(Pal.reactorPurple);
              float lerp = (float) (-2.2*Math.pow(e.warmup, 2) + 3.2*e.warmup);
              Fill.circle(e.x, e.y, 3*e.warmup);
              SglDraw.drawLightEdge(e.x, e.y,
                  26*lerp, 2.5f*lerp, e.rotatorAngle, 1,
                  16*lerp, 2f*lerp, -e.rotatorAngle, 1);
              Draw.z(z);
              Draw.color();
            }
          },
          new DrawRegion("_top")
      );
    }};

    force_field_extender = new ExtendMiner("force_field_extender"){{
      requirements(Category.production, ItemStack.with(
          SglItems.degenerate_neutron_polymer, 20,
          SglItems.crystal_FEX, 20,
          SglItems.iridium, 8,
          SglItems.strengthening_alloy, 30
      ));
      size = 2;

      master = (ExtendableDrill) tidal_drill;
      mining = SglFx.shrinkParticle(10, 1.5f, 120, Pal.reactorPurple);

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawDefault(),
          new DrawDirSpliceBlock<ExtendMinerBuild>(){{
            simpleSpliceRegion = true;

            spliceBits = e -> {
              int res = 0;
              for(int i = 0; i < 4; i++){
                if ((e.splice & 1 << i*2) != 0) res |= 1 << i;
              }
              return res;
            };

            planSplicer = (plan, other) -> plan.block instanceof ExtendMiner self && other.block instanceof ExtendMiner oth
                && self.chainable(oth) && oth.chainable(self);
          }},
          new DrawBlock(){
            @Override
            public void draw(Building build){
              if(Sgl.config.animateLevel < 2) return;

              ExtendMinerBuild e = (ExtendMinerBuild) build;

              Draw.z(Layer.effect);
              Draw.color(Pal.reactorPurple);
              SglDraw.drawLightEdge(e.x, e.y, 8*e.warmup, 2f*e.warmup, 8*e.warmup, 2f*e.warmup, 45);
              SglDraw.drawLightEdge(e.x, e.y, 15*e.warmup, 2f*e.warmup, 45, 0.6f, 15*e.warmup, 2f*e.warmup, 45, 0.6f);
            }
          }
      );
    }};

    matrix_miner = new MatrixMiner("matrix_miner"){{
      requirements(Category.production, ItemStack.with(
          SglItems.matrix_alloy, 130,
          SglItems.crystal_FEX_power, 80,
          SglItems.strengthening_alloy, 90,
          SglItems.aerogel, 90,
          Items.phaseFabric, 65,
          Items.graphite, 90,
          SglItems.iridium, 45
      ));
      size = 5;
      linkOffset = 10.75f;
    }};

    matrix_miner_node = new MatrixMinerEdge("matrix_miner_node"){
      {
        requirements(Category.production, ItemStack.with(
            SglItems.matrix_alloy, 30,
            SglItems.crystal_FEX_power, 25,
            SglItems.strengthening_alloy, 16,
            SglItems.aerogel, 20
        ));
        size = 3;
        linkOffset = 5.5f;
      }

      @Override
      public void load(){
        super.load();
        linkRegion = Singularity.getModAtlas("matrix_miner_linking");
        linkCapRegion = Singularity.getModAtlas("matrix_miner_link_cap");
      }
    };
  }
}
