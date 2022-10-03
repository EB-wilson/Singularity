package singularity.world.distribution;

import arc.Core;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.OrderedSet;
import arc.util.Time;
import org.jetbrains.annotations.NotNull;
import singularity.world.FinderContainerBase;
import singularity.world.components.distnet.DistComponent;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import singularity.world.modules.DistCoreModule;
import universecore.util.Empties;
import universecore.util.colletion.TreeSeq;

import java.util.Iterator;

public class DistributeNetwork extends FinderContainerBase<DistElementBuildComp> implements Iterable<DistElementBuildComp>{
  private static final ObjectSet<DistElementBuildComp> tmp = new ObjectSet<>();
  public static final DistElementBuildComp[] EMP_ARR = new DistElementBuildComp[0];

  public TreeSeq<DistElementBuildComp> elements = new TreeSeq<>((a, b) -> b.priority() - a.priority());
  public OrderedSet<DistElementBuildComp> energyBuffer = new OrderedSet<>();
  private DistElementBuildComp[] elementsIterateArr;

  public TreeSeq<MatrixGrid> grids = new TreeSeq<>((a, b) -> b.priority - a.priority);
  
  public OrderedSet<DistNetworkCoreComp> cores = new OrderedSet<>();
  public OrderedSet<DistComponent> components = new OrderedSet<>();

  public ObjectMap<String, Object> vars = new ObjectMap<>();

  public int frequencyUsed;
  public int maxFrequency;

  public float energyProduct;
  public float energyConsume;
  public float extraEnergyRequire;
  public float energyStatus;

  private boolean structUpdated = true;
  private boolean status = false;
  private boolean lock = false;

  long frame;

  public void putVar(String key, Object value){
    vars.put(key, value);
  }

  @SuppressWarnings("unchecked")
  public <T> T getVar(String key, T def){
    return (T) vars.get(key, def);
  }

  public <T> T getVar(String key){
    return getVar(key, null);
  }

  public void add(DistributeNetwork other){
    if(other != this){
      lock = true;
      for(DistElementBuildComp next: other){
        add(next);
      }

      for(DistComponent component: other.components){
        add(component);
      }
      lock = false;

      vars.putAll(other.vars);
      modified();
    }
  }

  public void add(DistComponent comp){
    if(comp instanceof DistElementBuildComp ele && (ele.distributor().network == this)) return;

    components.add(comp);
    modified();
  }
  
  public void add(DistElementBuildComp other){
    if(other == null || other.distributor().network == this) return;

    elements.add(other);
    if(other.getDistBlock().matrixEnergyCapacity() > 0) energyBuffer.add(other);
    if(other instanceof DistComponent c) components.add(c);
    if(other instanceof DistNetworkCoreComp d) cores.add(d);
    if(other instanceof DistMatrixUnitBuildComp mat) grids.add(mat.matrixGrid());
    
    other.distributor().setNet(this);
    modified();
  }

  public float netEfficiency(){
    return netStructValid()? energyStatus: 0;
  }
  
  public DistNetworkCoreComp getCore(){
    return cores.size == 1? cores.first(): null;
  }

  public boolean netValid(){
    return netStructValid() && energyStatus > 0.001f;
  }

  public boolean netStructValid(){
    DistNetworkCoreComp core = getCore();
    boolean res = core != null && frequencyUsed <= maxFrequency;
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

    updateEnergy();

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

  public void updateEnergy(){
    energyStatus = energyProduct = energyConsume = extraEnergyRequire = 0;

    if(netStructValid()){
      for(DistElementBuildComp element: elementsIterateArr){
        energyConsume += element.matrixEnergyConsume();
        extraEnergyRequire += element.extraEnergyRequire();
      }
      for(DistElementBuildComp element: elementsIterateArr){
        energyProduct += element.matrixEnergyProduct();
      }
      energyStatus = energyConsume == 0? 1: energyProduct/energyConsume;

      if(energyBuffer.isEmpty()) return;

      float avgDelta = (energyStatus - 1)*energyConsume/energyBuffer.size*Time.delta;
      int count = 0;
      boolean anyBuffer = false;
      for(DistElementBuildComp buffer: energyBuffer){
        count++;
        float origin = buffer.matrixEnergyBuffered();
        float set = Mathf.clamp(origin + avgDelta, 0, buffer.getDistBlock().matrixEnergyCapacity());
        buffer.matrixEnergyBuffered(set);

        if(avgDelta < 0 && set - origin < 0) anyBuffer = true;
        avgDelta += (avgDelta - set + origin)/(energyBuffer.size - count);
      }

      energyStatus = Mathf.clamp(energyStatus);
      if(energyStatus < 1 && anyBuffer) energyStatus = 1;
    }
  }

  public void modified(){
    if(lock) return;

    elementsIterateArr = elements.toArray(EMP_ARR);
    structUpdated = true;
  }
  
  public void flow(DistElementBuildComp origin){
    flow(origin, Empties.nilSetO());
  }

  public void flow(DistElementBuildComp origin, ObjectSet<DistElementBuildComp> excl){
    elements.clear();
    grids.clear();
    cores.clear();
    components.clear();

    restruct(origin, excl);
  }

  private void restruct(DistElementBuildComp origin, ObjectSet<DistElementBuildComp> excl){
    excluded.clear();
    excluded.addAll(excl);
    lock = true;
    super.flow(origin);
    lock = false;

    modified();
  }

  public void remove(DistElementBuildComp remove){
    for(DistElementBuildComp element: elementsIterateArr){
      element.networkRemoved(remove);
    }

    tmp.clear();
    tmp.add(remove);
    for(DistElementBuildComp other: elementsIterateArr){
      if(other.distributor().network != this) continue;
      
      other.distributor().setNet();
      other.distributor().network.restruct(other, tmp);
    }

    modified();
  }
  
  public void priorityModified(DistElementBuildComp target){
    if(elements.remove(target)){
      elements.add(target);
      elementsIterateArr = elements.toArray(EMP_ARR);
    }
    if(target instanceof DistMatrixUnitBuildComp && grids.remove(((DistMatrixUnitBuildComp) target).matrixGrid())) grids.add(((DistMatrixUnitBuildComp) target).matrixGrid());
  }

  private final Itr INST_ITR = new Itr();

  @NotNull
  @Override
  public Iterator<DistElementBuildComp> iterator(){
    INST_ITR.cursor = 0;
    return INST_ITR;
  }

  @Override
  public Iterable<DistElementBuildComp> getLinkVertices(DistElementBuildComp distElementBuildComp){
    return distElementBuildComp.netLinked();
  }

  @Override
  public boolean isDestination(DistElementBuildComp distElementBuildComp, DistElementBuildComp vert1){
    return false;
  }

  public NetStatus status(){
    if(netStructValid()){
      if(netValid()){
        for(BaseBuffer<?, ?, ?> value: getCore().distCore().buffers.values()){
          if(value.space() <= 0) return NetStatus.bufferBlocked;
        }
        for(DistRequestBase<?> task: getCore().distCore().requestTasks){
          if(task.isBlocked()) return NetStatus.requestBlocked;
        }
        return NetStatus.ordinary;
      }
      return NetStatus.energyLeak;
    }
    else if(frequencyUsed > maxFrequency) return NetStatus.topologyLeak;

    return NetStatus.unknow;
  }

  public enum NetStatus{
    ordinary,
    energyLeak,
    bufferBlocked,
    requestBlocked,
    topologyLeak,
    unknow;

    private final String localized;

    NetStatus(){
      localized = Core.bundle.get("status." + name());
    }

    public String localized(){
      return localized;
    }
  }

  private class Itr implements Iterator<DistElementBuildComp>{
    int cursor = 0;

    @Override
    public boolean hasNext(){
      return cursor < elementsIterateArr.length;
    }

    @Override
    public DistElementBuildComp next(){
      return elementsIterateArr[cursor++];
    }
  }
}
