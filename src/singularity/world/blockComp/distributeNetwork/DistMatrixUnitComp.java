package singularity.world.blockComp.distributeNetwork;

import mindustry.gen.Building;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;

public interface DistMatrixUnitComp extends DistElementBuildComp{
  MatrixGrid matrixGrid();
  
  @Override
  default int priority(){
    return matrixGrid().priority;
  }
  
  @Override
  default void priority(int priority){
    matrixGrid().priority = priority;
    distributor().network.priorityModified(this);
  }
  
  default boolean canDistribute(Building entity){
    return (entity.block.hasItems)
        || (entity.block.hasLiquids)
        || (entity instanceof GasBuildComp && ((GasBuildComp) entity).getGasBlock().hasGases());
  }
  
  default void gridAdd(Building entity, GridChildType type, int priority){
    matrixGrid().add(entity, type, priority);
  }
  
  default boolean gridRemove(Building entity){
    return matrixGrid().remove(entity);
  }
}
