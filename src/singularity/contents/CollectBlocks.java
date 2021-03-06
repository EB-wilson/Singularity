package singularity.contents;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.ContentList;
import mindustry.graphics.Drawf;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.Singularity;
import singularity.world.blocks.drills.BaseDrill;
import singularity.world.blocks.drills.MatrixMiner;
import singularity.world.blocks.drills.MatrixMinerEdge;
import singularity.world.blocks.product.SglAttributeCrafter;
import singularity.world.draw.DrawFactory;
import singularity.world.meta.SglAttribute;

import static mindustry.type.ItemStack.with;

public class CollectBlocks implements ContentList {
  /**速旋钻头*/
  public static Block fast_spin_drill,
  /**岩层钻井机*/
  rock_drill,
  /**矩阵矿床*/
  matrix_miner,
  /**矿床框架*/
  matrix_miner_node;

  @Override
  public void load() {
    fast_spin_drill = new BaseDrill("fast_spin_drill"){{
      requirements(Category.production, with(SglItems.strengthening_alloy, 60, SglItems.aerogel, 70, Items.silicon, 90, Items.graphite, 60));
      
      bitHardness = 5;
      drillTime = 300;
      rotatorSpeed = 3.8f;
      size = 4;
  
      updateEffect = Fx.pulverizeRed;
      drillEffect = Fx.mineHuge;
      
      newConsume();
      consume.power(1.8f);
      newBooster(2.1f);
      consume.time(240f);
      consume.item(SglItems.dry_ice, 1);
      newBooster(2.0f);
      consume.liquid(Liquids.cryofluid, 0.08f);
      newBooster(1.75f);
      consume.liquid(Liquids.water, 0.1f);
    }};
    
    rock_drill = new SglAttributeCrafter("rock_drill"){{
      requirements(Category.production, with(Items.titanium, 45, Items.lead, 30, Items.copper, 30));
      size = 2;
      liquidCapacity = 24;
      oneOfOptionCons = true;
      health = 180;
      
      setAttrBooster(SglAttribute.bitumen, 1.12f);
      
      newConsume();
      consume.liquid(Liquids.water, 0.3f);
      consume.power(1.75f);
      newProduce();
      produce.liquid(SglLiquids.rock_bitumen, 0.2f);
  
      newOptionalConsume((e, c) -> {
        e.consData(2.0f);
      }, (s, c) -> {
        s.add(Stat.boostEffect, 2.0f, StatUnit.timesSpeed);
      });
      consume.time(300);
      consume.item(SglItems.dry_ice, 1);
      newOptionalConsume((e, c) -> {
        e.consData(1.6f);
      }, (s, c) -> {
        s.add(Stat.boostEffect, 1.6f, StatUnit.timesSpeed);
      });
      consume.liquid(Liquids.cryofluid, 0.2f);
      
      buildType = () -> new SglAttributeCrafterBuild(){
        @Override
        public float efficiency(){
          return super.efficiency()*consData(Float.class, 1f);
        }
  
        @Override
        public void updateTile(){
          super.updateTile();
          consData(1);
        }
      };
      
      draw = new DrawFactory<NormalCrafterBuild>(this){{
        iconRotator = true;
        drawDef = e -> {
          Color color = Liquids.water.color;
          float alpha = e.liquids.get(Liquids.water)/e.block.liquidCapacity;
          
          Draw.rect(bottom, e.x, e.y);
          Draw.rect(region, e.x, e.y);
          Drawf.spinSprite(rotator, e.x, e.y, e.totalProgress()*1.5f);
          Draw.color(color);
          Draw.alpha(alpha);
          Draw.rect(liquid, e.x, e.y);
          Draw.color();
          Draw.rect(top, e.x, e.y);
        };
      }};
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
