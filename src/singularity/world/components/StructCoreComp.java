package singularity.world.components;

import arc.struct.Seq;
import singularity.world.blocks.structure.BlockStructure;
import universecore.annotations.Annotations;

public interface StructCoreComp extends StructBlockComp{
  @Annotations.BindField(value = "structures", initialize = "new arc.struct.Seq<>()")
  default Seq<BlockStructure> structures(){
    return null;
  }

  default void addStruct(BlockStructure structure){
    if (structures().contains(structure)) return;
    structures().add(structure);
  }
}
