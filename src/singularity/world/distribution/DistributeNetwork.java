package singularity.world.distribution;

import arc.Core;
import arc.struct.ObjectSet;
import arc.struct.OrderedSet;
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
  public static final DistElementBuildComp[] EMP_ARR = new DistElementBuildComp[0];

  public TreeSeq<DistElementBuildComp> elements = new TreeSeq<>((a, b) -> b.priority() - a.priority());
  public ObjectSet<DistElementBuildComp> existed = new ObjectSet<>();
  private DistElementBuildComp[] elementsIterateArr;

  public TreeSeq<MatrixGrid> grids = new TreeSeq<>((a, b) -> b.priority - a.priority);
  
  public OrderedSet<DistNetworkCoreComp> cores = new OrderedSet<>();
  public OrderedSet<DistComponent> components = new OrderedSet<>();
  public int frequencyUsed;
  public int maxFrequency;

  private boolean structUpdated = true;
  private boolean status = false;
  private boolean lock = false;

  long frame;

  public void add(DistributeNetwork other){
    if(other != this){
      lock = true;
      for(DistElementBuildComp next: other.elements){
        add(next);
      }

      for(DistComponent component: other.components){
        add(component);
      }
      lock = false;
      modified();
    }
  }

  public void add(DistComponent comp){
    if(comp instanceof DistElementBuildComp ele && (ele.distributor().network == this || !existed.add(ele))) return;

    components.add(comp);
    modified();
  }
  
  public void add(DistElementBuildComp other){
    if(other.distributor().network == this || !existed.add(other)) return;

    elements.add(other);
    if(other instanceof DistComponent c) components.add(c);
    if(other instanceof DistNetworkCoreComp d) cores.add(d);
    if(other instanceof DistMatrixUnitBuildComp mat) grids.add(mat.matrixGrid());
    
    other.distributor().setNet(this);
    modified();
  }

  public float netEfficiency(){
    return netValid()? getCore().netEff(): 0;
  }
  
  public DistNetworkCoreComp getCore(){
    return cores.size == 1? cores.first(): null;
  }

  public boolean netValid(){
    return netStructValid() && getCore().netEff() > 0.001f;
  }

  public boolean netStructValid(){
    DistNetworkCoreComp core = getCore();
    boolean res = core != null && frequencyUsed < maxFrequency;
    if(!res) status = false;
    return res;
  }
  
  public void update(){
    if(frame == Core.graphics.getFrameId()) return;
    frame = Core.graphics.getFrameId();

    if(!status && netStructValid()){
      status = true;
      for(DistElementBuildComp element: elementsIterateArr){
        element.networkValided();
      }
    }

    if(structUpdated){
      for(DistElementBuildComp element: elementsIterateArr){
        element.networkUpdated();
      }
      structUpdated = false;
    }

    frequencyUsed = 0;
    maxFrequency = 0;

    for(DistElementBuildComp element: elementsIterateArr){
      frequencyUsed += element.frequencyUse();
    }

    for(DistComponent distComponent: components){
      if(!distComponent.componentValid()) continue;

      maxFrequency += distComponent.frequencyOffer();
    }

    if(netStructValid()){
      DistCoreModule core = cores.first().distCore();

      for(DistBuffers<?> buffers: DistBuffers.all){
        core.getBuffer(buffers).capacity = 0;
      }

      core.calculatePower = 0;

      for(DistComponent distComponent: components){
        if(!distComponent.componentValid()) continue;

        core.calculatePower += distComponent.computingPower();

        for(DistBuffers<?> buffers: DistBuffers.all){
          core.getBuffer(buffers).capacity += distComponent.bufferSize().get(buffers, 0);
        }
      }
    }
  }

  public void modified(){
    if(lock) return;

    elementsIterateArr = elements.toArray(EMP_ARR);
    structUpdated = true;
  }
  
  public void restruct(DistElementBuildComp origin, Seq<DistElementBuildComp> exclude){
    finder.clear();
    added.clear();
    
    finder.addFirst(origin);

    lock = true;
    
    DistElementBuildComp other;
    while(!finder.isEmpty()){
      if(added.add(other = finder.removeLast())){
        add(other);
        
        for(DistElementBuildComp next: other.netLinked()){
          if(!exclude.contains(next)) finder.addFirst(next);
        }
      }
    }

    lock = false;
    modified();
  }
  
  public void flow(DistElementBuildComp origin){
    elements.clear();
    grids.clear();
    cores.clear();
    components.clear();
    
    restruct(origin, new Seq<>());
  }
  
  public void remove(DistElementBuildComp remove){
    for(DistElementBuildComp element: elements){
      element.networkRemoved(remove);
    }

    for(DistElementBuildComp other: remove.netLinked()){
      if(other.distributor().network != this) continue;
      
      other.distributor().setNet();
      tmp.clear();
      other.distributor().network.restruct(other, tmp.add(remove));
    }

    modified();
  }
  
  public void priorityModified(DistElementBuildComp target){
    if(elements.remove(target)) elements.add(target);
    if(target instanceof DistMatrixUnitBuildComp && grids.remove(((DistMatrixUnitBuildComp) target).matrixGrid())) grids.add(((DistMatrixUnitBuildComp) target).matrixGrid());
  }
}
