package singularity.content;

import singularity.type.SglCategory;
import singularity.world.blocks.debug.BlockDataMonitor;
import singularity.world.blocks.debug.VarsContainer;
import mindustry.ctype.ContentList;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;

public class DebugBlocks implements ContentList{
  public static Block blockMonitor, varsContainer;
  
  @Override
  public void load(){
    blockMonitor = new BlockDataMonitor("block_monitor"){{
      requirements(SglCategory.debugging, ItemStack.empty);
      buildVisibility = BuildVisibility.sandboxOnly;
    }};
    
    varsContainer = new VarsContainer("vars_container"){{
      requirements(SglCategory.debugging, ItemStack.empty);
      buildVisibility = BuildVisibility.sandboxOnly;
    }};
  }
}
