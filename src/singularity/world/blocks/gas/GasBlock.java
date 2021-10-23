package singularity.world.blocks.gas;

import singularity.world.blocks.SglBlock;
import singularity.world.meta.SglBlockGroup;

public class GasBlock extends SglBlock{
  public GasBlock(String name){
    super(name);
    group = SglBlockGroup.gas;
    hasGases = true;
  
    update = true;
  }
}
