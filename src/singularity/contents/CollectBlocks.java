package singularity.contents;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.ContentList;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.world.blocks.drills.BaseDrill;
import singularity.world.blocks.product.SglAttributeCrafter;
import singularity.world.draw.DrawFactory;
import singularity.world.meta.SglAttribute;

import static mindustry.type.ItemStack.with;

public class CollectBlocks implements ContentList {
  /**速旋钻头*/
  public static Block fast_spin_drill,
  /**岩层钻井机*/
  rock_drill;

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
      newBooster(2.0f);
      consume.time(60f);
      consume.item(SglItems.dry_ice, 1);
      newBooster(1.9f);
      consume.liquid(Liquids.cryofluid, 0.08f);
      newBooster(1.8f);
      consume.liquid(Liquids.water, 0.1f);
    }};
    
    rock_drill = new SglAttributeCrafter("rock_drill"){{
      requirements(Category.production, with(Items.titanium, 45, Items.lead, 30, Items.copper, 30));
      size = 2;
      liquidCapacity = 24;
      oneOfOptionCons = true;
      
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
      consume.time(120);
      consume.item(SglItems.dry_ice, 1);
      newOptionalConsume((e, c) -> {
        e.consData(1.6f);
      }, (s, c) -> {
        s.add(Stat.boostEffect, 1.6f, StatUnit.timesSpeed);
      });
      consume.liquid(Liquids.cryofluid, 0.5f);
      
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
      
      draw = new DrawFactory<>(this){{
        iconRotator = true;
        drawDef = e -> {
          Color color = Liquids.water.color;
          float alpha = e.liquids.get(Liquids.water)/e.block.liquidCapacity;
          
          Draw.rect(bottom, e.x, e.y);
          Draw.rect(region, e.x, e.y);
          Draw.rect(rotator, e.x, e.y, e.totalProgress*1.5f);
          Draw.color(color);
          Draw.alpha(alpha);
          Draw.rect(liquid, e.x, e.y);
          Draw.color();
          Draw.rect(top, e.x, e.y);
        };
      }};
    }};
  }
}
