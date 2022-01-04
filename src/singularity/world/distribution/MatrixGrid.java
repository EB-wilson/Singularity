package singularity.world.distribution;

import arc.func.Boolf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.gen.Building;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitComp;

import java.util.PriorityQueue;

public class MatrixGrid{
  public DistMatrixUnitComp handler;
  private static final Seq<Building> t = new Seq<>();
  
  ObjectMap<Building, BuildingEntry<?>> all = new ObjectMap<>();
  
  PriorityQueue<BuildingEntry<?>> output = new PriorityQueue<>((a, b) -> a.priority - b.priority);
  PriorityQueue<BuildingEntry<?>> input = new PriorityQueue<>((a, b) -> a.priority - b.priority);
  PriorityQueue<BuildingEntry<?>> container = new PriorityQueue<>((a, b) -> a.priority - b.priority);
  
  public int priority;
  
  public MatrixGrid(DistMatrixUnitComp handler){
    this.handler = handler;
  }
  
  @SuppressWarnings("unchecked")
  public <T> Seq<Building> get(Class<T> clazz, GridChildType type, Boolf<T> req){
    PriorityQueue<BuildingEntry<?>> temp = null;
    switch(type){
      case output: temp = output; break;
      case input: temp = input; break;
      case container: temp = container; break;
    }
    
    t.clear();
    for(BuildingEntry<?> entry: temp){
      if(clazz.isAssignableFrom(entry.entity.getClass()) && req.get((T)entry.entity)) t.add(entry.entity);
    }
    
    return t;
  }
  
  @SuppressWarnings("unchecked")
  public <T extends Building> void add(T entity, GridChildType type, int priority){
    PriorityQueue<BuildingEntry<?>> temp = null;
    switch(type){
      case output: temp = output; break;
      case input: temp = input; break;
      case container: temp = container; break;
    }
  
    BuildingEntry<T> entry = (BuildingEntry<T>)all.get(entity);
    if(entry != null){
      entry.priority = priority;
      entry.type = type;
      
      switch(entry.type){
        case output: output.remove(entry); break;
        case input: input.remove(entry); break;
        case container: container.remove(entry); break;
      }
    }
    else{
      entry = new BuildingEntry<>(entity, priority, type);
      all.put(entity, entry);
    }
    temp.add(entry);
  }
  
  public boolean remove(Building building){
    BuildingEntry<?> entry = all.remove(building);
  
    if(entry != null){
      switch(entry.type){
        case output: output.remove(entry); break;
        case input: input.remove(entry); break;
        case container: container.remove(entry); break;
      }
      
      return true;
    }
    return false;
  }
  
  public static class BuildingEntry<T extends Building>{
    public final T entity;
    public int priority;
    public GridChildType type;
    
    public BuildingEntry(T entity, int priority, GridChildType type){
      this.entity = entity;
      this.priority = priority;
      this.type = type;
    }
  }
}
