package singularity.world.distribution;

import arc.func.Boolf2;
import arc.func.Cons2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.storage.CoreBlock;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import universecore.util.colletion.TreeSeq;

@SuppressWarnings("unchecked")
public class MatrixGrid{
  private static final Seq tmp = new Seq();

  public DistMatrixUnitBuildComp handler;
  
  final ObjectMap<Building, BuildingEntry<?>> all = new ObjectMap<>();
  
  final TreeSeq<BuildingEntry<?>> output = new TreeSeq<>((a, b) -> b.config.priority - a.config.priority);
  final TreeSeq<BuildingEntry<?>> input = new TreeSeq<>((a, b) -> b.config.priority - a.config.priority);
  final TreeSeq<BuildingEntry<?>> container = new TreeSeq<>((a, b) -> b.config.priority - a.config.priority);
  final TreeSeq<BuildingEntry<?>> acceptor = new TreeSeq<>((a, b) -> b.config.priority - a.config.priority);
  
  public int priority;
  
  public MatrixGrid(DistMatrixUnitBuildComp handler){
    this.handler = handler;
  }

  public void update(){
    for(Building bu: all.keys()){
      if(!(bu instanceof CoreBlock.CoreBuild && bu.isAdded()) && bu.tile.build != bu){
        remove(bu);
      }
    }
  }

  public <T> Seq<BuildingEntry<T>> get( GridChildType type, Boolf2<T, DistTargetConfigTable.TargetConfigure> req){
    return get(type, req, tmp);
  }

  public <T> Seq<BuildingEntry<T>> get(GridChildType type, Boolf2<T, DistTargetConfigTable.TargetConfigure> req, Seq<BuildingEntry<T>> temp){
    temp.clear();
    each(type, req, (e, entry) -> temp.add((BuildingEntry<T>) all.get((Building) e)));
    return temp;
  }

  @SuppressWarnings("unchecked")
  public <T> void each(GridChildType type, Boolf2<T, DistTargetConfigTable.TargetConfigure> req, Cons2<T, DistTargetConfigTable.TargetConfigure> cons){
    TreeSeq<BuildingEntry<?>> temp = switch(type){
      case output -> output;
      case input -> input;
      case container -> container;
      case acceptor -> acceptor;
    };

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
      TreeSeq<BuildingEntry<?>> temp = switch(type){
        case output -> output;
        case input -> input;
        case container -> container;
        case acceptor -> acceptor;
      };

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
