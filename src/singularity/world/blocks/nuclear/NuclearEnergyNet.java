package singularity.world.blocks.nuclear;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import singularity.world.FinderContainerBase;
import singularity.world.components.NuclearEnergyBuildComp;
import universecore.util.Empties;
import universecore.util.path.GenericPath;
import universecore.util.path.IPath;

public class NuclearEnergyNet extends FinderContainerBase<NuclearEnergyBuildComp>{
  public static final GenericPath<NuclearEnergyBuildComp> EMP_PATH = new GenericPath<>();
  public ObjectMap<NuclearEnergyBuildComp, ObjectMap<NuclearEnergyBuildComp, IPath<NuclearEnergyBuildComp>>> paths = new ObjectMap<>();

  public Seq<NuclearEnergyBuildComp> all = new Seq<>();
  public Seq<NuclearEnergyBuildComp> sources = new Seq<>();
  public Seq<NuclearEnergyBuildComp> consumer = new Seq<>();
  
  private long lastFrameUpdated;
  private final Queue<Runnable> findTask = new Queue<>();

  public void addNet(NuclearEnergyNet net){
    if(net != this) for(NuclearEnergyBuildComp entity: net.all){
      add(entity);
    }
    
    onStructModified(Empties.nilSetO());
  }
  
  public void add(NuclearEnergyBuildComp entity){
    if(entity.energy().energyNet == this) return;

    if(entity.getNuclearBlock().consumeEnergy()) consumer.add(entity);
    if(entity.getNuclearBlock().outputEnergy()) sources.add(entity);

    all.add(entity);
    entity.energy().setNet(this);
    onStructModified(Empties.nilSetO());
  }
  
  /**进行一次bfs搜索构成网络的所有成员*/
  public void flow(NuclearEnergyBuildComp origin){
    excluded.clear();
    super.flow(origin);
    onStructModified(Empties.nilSetO());
  }

  public void flow(NuclearEnergyBuildComp origin, ObjectSet<NuclearEnergyBuildComp> excl){
    excluded.clear();
    excluded.addAll(excl);
    super.flow(origin);
    onStructModified(excluded);
  }
  
  public void onStructModified(ObjectSet<NuclearEnergyBuildComp> exclude){
    findTask.clear();
    paths.clear();
    
    for(NuclearEnergyBuildComp source: sources){
      findTask.addFirst(() -> {
        paths.put(source, calculatePath(source, exclude));
      });
    }
  }
  
  public void update(){
    if(Core.graphics.getFrameId() == lastFrameUpdated) return;
    lastFrameUpdated = Core.graphics.getFrameId();
    
    if(!findTask.isEmpty()){
      findTask.removeLast().run();
    }
  }
  
  public IPath<NuclearEnergyBuildComp> getPath(NuclearEnergyBuildComp source, NuclearEnergyBuildComp dest){
    return paths.get(source, ObjectMap::new).get(dest, EMP_PATH);
  }
  
  public void remove(NuclearEnergyBuildComp removed){
    for(NuclearEnergyBuildComp other: removed.energyLinked()){
      if(other.energy().energyNet != this) continue;
      
      other.energy().setNet();
      excluded.clear();
      excluded.add(removed);
      other.energy().energyNet.flow(other, excluded);
    }
  }
  
  /**计算从一个源发出到所有消耗者*/
  protected ObjectMap<NuclearEnergyBuildComp, IPath<NuclearEnergyBuildComp>> calculatePath(NuclearEnergyBuildComp source, ObjectSet<NuclearEnergyBuildComp> excl){
    ObjectMap<NuclearEnergyBuildComp, IPath<NuclearEnergyBuildComp>> result = new ObjectMap<>();

    excluded.clear();
    excluded.addAll(excl);
    findPath(source, result::put);

    return result;
  }

  @Override
  public Iterable<NuclearEnergyBuildComp> getLinkVertices(NuclearEnergyBuildComp nuclearEnergyBuildComp){
    return nuclearEnergyBuildComp.energyLinked();
  }

  @Override
  public boolean isDestination(NuclearEnergyBuildComp nuclearEnergyBuildComp, NuclearEnergyBuildComp vert1){
    return consumer.contains(nuclearEnergyBuildComp) && nuclearEnergyBuildComp.acceptEnergy(vert1);
  }
}
