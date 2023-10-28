package singularity.world.distribution;

import arc.Core;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.OrderedSet;
import arc.util.Time;
import mindustry.gen.Building;
import singularity.world.FinderContainerBase;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import universecore.util.Empties;
import universecore.util.colletion.TreeSeq;

import java.util.Iterator;

public class DistributeNetwork extends FinderContainerBase<DistElementBuildComp> implements Iterable<DistElementBuildComp>{
  public static final OrderedSet<DistributeNetwork> activityNetwork = new OrderedSet<>();

  private static final ObjectSet<DistElementBuildComp> tmp = new ObjectSet<>();
  public static final DistElementBuildComp[] EMP_ARR = new DistElementBuildComp[0];

  public OrderedSet<DistElementBuildComp> allElem = new OrderedSet<>();
  public TreeSeq<DistElementBuildComp> elements = new TreeSeq<>((a, b) -> b instanceof DistNetworkCoreComp? 1: b.priority() - a.priority());
  public OrderedSet<DistElementBuildComp> energyBuffer = new OrderedSet<>();
  private DistElementBuildComp[] elementsIterateArr;

  public TreeSeq<MatrixGrid> grids = new TreeSeq<>((a, b) -> b.priority - a.priority);
  
  public OrderedSet<DistNetworkCoreComp> cores = new OrderedSet<>();

  public ObjectMap<String, Object> vars = new ObjectMap<>();

  public int topologyUsed;
  public int totalTopologyCapacity;

  public float energyProduct;
  public float energyConsume;
  public float energyBuffered;
  public float energyCapacity;
  public float energyStatus;

  private boolean structUpdated = true;
  private boolean status = false;
  private boolean lock = false;
  private boolean handlingStat = false;

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

      lock = false;

      vars.putAll(other.vars);
      modified();
    }
  }
  
  public void add(DistElementBuildComp other){
    if(other == null || other.distributor().network == this) return;

    elements.add(other);
    allElem.add(other);
    if(other.getDistBlock().matrixEnergyCapacity() > 0) energyBuffer.add(other);
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
    boolean res = core != null && topologyUsed <= totalTopologyCapacity;
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
      activityNetwork.add(this);
    }

    if(structUpdated){
      for(DistElementBuildComp element: elementsIterateArr){
        element.networkUpdated();
      }
      structUpdated = false;
    }

    handlingStat = true;
    totalTopologyCapacity = 0;

    DistNetworkCoreComp core = getCore();
    if (core != null) {
      for (DistBufferType<?> buffers : DistBufferType.all) {
        core.getBuffer(buffers).capacity = 0;
      }

      core.distCore().calculatePower = 0;
    }

    for (DistElementBuildComp element : elements) {
      element.updateNetStat();
    }
    handlingStat = false;

    updateEnergy();

    topologyUsed = 0;

    for(DistElementBuildComp element: elementsIterateArr){
      topologyUsed += element.frequencyUse();
    }
  }

  public void handleTopologyCapacity(int count){
    if (!handlingStat) return;
    totalTopologyCapacity += count;
  }

  public void handleBufferCapacity(DistBufferType<?> type, int count){
    if (!handlingStat) return;

    DistNetworkCoreComp core = getCore();
    if (core == null) return;

    core.getBuffer(type).capacity += count;
  }

  public void handleCalculatePower(int count){
    if (!handlingStat) return;

    DistNetworkCoreComp core = getCore();
    if (core == null) return;

    core.distCore().calculatePower += count;
  }

  public void updateEnergy(){
    energyCapacity = energyBuffered = energyStatus = energyProduct = energyConsume = 0;

    if(netStructValid()){
      for(DistElementBuildComp element: elementsIterateArr){
        energyConsume += element.matrixEnergyConsume()*(element instanceof Building b? b.delta(): Time.delta);
      }
      for(DistElementBuildComp element: elementsIterateArr){
        energyProduct += element.matrixEnergyProduct()*(element instanceof Building b? b.delta(): Time.delta);
      }
      for (DistElementBuildComp buff : energyBuffer) {
        energyBuffered += buff.matrixEnergyBuffered();
        energyCapacity += buff.getDistBlock().matrixEnergyCapacity();
      }

      float delta = energyBuffer.isEmpty()? 0: Math.min(energyCapacity - energyBuffered, energyProduct - energyConsume) / energyBuffer.size;
      int counter = 0;
      for (DistElementBuildComp buff : energyBuffer) {
        counter++;
        float origin = buff.matrixEnergyBuffered();
        float set = Mathf.clamp(origin + delta, 0, buff.getDistBlock().matrixEnergyCapacity());
        buff.matrixEnergyBuffered(set);

        energyProduct -= set - origin;
        delta += (delta - (set - origin)) / (energyBuffer.size - counter);
      }

      energyStatus = Mathf.clamp(energyProduct/energyConsume, 0, 1);
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
    activityNetwork.remove(this);
    elements.clear();
    allElem.clear();
    grids.clear();
    cores.clear();

    restruct(origin, excl);
  }

  private void restruct(DistElementBuildComp origin, ObjectSet<DistElementBuildComp> excl){
    activityNetwork.remove(this);
    excluded.clear();
    excluded.addAll(excl);
    lock = true;
    super.flow(origin);
    lock = false;

    modified();
  }

  public void remove(DistElementBuildComp remove){
    activityNetwork.remove(this);

    for(DistElementBuildComp element: elementsIterateArr){
      element.networkRemoved(remove);
    }

    tmp.clear();
    tmp.add(remove);
    for(DistElementBuildComp other: elementsIterateArr){
      if(other.distributor().network != this) continue;
      
      new DistributeNetwork().flow(other, tmp);
    }

    modified();
  }
  
  public void priorityModified(DistElementBuildComp target){
    if(allElem.contains(target) && elements.remove(target)){
      elements.add(target);
      elementsIterateArr = elements.toArray(EMP_ARR);
    }
    if(target instanceof DistMatrixUnitBuildComp un && grids.remove(un.matrixGrid())) grids.add(un.matrixGrid());
  }

  private final Itr INST_ITR = new Itr();

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
        for(BaseBuffer<?, ?, ?> value: getCore().buffers().values()){
          if(value.space() <= 0) return NetStatus.bufferBlocked;
        }
        for(DistRequestBase task: getCore().distCore().requestTasks){
          if(task.isBlocked()) return NetStatus.requestBlocked;
        }
        return NetStatus.ordinary;
      }
      return NetStatus.energyLeak;
    }
    else if(topologyUsed > totalTopologyCapacity) return NetStatus.topologyLeak;

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
