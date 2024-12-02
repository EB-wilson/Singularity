package singularity.world.blocks;

import arc.util.Log;
import arc.util.pooling.Pools;
import singularity.ui.fragments.notification.Notification;
import singularity.world.blocks.distribute.TargetConfigure;
import singularity.world.blocks.distribute.matrixGrid.MatrixGridBlock.MatrixGridBuild.PosCfgPair;
import singularity.world.blocks.distribute.matrixGrid.MatrixGridCore.MatrixGridCoreBuild.LinkPair;
import universecore.util.DataPackable;

public class BytePackAssign {
  public static void assignAll() {
    try{
      DataPackable.assignType(TargetConfigure.typeID, param -> new TargetConfigure());
      DataPackable.assignType(LinkPair.typeID, param -> Pools.obtain(LinkPair.class, LinkPair::new));
      DataPackable.assignType(PosCfgPair.typeID, param -> Pools.obtain(PosCfgPair.class, PosCfgPair::new));

      Notification.Note.assign();
      Notification.Warning.assign();
      Notification.ResearchCompleted.assign();
      Notification.Inspired.assign();
    }catch(Throwable e){
      Log.err("some error happened, may fatal, details: ");
      Log.err(e);
    }
  }
}
