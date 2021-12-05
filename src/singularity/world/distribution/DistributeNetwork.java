package singularity.world.distribution;

import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import singularity.world.blockComp.distributeNetwork.DistElementBuildComp;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitComp;
import singularity.world.blockComp.distributeNetwork.DistNetworkCoreComp;

import java.util.PriorityQueue;

public class DistributeNetwork{
  private static final Queue<DistElementBuildComp> finder = new Queue<>();
  private static final ObjectSet<DistElementBuildComp> added = new ObjectSet<>();
  private static final Seq<DistElementBuildComp> tmp = new Seq<>();
  
  public PriorityQueue<DistElementBuildComp> elements = new PriorityQueue<>((a, b) -> a.priority() - b.priority());
  public PriorityQueue<MatrixGrid> grids = new PriorityQueue<>((a, b) -> a.priority - b.priority);
  
  public Seq<DistNetworkCoreComp> cores = new Seq<>();
  public int frequency;
  
  public void add(DistributeNetwork other){
    for(DistElementBuildComp next: other.elements){
      add(next);
    }
  }
  
  public void add(DistElementBuildComp other){
    elements.add(other);
    if(other instanceof DistNetworkCoreComp) cores.add((DistNetworkCoreComp) other);
    if(other instanceof DistMatrixUnitComp) grids.add(((DistMatrixUnitComp) other).matrixGrid());
    
    frequency += other.getDistBlock().frequencyAmount();
    other.distributor().network = this;
  }
  
  public boolean netValid(){
    return cores.size == 1 && frequency >= 0;
  }
  
  public void update(){
  
  }
  
  public void restruct(DistElementBuildComp origin, Seq<DistElementBuildComp> exclude){
    finder.addFirst(origin);
    added.add(origin);
    
    DistElementBuildComp other;
    while(!finder.isEmpty()){
      if(!added.contains(other = finder.removeFirst())){
        add(other);
        
        for(DistElementBuildComp next: other.getLinked()){
          if(!exclude.contains(next)) finder.addFirst(next);
        }
      }
    }
  }
  
  public void remove(DistElementBuildComp remove){
    for(DistElementBuildComp other: remove.getLinked()){
      if(other.distributor().network != this) continue;
      
      other.distributor().setNet();
      tmp.clear();
      other.distributor().network.restruct(other, tmp.and(remove));
    }
  }
}
