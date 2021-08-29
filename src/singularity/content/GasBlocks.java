package singularity.content;

import mindustry.content.Items;
import mindustry.ctype.ContentList;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;
import singularity.type.SglCategory;
import singularity.world.blocks.gas.GasConduit;
import singularity.world.blocks.gas.GasSource;
import singularity.world.blocks.gas.GasVoid;

public class GasBlocks implements ContentList{
  public static Block gasConduit,
  gasSource,
  gasVoid;
  
  @Override
  public void load(){
    gasConduit = new GasConduit("gas_conduit"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 100));
    }};
    
    gasSource = new GasSource("gas_source"){{
      requirements(SglCategory.gases, ItemStack.empty);
    }};
    
    gasVoid = new GasVoid("gas_void"){{
      requirements(SglCategory.gases, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
  }
}
