package singularity.contents;

import mindustry.ctype.ContentList;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import singularity.world.blocks.distribute.MatrixBridge;

public class DistributeBlocks implements ContentList{
  public static Block matrixBridge;
  
  @Override
  public void load(){
    matrixBridge = new MatrixBridge("matrixBridge"){{
      requirements(Category.distribution, ItemStack.with());
    }};
  }
}
