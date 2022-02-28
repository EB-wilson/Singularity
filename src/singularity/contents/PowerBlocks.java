package singularity.contents;

import mindustry.content.Items;
import mindustry.ctype.ContentList;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import singularity.world.blocks.product.NormalCrafter;

public class PowerBlocks implements ContentList{
  public static Block fluidic_generator;
  
  @Override
  public void load(){
    fluidic_generator = new NormalCrafter("fluidic_generator"){{
      requirements(Category.power, ItemStack.with(Items.titanium, 100, Items.surgeAlloy, 80, Items.silicon, 95, SglItems.strengthening_alloy, 80, Items.metaglass, 140));
      size = 4;
    }};
  }
}
