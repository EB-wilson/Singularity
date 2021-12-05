package singularity.world.blockComp.distributeNetwork;

import singularity.world.distribution.MatrixGrid;

import java.util.PriorityQueue;

public interface DistMatrixUnitComp extends DistElementBuildComp{
  MatrixGrid matrixGrid();
  
  PriorityQueue<MatrixGrid.BuildingEntry<?>> lerpBuilds();
}
