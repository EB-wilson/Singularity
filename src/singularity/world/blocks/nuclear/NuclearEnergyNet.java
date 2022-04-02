package singularity.world.blocks.nuclear;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import singularity.world.components.NuclearEnergyBuildComp;

public class NuclearEnergyNet{
  private static final Seq<NuclearEnergyBuildComp> empty = new Seq<>();
  private static final Seq<NuclearEnergyBuildComp> tmp = new Seq<>();
  private static final ObjectMap<NuclearEnergyBuildComp, ObjectMap<NuclearEnergyBuildComp, Seq<NuclearEnergyBuildComp>>> emptyMap = new ObjectMap<>();
  private static final ObjectSet<NuclearEnergyBuildComp> added = new ObjectSet<>();
  
  private static final Queue<NuclearEnergyBuildComp> bfsQueue = new Queue<>();
  private static final Queue<Node> pathFindQueue = new Queue<>();
  
  public ObjectMap<NuclearEnergyBuildComp, ObjectMap<NuclearEnergyBuildComp, Seq<NuclearEnergyBuildComp>>> paths = new ObjectMap<>();
  
  public Seq<NuclearEnergyBuildComp> all = new Seq<>();
  public Seq<NuclearEnergyBuildComp> sources = new Seq<>();
  public Seq<NuclearEnergyBuildComp> consumer = new Seq<>();
  
  private long lastFrameUpdated;
  private final Queue<Runnable> findTask = new Queue<>();
  
  public void addNet(NuclearEnergyNet net){
    if(net != this) for(NuclearEnergyBuildComp entity: net.all){
      add(entity);
    }
    
    onStructModified(empty);
  }
  
  public void add(NuclearEnergyBuildComp entity){
    if(entity.getNuclearBlock().hasEnergy()){
      if(entity.getNuclearBlock().consumeEnergy()) consumer.add(entity);
      if(entity.getNuclearBlock().outputEnergy()) sources.add(entity);
  
      all.add(entity);
      entity.energy().setNet(this);
      onStructModified(empty);
    }
  }
  
  /**进行一次bfs搜索构成网络的所有成员*/
  public void flow(NuclearEnergyBuildComp origin, Seq<NuclearEnergyBuildComp> exclude){
    bfsQueue.clear();
    added.clear();
    
    bfsQueue.addFirst(origin);
    added.add(origin);
    all.clear();
    while(!bfsQueue.isEmpty()){
      NuclearEnergyBuildComp other = bfsQueue.removeLast();
      for(NuclearEnergyBuildComp next: other.energyLinked()){
        boolean valid = added.add(next);
        if(!exclude.contains(next) && valid) bfsQueue.addFirst(next);
      }
      add(other);
    }
    
    onStructModified(exclude);
  }
  
  public void flow(NuclearEnergyBuildComp origin){
    flow(origin, empty);
  }
  
  public void onStructModified(Seq<NuclearEnergyBuildComp> exclude){
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
  
  public Seq<NuclearEnergyBuildComp> getPath(NuclearEnergyBuildComp source, NuclearEnergyBuildComp dest){
    return paths.get(source, new ObjectMap<>()).get(dest, new Seq<>());
  }
  
  public void remove(NuclearEnergyBuildComp removed){
    for(NuclearEnergyBuildComp other: removed.energyLinked()){
      if(other.energy().energyNet != this) continue;
      
      other.energy().setNet();
      tmp.clear();
      other.energy().energyNet.flow(other, tmp.and(removed));
    }
  }
  
  /**计算从一个源发出到所有消耗者*/
  protected ObjectMap<NuclearEnergyBuildComp, Seq<NuclearEnergyBuildComp>> calculatePath(NuclearEnergyBuildComp source, Seq<NuclearEnergyBuildComp> exclude){
    ObjectMap<NuclearEnergyBuildComp, Seq<NuclearEnergyBuildComp>> result = new ObjectMap<>();
    
    pathFindQueue.clear();
    added.clear();
    
    Node origin = new Node(source, null);
    source.energyLinked().each(e -> origin.children.add(new Node(e, origin)));
    pathFindQueue.addFirst(origin);
    added.add(source);
    
    while(!pathFindQueue.isEmpty()){
      Node current = pathFindQueue.removeLast();
      
      for(Node child: current.children){
        if(added.add(child.self)){
          child.self.energyLinked().each(e -> {
            if(!added.contains(e) && !exclude.contains(e)){
              child.children.add(new Node(e, child));
            }
          });
        }
        pathFindQueue.addFirst(child);
      }
      
      if(consumer.contains(current.self) && current.self != source){
        Node flow = current;
        Seq<NuclearEnergyBuildComp> path = new Seq<>();
        while(!flow.first){
          path.add(flow.self);
          flow = flow.parent;
        }
        path.add(flow.self);
        
        result.put(current.self, path);
      }
    }
    
    return result;
  }
  
  protected static class Node{
    Seq<Node> children = new Seq<>();
    NuclearEnergyBuildComp self;
    Node parent;
    boolean first;
    
    Node(NuclearEnergyBuildComp self, Node parent){
      this.self = self;
      this.parent = parent;
      first = parent == null;
    }
  }
}
