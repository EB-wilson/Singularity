package singularity.content.override;

import singularity.world.draw.SglDrawSmelter;
import singularity.world.blocks.product.NormalCrafter;
import arc.graphics.Color;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import universeCore.util.OverrideContentList;

public class OverrideBlocks implements OverrideContentList{
  @Override
  public void load(){
    doOverrideContent(Blocks.siliconSmelter,
      new NormalCrafter("silicon-smelter_override"){{
      requirements(Category.crafting, ItemStack.with(Items.copper, 30,Items.lead, 25));
      craftEffect = Fx.smeltsmoke;
      size = 2;
      hasPower = true;
      hasLiquids = false;
      
      drawer = new SglDrawSmelter(Color.valueOf("ffef99"));
      
      newConsume();
      consume.items(ItemStack.with(Items.coal, 1, Items.sand, 2));
      consume.power(0.50f);
      consume.time(40f);
      
      newProduce();
      produce.item(Items.silicon, 2);
    }});
  }
}
