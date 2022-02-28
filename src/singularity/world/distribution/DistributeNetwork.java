package singularity.world.distribution;

import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import singularity.world.blockComp.distributeNetwork.DistComponent;
import singularity.world.blockComp.distributeNetwork.DistElementBuildComp;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;
import singularity.world.blockComp.distributeNetwork.DistNetworkCoreComp;
import singularity.world.modules.DistCoreModule;

import java.util.PriorityQueue;

public class DistributeNetwork{
  private static final Queue<DistElementBuildComp> finder = new Queue<>();
  private static final ObjectSet<DistElementBuildComp> added = new ObjectSet<>();
  private static final Seq<DistElementBuildComp> tmp = new Seq<>();
  
  public PriorityQueue<DistElementBuildComp> elements = new PriorityQueue<>((a, b) -> a.priority() - b.priority());
  public PriorityQueue<MatrixGrid> grids = new PriorityQueue<>((a, b) -> a.priority - b.priority);
  
  public Seq<DistNetworkCoreComp> cores = new Seq<>();
  public Seq<DistComponent> components = new Seq<>();
  public int frequencyUsed;
  public int maxFrequency;
  
  public void add(DistributeNetwork other){
    if(other != this) for(DistElementBuildComp next: other.elements){
      add(next);
    }
  }
  
  public void add(DistElementBuildComp other){
    elements.add(other);
    if(other.getBlock() instanceof DistComponent){
      maxFrequency += other.getBlock(DistComponent.class).frequencyOffer();
      components.add((DistComponent) other.getBlock());
    }
    if(other instanceof DistNetworkCoreComp) cores.add((DistNetworkCoreComp) other);
    if(other instanceof DistMatrixUnitBuildComp) grids.add(((DistMatrixUnitBuildComp) other).matrixGrid());
    
    other.distributor().network = this;
    modified();
  }
  
  public DistNetworkCoreComp getCore(){
    return cores.size == 1? cores.get(0): null;
  }
  
  public boolean netValid(){
    return getCore() != null && frequencyUsed < maxFrequency;
  }
  
  public void update(){
    frequencyUsed = 0;
    for(DistElementBuildComp element: elements){
      frequencyUsed += element.frequencyUse();
    }
  }
  
  public void modified(){
    if(netValid()){
      DistCoreModule core = cores.get(0).distributor();
  
      for(DistBuffers<?> buffers: DistBuffers.all){
        core.getBuffer(buffers).capacity = 0;
      }
      
      core.calculatePower = 0;
      for(DistComponent distComponent: components){
        core.calculatePower += distComponent.computingPower();
  
        for(DistBuffers<?> buffers: DistBuffers.all){
          core.getBuffer(buffers).capacity += distComponent.bufferSize().get(buffers, 0);
        }
      }
    }
  }
  
  public void restruct(DistElementBuildComp origin, Seq<DistElementBuildComp> exclude){
    finder.clear();
    added.clear();
    
    finder.addFirst(origin);
    
    DistElementBuildComp other;
    while(!finder.isEmpty()){
      if(added.add(other = finder.removeLast())){
        add(other);
        
        for(DistElementBuildComp next: other.netLinked()){
          if(!exclude.contains(next)) finder.addFirst(next);
        }
      }
    }
    
    modified();
  }
  
  public void flow(DistElementBuildComp origin){
    elements.clear();
    grids.clear();
    cores.clear();
    components.clear();
    frequencyUsed = maxFrequency = 0;
    
    restruct(origin, new Seq<>());
  }
  
  public void remove(DistElementBuildComp remove){
    maxFrequency = remove.getBlock() instanceof DistElementBuildComp? remove.getBlock(DistComponent.class).frequencyOffer(): 0;
    
    for(DistElementBuildComp other: remove.netLinked()){
      if(other.distributor().network != this) continue;
      
      other.distributor().setNet();
      tmp.clear();
      other.distributor().network.restruct(other, tmp.and(remove));
    }
  }
  
  public void priorityModified(DistElementBuildComp target){
    if(elements.remove(target)) elements.add(target);
    if(target instanceof DistMatrixUnitBuildComp && grids.remove(((DistMatrixUnitBuildComp) target).matrixGrid())) grids.add(((DistMatrixUnitBuildComp) target).matrixGrid());
  }
}
