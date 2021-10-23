package singularity.world.meta;

import mindustry.world.meta.BlockGroup;
import universeCore.util.handler.EnumHandler;
import universeCore.util.handler.FieldHandler;

public class SglBlockGroup{
  private static final EnumHandler<BlockGroup> handler = new EnumHandler<>(BlockGroup.class){
    @Override
    public void constructor(BlockGroup instance, Object... param){
      if(param.length == 0){
        FieldHandler.setValue(BlockGroup.class, "anyReplace", instance, false);
      }
      else{
        FieldHandler.setValue(BlockGroup.class, "anyReplace", instance, param[0]);
      }
    }
  };
  
  public static BlockGroup gas = handler.addEnumItemTail("gas", true),
      nuclear = handler.addEnumItemTail("nuclear", true);
}
