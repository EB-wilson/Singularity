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
  public static Block matrix_core,
      matrix_bridge,
      matrix_controller,
      matrix_gridNode;
  
  @Override
  public void load(){
    Sgl.ioPoint = new IOPointBlock("ioPoint");
    
    matrix_core = new DistNetCore("matrix_core"){{
      requirements(Category.distribution, ItemStack.with());
      size = 5;
    }};
    
    matrix_bridge = new MatrixBridge("matrix_bridge"){{
      requirements(Category.distribution, ItemStack.with());
      size = 2;
    }};
    
    matrix_controller = new MatrixGridCore("matrix_controller"){{
      requirements(Category.distribution, ItemStack.with());
      size = 4;
    }};
    
    matrix_gridNode = new MatrixEdgeBlock("matrix_gridNode"){{
      requirements(Category.distribution, ItemStack.with());
      size = 2;
    }};
  }
}
