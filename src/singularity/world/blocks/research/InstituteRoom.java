package singularity.world.blocks.research;

import singularity.world.blocks.SglBlock;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ChainsBlockComp;
import universecore.components.blockcomp.ChainsBuildComp;

@Annotations.ImplEntries
public class InstituteRoom extends SglBlock implements ChainsBlockComp {
  public InstituteRoom(String name) {
    super(name);
    destructible = true;
    update = false;
  }

  @Override
  public boolean chainable(ChainsBlockComp other) {
    return other instanceof Institute || other instanceof InstituteRoom;
  }

  @Annotations.ImplEntries
  public class InstituteRoomBuild extends SglBuilding implements ChainsBuildComp {
    public ResearchDevice device;
  }
}
