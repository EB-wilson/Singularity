package singularity.world.distribution;

import arc.func.Boolf2;
import arc.func.Cons2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;

import java.util.PriorityQueue;

public class MatrixGrid{
  public DistMatrixUnitBuildComp handler;
  
  final ObjectMap<Building, BuildingEntry<?>> all = new ObjectMap<>();
  
  final PriorityQueue<BuildingEntry<?>> output = new PriorityQueue<>((a, b) -> a.config.priority - b.config.priority);
  final PriorityQueue<BuildingEntry<?>> input = new PriorityQueue<>((a, b) -> a.config.priority - b.config.priority);
  final PriorityQueue<BuildingEntry<?>> container = new PriorityQueue<>((a, b) -> a.config.priority - b.config.priority);
  final PriorityQueue<BuildingEntry<?>> acceptor = new PriorityQueue<>((a, b) -> a.config.priority - b.config.priority);
  
  public int priority;
  
  public MatrixGrid(DistMatrixUnitBuildComp handler){
    this.handler = handler;
  }

  public <T> Seq<BuildingEntry<T>> get( GridChildType type, Boolf2<T, DistTargetConfigTable.TargetConfigure> req){
    return get(type, req, new Seq<>());
  }

  public <T> Seq<BuildingEntry<T>> get(GridChildType type, Boolf2<T, DistTargetConfigTable.TargetConfigure> req, Seq<BuildingEntry<T>> temp){
    temp.clear();
    each(type, req, (e, entry) -> temp.add(new BuildingEntry<>(e, entry)));
    return temp;
  }

  @SuppressWarnings("unchecked")
  public <T> void each(GridChildType type, Boolf2<T, DistTargetConfigTable.TargetConfigure> req, Cons2<T, DistTargetConfigTable.TargetConfigure> cons){
    PriorityQueue<BuildingEntry<?>> temp = null;
    switch(type){
      case output: temp = output; break;
      case input: temp = input; break;
      case container: temp = container; break;
      case acceptor: temp = acceptor; break;
    }
    
    for(BuildingEntry<?> entry: temp){
      if(req.get((T)entry.entity, entry.config)) cons.get((T) entry.entity, entry.config);
    }
  }

  public void addConfig(DistTargetConfigTable.TargetConfigure c){
    Building t = Vars.world.build(c.position);
    if(t == null) return;
    boolean existed = all.containsKey(t);
    BuildingEntry<?> entry = all.get(t, new BuildingEntry<>(t, c));

    c.eachChildType((type, map) -> {
      PriorityQueue<BuildingEntry<?>> temp = null;

      switch(type){
        case output:
          temp = output;
          break;
        case input:
          temp = input;
          break;
        case container:
          temp = container;
          break;
        case acceptor:
          temp = acceptor;
      }

      if(existed){
        entry.config.priority = priority;
        temp.remove(entry);
      }
      temp.add(entry);
    });
    all.put(t, entry);
  }
  
  public boolean remove(Building building){
    BuildingEntry<?> entry = all.remove(building);
    if(entry != null){
      output.remove(entry);
      input.remove(entry);
      container.remove(entry);
      acceptor.remove(entry);
      return true;
    }
    return false;
  }
  
  public void clear(){
    for(Building building : all.keys()){
      remove(building);
    }
  }
  
  public static class BuildingEntry<T>{
    public final T entity;
    public DistTargetConfigTable.TargetConfigure config;
    
    public BuildingEntry(T entity, DistTargetConfigTable.TargetConfigure config){
      this.entity = entity;
      this.config = config;
    }
  }
}
