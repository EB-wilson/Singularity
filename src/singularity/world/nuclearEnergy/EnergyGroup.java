package singularity.world.nuclearEnergy;

import arc.util.Log;
import singularity.world.blockComp.NuclearEnergyBuildComp;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Nullable;

public class EnergyGroup{
  private static final Queue<NuclearEnergyBuildComp> queue = new Queue<>();
  private static final IntSet finded = new IntSet();
  
  public final Seq<NuclearEnergyBuildComp> contains = new Seq<>(false);
  public final Seq<NuclearEnergyBuildComp> sources = new Seq<>(false);
  public final Seq<NuclearEnergyBuildComp> consumers = new Seq<>(false);
  public final Seq<NuclearEnergyBuildComp> buffers = new Seq<>(false);
  
  private final Seq<Seq<Path>> paths = new Seq<>();
  private final IntSeq index = new IntSeq();
  
  public void add(NuclearEnergyBuildComp entity){
    if(entity.getNuclearBlock().hasEnergyGroup()){
      if(entity.energy().group == this){
        return;
      }
      else{
        entity.energy().group = this;
      }
    }
    
    if(entity.getNuclearBlock().outputEnergy() && entity.getNuclearBlock().consumeEnergy()){
      buffers.add(entity);
    }
    
    if(entity.getNuclearBlock().outputEnergy()){
      sources.add(entity);
    }
    else if(entity.getNuclearBlock().consumeEnergy()){
      consumers.add(entity);
    }
    
    contains.add(entity);
  }
  
  public Seq<Path> getPaths(NuclearEnergyBuildComp source){
    if(source.getBuilding().id() >= index.size || !index.contains(source.getBuilding().id())) return new Seq<>();
    return paths.get(index.get(source.getBuilding().id()));
  }
  
  /**重新构建group组成，从runner开始重新搜索所有连接*/
  public void reflow(NuclearEnergyBuildComp runner){
    //第一次BFS，用于搜索整个网络当中的成员
    queue.clear();
    queue.addFirst(runner);
    finded.clear();
  
    while(queue.size > 0){
      NuclearEnergyBuildComp current = queue.removeLast();
      add(current);
      for(NuclearEnergyBuildComp next : current.getEnergyLinked()){
        //记录已搜索的目标，避免重复搜索
        if(finded.add(next.getBuilding().pos())){
          queue.addFirst(next);
        }
      }
    }
    
    calculatePath();
  }
  
  /**从Group中移除目标,并重新分配整个Group组成
   * 和PowerGraph一样，这个操作并没有移除目标，只是分配了新的而已*/
  public void remove(NuclearEnergyBuildComp removing){
    for(NuclearEnergyBuildComp other: removing.getEnergyLinked()){
      //跳过不加入网络的和在之前的处理中已经被添加的方块，减少运算次数
      if(!other.getNuclearBlock().hasEnergyGroup() || other.energy().group != this) continue;
      //第一次BFS，用于搜索整个网络当中的成员
      queue.clear();
      queue.addFirst(other);
      finded.clear();
  
      while(queue.size > 0){
        NuclearEnergyBuildComp current = queue.removeLast();
        add(current);
        for(NuclearEnergyBuildComp next : current.getEnergyLinked()){
          //记录已搜索的目标，避免重复搜索
          if(finded.add(next.getBuilding().pos()) && next != removing){
            queue.addFirst(next);
          }
        }
      }
  
      calculatePath();
    }
  }
  
  /**进行路径搜索匹配，得到每一个能量生产者到消费者的最短距离与运输阻值*/
  public void calculatePath(){
    paths.clear();
    index.clear();
    //每一个源执行一次BFS，搜索从输入点开始的所有路径，寻找所有输出点，并标记路径
    for(NuclearEnergyBuildComp source: sources){
      Seq<Path> pathCaching = new Seq<>();
      paths.add(pathCaching);
      if(source.getBuilding().id() >= index.size) index.setSize(source.getBuilding().id()+1);
      index.set(source.getBuilding().id(), paths.size-1);
      
      Queue<PathStick> tempPath = new Queue<>();
      queue.clear();
      queue.addFirst(source);
      finded.clear();
      
      PathStick preStick = null;
      while(queue.size > 0){
        NuclearEnergyBuildComp current = queue.removeLast();
        preStick = new PathStick(current, preStick);
        for(NuclearEnergyBuildComp next: current.getEnergyLinked()){
          //记录已搜索的目标，避免重复搜索
          if(finded.add(next.getBuilding().pos())){
            queue.addFirst(next);
            //将输出点标记起来，稍后使用
            if(next != source && next.getNuclearBlock().consumeEnergy())tempPath.addLast(new PathStick(next, preStick));
          }
        }
      }
      
      //返回路径，从输出点向前迭代每一个标记，直到回到输入点
      while(tempPath.size > 0){
        Path newPath = new Path();
        //取较为靠前添加的路径标记
        PathStick pathStick = tempPath.removeFirst();
        while(pathStick != null){
          //当路径标记不从属于任何一条路径时迭代
          if(pathStick.subordinate == null){
            newPath.buildings.addFirst(pathStick.current);
            newPath.resident += pathStick.current.getResident();
            pathStick.subordinate = newPath;
            pathStick = pathStick.previous;
          }
          else{
            //如果在回路找到任何一个点属于另一条路径，则直接将那个路径起始点到这个点间的路径作为当前路径的剩余路径
            //这是很自然的，因为BFS一定是先搜索到最近的一个点，若较后的一个回路上有任何一个点与之前的路径重叠
            //那么它们之前的路径也必然是重叠的
            boolean connectQueues = false;
            for(int i=pathStick.subordinate.buildings.size-1; i>0; i--){
              NuclearEnergyBuildComp otherPath = pathStick.subordinate.buildings.get(i);
              if(otherPath == pathStick.current) connectQueues = true;
              if(connectQueues){
                newPath.buildings.addFirst(otherPath);
                newPath.resident += otherPath.getResident();
              }
            }
            break;
          }
        }
        
        newPath.destination = newPath.buildings.removeLast();
        pathCaching.add(newPath);
      }
    }
  }
  
  public void addGroup(EnergyGroup other){
    if(other == this) return;
    for(NuclearEnergyBuildComp o: other.contains){
      add(o);
    }
  }
  
  public void reset(){
    contains.clear();
    sources.clear();
    contains.clear();
    buffers.clear();
  }
  
  public static class Path{
    public Queue<NuclearEnergyBuildComp> buildings = new Queue<>();
    public NuclearEnergyBuildComp destination;
    public float resident = 0f;
  }
  
  private static class PathStick{
    Path subordinate = null;
    NuclearEnergyBuildComp current;
    @Nullable PathStick previous;
    
    PathStick(NuclearEnergyBuildComp current, @Nullable PathStick previous){
      this.current = current;
      this.previous = previous;
    }
  }
}
