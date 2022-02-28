package singularity.contents;

import mindustry.ctype.ContentList;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import singularity.Sgl;
import singularity.world.blocks.distribute.DistNetCore;
import singularity.world.blocks.distribute.IOPointBlock;
import singularity.world.blocks.distribute.MatrixBridge;
import singularity.world.blocks.distribute.matrixGrid.MatrixEdgeBlock;
import singularity.world.blocks.distribute.matrixGrid.MatrixGridCore;

public class DistributeBlocks implements ContentList{
  public static Block matrixCore,
      matrixBridge,
      matrixController,
      matrixGridNode;
  
  @Override
  public void load(){
    Sgl.ioPoint = new IOPointBlock("ioPoint");
    
    matrixCore = new DistNetCore("matrixCore"){{
      requirements(Category.distribution, ItemStack.with());
      size = 5;
    }};
    
    matrixBridge = new MatrixBridge("matrixBridge"){{
      requirements(Category.distribution, ItemStack.with());
      size = 2;
    }};
    
    matrixController = new MatrixGridCore("matrixController"){{
      requirements(Category.distribution, ItemStack.with());
      size = 4;
    }};
    
    matrixGridNode = new MatrixEdgeBlock("matrixGridNode"){{
      requirements(Category.distribution, ItemStack.with());
      size = 2;
    }};
  }
}
