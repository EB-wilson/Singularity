package singularity.world.blocks.drills;

import singularity.world.blocks.distribute.matrixGrid.MatrixEdgeBlock;
import singularity.world.components.EdgeLinkerComp;

public class MatrixMinerEdge extends MatrixEdgeBlock{
  public MatrixMinerEdge(String name){
    super(name);
    linkLength = 25;
  }

  @Override
  public boolean linkable(EdgeLinkerComp other){
    return other instanceof MatrixMinerEdge || other instanceof MatrixMiner;
  }
}
