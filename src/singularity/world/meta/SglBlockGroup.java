package singularity.world.meta;

import mindustry.world.meta.BlockGroup;
import universecore.util.handler.EnumHandler;
import universecore.util.handler.FieldHandler;

public class SglBlockGroup{
  private static final EnumHandler<BlockGroup> handler = new EnumHandler<>(BlockGroup.class, (inst, param) -> {
    if(param.length == 0){
      FieldHandler.setValue(BlockGroup.class, "anyReplace", inst, false);
    }
    else{
      FieldHandler.setValue(BlockGroup.class, "anyReplace", inst, param[0]);
    }
  });
  
  public static BlockGroup gas = handler.addEnumItemTail("gas", true),
      nuclear = handler.addEnumItemTail("nuclear", true);
}
