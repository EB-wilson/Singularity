package singularity.world.blocks.research;

import arc.struct.Seq;
import arc.util.Structs;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.game.planet.context.ResearchContext;
import singularity.world.blocks.SglBlock;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ChainsBlockComp;
import universecore.components.blockcomp.ChainsBuildComp;

@Annotations.ImplEntries
public class Institute extends SglBlock implements ChainsBlockComp {
  public int baseTechPoints = 4;

  public Institute(String name) {
    super(name);
    configurable = true;
    update = true;
  }

  @Override
  public boolean chainable(ChainsBlockComp other) {
    return other instanceof Institute || other instanceof InstituteRoom;
  }

  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotation) {
    return super.canPlaceOn(tile, team, rotation)
           && !Sgl.logic.currentPlanet.currentContext(team, ResearchContext.class).processing;
  }

  @Annotations.ImplEntries
  public class InstituteBuild extends SglBuilding implements ChainsBuildComp {
    public ResearchContext context;
    public Seq<InstituteRoom.InstituteRoomBuild> rooms = new Seq<>();

    boolean deviceUpdated = false;

    @Override
    public void created() {
      super.created();

      context = Sgl.logic.currentPlanet.currentContext(team(), ResearchContext.class);
      context.processing = true;
    }

    @Override
    public void onDestroyed() {
      context.processing = false;
    }

    public void deviceUpdate(){
      deviceUpdated = true;
    }

    @Override
    public void onChainsUpdated() {
      deviceUpdate();
    }

    @Override
    public void updateTile() {
      super.updateTile();

      if (deviceUpdated) {
        deviceUpdated = false;

        rooms.clear();
        for (ChainsBuildComp comp : this) {
          if (comp instanceof InstituteRoom.InstituteRoomBuild room) rooms.add(room);
        }

        context.devices.clear();

        for (InstituteRoom.InstituteRoomBuild room: rooms) {
          if (room.device != null) {
            context.devices.add(room.device);
          }
        }
        context.updateTechs(baseTechPoints);
      }
    }
  }
}
