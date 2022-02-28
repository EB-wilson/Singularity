package singularity.world.distribution;

import arc.func.Boolf;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.gen.Building;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;

import java.util.PriorityQueue;

public class MatrixGrid{
  public DistMatrixUnitBuildComp handler;
  private static final Seq<Building> t = new Seq<>();
  private static final Seq<BuildingEntry<?>> temp = new Seq<>();
  
  final ObjectMap<Building, BuildingEntry<?>> outputAll = new ObjectMap<>();
  final ObjectMap<Building, BuildingEntry<?>> inputAll = new ObjectMap<>();
  final ObjectMap<Building, BuildingEntry<?>> contAll = new ObjectMap<>();
  final ObjectMap<Building, BuildingEntry<?>> acceptorAll = new ObjectMap<>();
  
  final PriorityQueue<BuildingEntry<?>> output = new PriorityQueue<>((a, b) -> a.priority - b.priority);
  final PriorityQueue<BuildingEntry<?>> input = new PriorityQueue<>((a, b) -> a.priority - b.priority);
  final PriorityQueue<BuildingEntry<?>> container = new PriorityQueue<>((a, b) -> a.priority - b.priority);
  final PriorityQueue<BuildingEntry<?>> acceptor = new PriorityQueue<>((a, b) -> a.priority - b.priority);
  
  public int priority;
  
  public MatrixGrid(DistMatrixUnitBuildComp handler){
    this.handler = handler;
  }
  
  @SuppressWarnings("unchecked")
  public <T> Seq<Building> get(Class<T> clazz, GridChildType type, Boolf<T> req){
    PriorityQueue<BuildingEntry<?>> temp = null;
    switch(type){
      case output: temp = output; break;
      case input: temp = input; break;
      case container: temp = container; break;
      case acceptor: temp = acceptor; break;
    }
    
    t.clear();
    for(BuildingEntry<?> entry: temp){
      if(clazz.isAssignableFrom(entry.entity.getClass()) && req.get((T)entry.entity)) t.add(entry.entity);
    }
    
    return t;
  }
  
  @SuppressWarnings("unchecked")
  public <T> void get(Class<T> clazz, GridChildType type, Boolf<T> req, Cons<T> cons){
    PriorityQueue<BuildingEntry<?>> temp = null;
    switch(type){
      case output: temp = output; break;
      case input: temp = input; break;
      case container: temp = container; break;
      case acceptor: temp = acceptor; break;
    }
    
    for(BuildingEntry<?> entry: temp){
      if(clazz.isAssignableFrom(entry.entity.getClass()) && req.get((T)entry.entity)) cons.get((T) entry.entity);
    }
  }
  
  @SuppressWarnings("unchecked")
  public <T extends Building> void add(T entity, GridChildType type, int priority){
    PriorityQueue<BuildingEntry<?>> temp = null;
    ObjectMap<Building, BuildingEntry<?>> all = null;
    
    switch(type){
      case output:
        temp = output;
        all = outputAll;
        break;
      case input:
        temp = input;
        all = inputAll;
        break;
      case container:
        temp = container;
        all = contAll;
        break;
      case acceptor:
        temp = acceptor;
        all = acceptorAll;
    }
  
    BuildingEntry<T> entry = (BuildingEntry<T>)all.get(entity);
    if(entry != null){
      entry.priority = priority;
      temp.remove(entry);
    }
    else{
      entry = new BuildingEntry<>(entity, priority, type);
      all.put(entity, entry);
    }
    temp.add(entry);
  }
  
  public boolean remove(Building building){
    temp.clear();
    BuildingEntry<?> tmpEntry;
    tmpEntry = outputAll.remove(building);
    if(tmpEntry != null) temp.add(tmpEntry);
    tmpEntry = inputAll.remove(building);
    if(tmpEntry != null) temp.add(tmpEntry);
    tmpEntry = contAll.remove(building);
    if(tmpEntry != null) temp.add(tmpEntry);
    tmpEntry = acceptorAll.remove(building);
    if(tmpEntry != null) temp.add(tmpEntry);
    
    for(BuildingEntry<?> entry : temp){
      switch(entry.type){
        case output: output.remove(entry); break;
        case input: input.remove(entry); break;
        case container: container.remove(entry); break;
        case acceptor: acceptor.remove(entry); break;
      }
    }
    return !temp.isEmpty();
  }
  
  public static class BuildingEntry<T extends Building>{
    public final T entity;
    public int priority;
    public final GridChildType type;
    
    public BuildingEntry(T entity, int priority, GridChildType type){
      this.entity = entity;
      this.priority = priority;
      this.type = type;
    }
  }
}
