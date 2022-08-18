package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.struct.ObjectSet;
import arc.struct.OrderedSet;
import arc.struct.Queue;
import singularity.world.distribution.DistributeNetwork;
import universecore.util.path.BFSPathFinder;
import universecore.util.path.IPath;

public class CompBusGroup implements BFSPathFinder<ComponentBus.ComponentBusBuild>{
  private static final ObjectSet<ComponentBus.ComponentBusBuild> added = new ObjectSet<>();
  private static final Queue<ComponentBus.ComponentBusBuild> queue = new Queue<>();

  private final OrderedSet<ComponentBus.ComponentBusBuild> child = new OrderedSet<>();
  private final OrderedSet<NetPluginComp.NetPluginCompBuild> proximityComp = new OrderedSet<>();

  private long lastUpdateFrame;

  public DistributeNetwork ownerNet;

  public void add(ComponentBus.ComponentBusBuild comp){
    child.add(comp);

    comp.busGroup = this;
    comp.proximityComp.each(proximityComp::add);
  }

  public void add(CompBusGroup group){
    if(group == this) return;

    group.child.each(this::add);
  }

  public void remove(ComponentBus.ComponentBusBuild target){
    for(ComponentBus.ComponentBusBuild build: target.busGroup.child){
      if(build.busGroup != this) continue;

      build.proximityBus.remove(target);
      CompBusGroup newGroup = new CompBusGroup();
      eachVertices(build, newGroup::add);
    }
  }

  public Iterable<NetPluginComp.NetPluginCompBuild> getComps(){
    return proximityComp;
  }

  public Iterable<ComponentBus.ComponentBusBuild> getBuses(){
    return child;
  }

  @Override
  public void reset(){
    added.clear();
    queue.clear();
  }

  @Override
  public boolean relateToPointer(ComponentBus.ComponentBusBuild componentBusBuild, PathPointer<ComponentBus.ComponentBusBuild> pathPointer){
    return added.add(componentBusBuild);
  }

  @Override
  public PathPointer<ComponentBus.ComponentBusBuild> getPointer(ComponentBus.ComponentBusBuild componentBusBuild){
    return null;
  }

  @Override
  public ComponentBus.ComponentBusBuild queueNext(){
    return queue.removeFirst();
  }

  @Override
  public void queueAdd(ComponentBus.ComponentBusBuild componentBusBuild){
    queue.addFirst(componentBusBuild);
  }

  @Override
  public IPath<ComponentBus.ComponentBusBuild> createPath(){
    return null;
  }

  @Override
  public boolean isDestination(ComponentBus.ComponentBusBuild componentBusBuild, ComponentBus.ComponentBusBuild vert1){
    return false;
  }

  public void doUpdate(){
    if(lastUpdateFrame == Core.graphics.getFrameId()) return;
    child.each(ComponentBus.ComponentBusBuild::onBusUpdated);

    lastUpdateFrame = Core.graphics.getFrameId();
  }
}
