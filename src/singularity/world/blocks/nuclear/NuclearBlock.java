package singularity.world.blocks.nuclear;

import singularity.world.blocks.SglBlock;
import singularity.world.meta.SglBlockGroup;

public class NuclearBlock extends SglBlock{
  
  public NuclearBlock(String name){
    super(name);
    hasEnergy = true;
    solid = true;
    update = true;
    group = SglBlockGroup.nuclear;
  }
}
