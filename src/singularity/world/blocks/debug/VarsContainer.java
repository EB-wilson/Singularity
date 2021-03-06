package singularity.world.blocks.debug;

import arc.struct.ObjectMap;
import mindustry.gen.Building;
import mindustry.world.Block;
import universecore.debugs.ObjectDataMonitor;

public class VarsContainer extends Block{
  public VarsContainer(String name){
    super(name);
    update = true;
    hasItems = false;
    hasLiquids = false;
    hasPower = false;
  }
  
  public static class VarsContainerBuild extends Building{
    public ObjectMap<String, ObjectDataMonitor.VarStructure> vars = new ObjectMap<>();
  }
}
