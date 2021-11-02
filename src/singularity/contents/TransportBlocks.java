package singularity.contents;

import singularity.world.blocks.liquid.LiquidUnloader;
import mindustry.content.Items;
import mindustry.ctype.ContentList;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;

public class TransportBlocks implements ContentList {
  /**液体提取器*/
  public static Block liquid_unloader;
  
  @Override
  public void load() {
    liquid_unloader = new LiquidUnloader("liquid_unloader"){{
      requirements(Category.liquid, ItemStack.with(Items.metaglass, 60, SglItems.aerogel, 50, Items.silicon, 45));
      size = 1;
    }};
  }
}
