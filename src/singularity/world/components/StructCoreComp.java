package singularity.world.components;

import singularity.world.blocks.structure.BlockStructure;
import universecore.annotations.Annotations;

public interface StructCoreComp extends StructBlockComp{
  @Annotations.BindField("structure")
  default BlockStructure structure(){
    return null;
  }
}
