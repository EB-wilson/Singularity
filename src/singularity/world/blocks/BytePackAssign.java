package singularity.world.blocks;

import arc.util.pooling.Pools;
import singularity.world.blocks.distribute.TargetConfigure;
import singularity.world.blocks.distribute.matrixGrid.MatrixGridBlock.MatrixGridBuild.PosCfgPair;
import singularity.world.blocks.distribute.matrixGrid.MatrixGridCore.MatrixGridCoreBuild.LinkPair;
import universecore.util.DataPackable;

public class BytePackAssign {
  public static void assignAll() {
    DataPackable.assignType(TargetConfigure.typeID, p -> new TargetConfigure());
    DataPackable.assignType(LinkPair.typeID, param -> Pools.obtain(LinkPair.class, LinkPair::new));
    DataPackable.assignType(PosCfgPair.typeID, param -> Pools.obtain(PosCfgPair.class, PosCfgPair::new));
  }
}
