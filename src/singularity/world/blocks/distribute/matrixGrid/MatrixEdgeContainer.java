package singularity.world.blocks.distribute.matrixGrid;

import arc.math.geom.Polygon;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.world.Tile;
import universeCore.util.Empties;

public class MatrixEdgeContainer{
  private static final ObjectSet<MatrixGridEdge> flowed = new ObjectSet<>();
  
  public Seq<MatrixGridEdge> all = new Seq<>();
  
  private Polygon poly;
  private boolean closure;
  
  public boolean inLerp(Tile tile){
    return closure && poly != null && poly.contains(tile.drawx(), tile.drawy());
  }
  
  public void add(MatrixGridEdge other){
    all.add(other);
    other.setEdges(this);
  }
  
  public void flow(MatrixGridEdge source){
    flow(source, Empties.nilSeq());
  }
  
  public void flow(MatrixGridEdge source, Seq<MatrixGridEdge> exclude){
    flowed.clear();
    MatrixGridEdge curr = source;
    Seq<MatrixGridEdge> temp = new Seq<>();
    while(curr != null && flowed.add(curr) && !exclude.contains(curr)){
      temp.add(curr);
      curr = curr.perEdge();
    }
    for(int i = temp.size - 1; i >= 0; i--){
      add(temp.get(i));
    }
    if(curr != null && !exclude.contains(curr)){
      closure = true;
      updatePoly();
      return;
    }
    
    curr = source;
    while(curr != null && flowed.add(curr) && !exclude.contains(curr)){
      add(curr);
      curr = curr.nextEdge();
    }
    poly = null;
    closure = false;
  }
  
  public boolean isClosure(){
    return closure;
  }
  
  public Polygon getPoly(){
    return poly;
  }
  
  private void updatePoly(){
    float[] vertexArr = new float[all.size*2];
    for(int i = 0; i < all.size; i++){
      vertexArr[i*2] = all.get(i).getBuilding().x();
      vertexArr[i*2 + 1] = all.get(i).getBuilding().y();
    }
    poly = new Polygon(vertexArr);
  }
  
  public void remove(MatrixGridEdge remove){
    if(remove.nextEdge() != null){
      new MatrixEdgeContainer().flow(remove.nextEdge(), Seq.with(remove));
    }
    if(remove.perEdge() != null && remove.getEdges() != this){
      new MatrixEdgeContainer().flow(remove.perEdge(), Seq.with(remove));
    }
  }
}
