package singularity.world.blocks.drills;

import arc.graphics.g2d.Draw;
import singularity.graphic.SglDraw;
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

  public class MatrixMinerEdgeBuild extends MatrixEdgeBuild{
    public void drawLink(){
      float l;
      Draw.z((l = Draw.z()) + 5f);
      if(nextEdge() != null){
        SglDraw.drawLink(
            tile, linkOffset,
            nextEdge().tile(), nextEdge().getEdgeBlock().linkOffset(),
            linkRegion, linkCapRegion,
            linkLerp()
        );
      }
      Draw.z(l);
    }
  }
}
