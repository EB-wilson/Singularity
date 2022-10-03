package singularity.ui.tables;

import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.MatrixGrid;

public class DistContainerMonitor extends Monitor{
  public DistContainerMonitor(int maxCount){

  }

  @Override
  public void startMonit(DistributeNetwork distNetwork){
    distNetwork.grids.forEach(MatrixGrid::startStatContainer);
  }

  @Override
  public void endMonit(DistributeNetwork distNetwork){
    distNetwork.grids.forEach(MatrixGrid::endStatContainer);
  }
}
