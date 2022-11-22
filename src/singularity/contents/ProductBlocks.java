package singularity.contents;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.draw.*;
import singularity.Singularity;
import singularity.graphic.SglDraw;
import singularity.world.SglFx;
import singularity.world.blocks.drills.ExtendMiner;
import singularity.world.blocks.drills.ExtendableDrill;
import singularity.world.blocks.drills.MatrixMiner;
import singularity.world.blocks.drills.MatrixMinerEdge;
import singularity.world.blocks.product.SglAttributeCrafter;
import singularity.world.draw.DrawBottom;
import singularity.world.draw.DrawDirSpliceBlock;
import singularity.world.draw.DrawExpandPlasma;
import singularity.world.meta.SglAttribute;

import static mindustry.type.ItemStack.with;

public class ProductBlocks implements ContentList {
  /**岩层钻井机*/
  public static Block rock_drill,
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
      produce.item(SglItems.rock_bitumen, 1);

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

      squareSprite = false;

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

      squareSprite = false;

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
                if(e.splice()[i] != -1) res |= 0b0001 << i;
              }
              return res;
            };

            planSplicer = (plan, other) -> plan.block instanceof ExtendMiner self && other.block instanceof ExtendMiner oth
                && self.chainable(oth) && oth.chainable(self);
          }},
          new DrawBlock(){
            @Override
            public void draw(Building build){
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
