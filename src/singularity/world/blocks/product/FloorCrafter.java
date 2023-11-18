package singularity.world.blocks.product;

import arc.Core;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.world.Tile;
import singularity.world.components.FloorCrafterBuildComp;
import singularity.world.consumers.SglConsumeFloor;
import singularity.world.consumers.SglConsumeType;
import universecore.annotations.Annotations;
import universecore.world.consumers.BaseConsumers;

public class FloorCrafter extends NormalCrafter{
  public FloorCrafter(String name){
    super(name);
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    float eff = 0;
    int c = 0;

    Tile t = Vars.world.tile(x, y);
    if (t != null) {
      for (BaseConsumers consumer : consumers) {
        SglConsumeFloor<?> cons = consumer.get(SglConsumeType.floor);
        if (cons != null) {
          c++;
          eff = cons.getEff(FloorCrafterBuildComp.getFloors(t, this));
        }
      }
    }

    drawPlaceText(
        c == 1 && valid? Core.bundle.format("bar.efficiency", (int)(eff * 100f)): valid? Core.bundle.get("infos.placeValid"): Core.bundle.get("infos.placeInvalid"),
        x, y, valid
    );
  }

  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotation) {
    float eff = 0;
    int c = 0;

    for (BaseConsumers consumer : consumers) {
      SglConsumeFloor<?> cons = consumer.get(SglConsumeType.floor);
      if (cons != null) {
        c++;
        eff = cons.getEff(FloorCrafterBuildComp.getFloors(tile, this));
      }
    }

    return c > 0 && eff > 0;
  }

  @Annotations.ImplEntries
  public class FloorCrafterBuild extends NormalCrafterBuild implements FloorCrafterBuildComp {

  }
}
