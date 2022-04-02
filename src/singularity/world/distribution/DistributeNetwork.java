package singularity.world.distribution;

import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import singularity.world.components.distnet.DistComponent;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.modules.DistCoreModule;
import universecore.util.colletion.TreeSeq;

public class DistributeNetwork{
  private static final Queue<DistElementBuildComp> finder = new Queue<>();
  private static final ObjectSet<DistElementBuildComp> added = new ObjectSet<>();
  private static final Seq<DistElementBuildComp> tmp = new Seq<>();
  
  public TreeSeq<DistElementBuildComp> elements = new TreeSeq<>((a, b) -> b.priority() - a.priority());
  public TreeSeq<MatrixGrid> grids = new TreeSeq<>((a, b) -> b.priority - a.priority);
  
  public Seq<DistNetworkCoreComp> cores = new Seq<>();
  public Seq<DistComponent> components = new Seq<>();
  public int frequencyUsed;
  public int maxFrequency;
  
  private boolean status = false;
  
  public void add(DistributeNetwork other){
    if(other != this) for(DistElementBuildComp next: other.elements){
      add(next);
    }
  }
  
  public void add(DistElementBuildComp other){
    elements.add(other);
    if(other instanceof DistComponent) components.add((DistComponent) other);
    if(other instanceof DistNetworkCoreComp) cores.add((DistNetworkCoreComp) other);
    if(other instanceof DistMatrixUnitBuildComp) grids.add(((DistMatrixUnitBuildComp) other).matrixGrid());
    
    other.distributor().setNet(this);
    modified();
  }
  
  public DistNetworkCoreComp getCore(){
    return cores.size == 1? cores.get(0): null;
  }
  
  public boolean netValid(){
    boolean res = getCore() != null && frequencyUsed < maxFrequency;
    if(!res) status = false;
    return res;
  }
  
  public void update(){
    if(!status && netValid()){
      status = true;
      for(DistElementBuildComp element : elements){
        element.networkValided();
      }
    }
    frequencyUsed = 0;
    for(DistElementBuildComp element: elements){
      frequencyUsed += element.frequencyUse();
    }
  }

  public void modified(){
    maxFrequency = 0;

    for(DistComponent distComponent: components){
      maxFrequency += distComponent.frequencyOffer();
    }

    if(netValid()){
      DistCoreModule core = cores.get(0).distCore();

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
    for(DistElementBuildComp other: remove.netLinked()){
      if(other.distributor().network != this) continue;
      
      other.distributor().setNet();
      tmp.clear();
      other.distributor().network.restruct(other, tmp.and(remove));
    }
    modified();
  }
  
  public void priorityModified(DistElementBuildComp target){
    if(elements.remove(target)) elements.add(target);
    if(target instanceof DistMatrixUnitBuildComp && grids.remove(((DistMatrixUnitBuildComp) target).matrixGrid())) grids.add(((DistMatrixUnitBuildComp) target).matrixGrid());
  }
}
