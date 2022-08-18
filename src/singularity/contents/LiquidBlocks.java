package singularity.contents;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import singularity.world.blocks.liquid.ClusterConduit;
import singularity.world.blocks.liquid.ConduitRiveting;
import singularity.world.blocks.liquid.LiquidUnloader;

public class LiquidBlocks implements ContentList {
  /**集束管道*/
  public static Block cluster_conduit,
  /**管道铆*/
  conduit_riveting,
  /**液体提取器*/
  liquid_unloader;
  
  @Override
  public void load() {
    cluster_conduit = new ClusterConduit("cluster_conduit"){{
      requirements(Category.liquid, ItemStack.with(SglItems.strengthening_alloy, 18, SglItems.aerogel, 15, Items.plastanium, 10));
      liquidCapacity = 10;
      liquidPressure = 1.05f;
      health = 360;
    }};
    
    conduit_riveting = new ConduitRiveting("conduit_riveting"){{
      requirements(Category.liquid, ItemStack.with(SglItems.strengthening_alloy, 20, Items.metaglass, 20, Items.plastanium, 15));
      liquidCapacity = 10;
      health = 300;
    }};
    
    liquid_unloader = new LiquidUnloader("liquid_unloader"){{
      requirements(Category.liquid, ItemStack.with(Items.metaglass, 60, SglItems.aerogel, 50, Items.silicon, 45));
      size = 1;
    }};
  }
}
