package singularity.world.components;

import singularity.world.blocks.structure.MultBlockStructure;
import universecore.annotations.Annotations;

public interface StructCoreComp extends StructBlockComp{
  @Annotations.BindField("structure")
  default MultBlockStructure structure(){
    return null;
  }
}
