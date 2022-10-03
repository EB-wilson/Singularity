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
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.Singularity;
import singularity.graphic.SglDraw;
import singularity.world.blocks.drills.ExtendMiner;
import singularity.world.blocks.drills.ExtendableDrill;
import singularity.world.blocks.drills.MatrixMiner;
import singularity.world.blocks.drills.MatrixMinerEdge;
import singularity.world.blocks.product.SglAttributeCrafter;
import singularity.world.draw.DrawBottom;
import singularity.world.draw.DrawExpandPlasma;
import singularity.world.meta.SglAttribute;
import universecore.world.consumers.BaseConsumers;

import static mindustry.type.ItemStack.with;

public class CollectBlocks implements ContentList {
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

      setAttrBooster(SglAttribute.bitumen, 1.12f);
      
      newConsume();
      consume.liquid(Liquids.water, 0.3f);
      consume.power(1.75f);
      newProduce();
      produce.liquid(SglLiquids.rock_bitumen, 0.2f);
  
      newOptionalConsume((NormalCrafterBuild e, BaseConsumers c) -> {
        e.setVar(2.0f);
      }, (s, c) -> {
        s.add(Stat.boostEffect, 2.0f, StatUnit.timesSpeed);
      });
      consume.time(300);
      consume.item(SglItems.dry_ice, 1);
      newOptionalConsume((NormalCrafterBuild e, BaseConsumers c) -> {
        e.setVar(1.6f);
      }, (s, c) -> {
        s.add(Stat.boostEffect, 1.6f, StatUnit.timesSpeed);
      });
      consume.liquid(Liquids.cryofluid, 0.2f);


      buildType = () -> new SglAttributeCrafterBuild(){
        @Override
        public float efficiency(){
          return super.efficiency()*getVar(Float.class, 1f);
        }
  
        @Override
        public void updateTile(){
          super.updateTile();
          setVar(1);
        }
      };

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
      requirements(Category.production, ItemStack.with());
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
      newBooster(3.6f);
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
      requirements(Category.production, ItemStack.with());
      size = 2;

      master = (ExtendableDrill) tidal_drill;
    }};

    matrix_miner = new MatrixMiner("matrix_miner"){{
      requirements(Category.production, ItemStack.with());
      size = 5;
      linkOffset = 10.75f;
    }};

    matrix_miner_node = new MatrixMinerEdge("matrix_miner_node"){
      {
        requirements(Category.production, ItemStack.with());
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
