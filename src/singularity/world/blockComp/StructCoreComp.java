package singularity.world.blockComp;

import singularity.world.blocks.structure.MultBlockStructure;
import universeCore.annotations.Annotations;

public interface StructCoreComp extends StructBlockComp{
  @Annotations.BindField("structure")
  default MultBlockStructure structure(){
    return null;
  }
}
