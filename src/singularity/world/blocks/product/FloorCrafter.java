package singularity.world.blocks.product;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectFloatMap;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.world.Tile;
import org.jetbrains.annotations.NotNull;
import singularity.world.components.FloorCrafterBuildComp;
import singularity.world.consumers.SglConsumeFloor;
import singularity.world.consumers.SglConsumeType;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsumers;
import universecore.world.producers.BaseProduce;
import universecore.world.producers.BaseProducers;

public class FloorCrafter extends NormalCrafter{
  private static final Table iconsTable = new Table();

  public FloorCrafter(String name){
    super(name);
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    float eff = 0;
    int c = 0;
    int line = 0;

    Tile t = Vars.world.tile(x, y);
    if (t != null) {
      for (BaseConsumers consumer : consumers()) {
        SglConsumeFloor<?> cons = consumer.get(SglConsumeType.floor);
        if (cons != null) {
          c++;
          eff = cons.getEff(FloorCrafterBuildComp.getFloors(t, this));
        }
      }

      if (c == 0) eff = 1;
      for (ObjectFloatMap.Entry<BaseConsumers> boost : boosts) {
        SglConsumeFloor<?> cons = boost.key.get(SglConsumeType.floor);
        if (cons != null) {
          c++;
          eff *= cons.getEff(FloorCrafterBuildComp.getFloors(t, this));
        }
      }

      for (ObjectMap.Entry<BaseConsumers, BaseProducers> product : optionalProducts) {
        if (!valid && !product.key.optionalAlwaysValid) continue;

        SglConsumeFloor<?> cons = product.key.get(SglConsumeType.floor);
        if (cons != null) {
          float optEff = cons.getEff(FloorCrafterBuildComp.getFloors(t, this));
          if (optEff <= 0) continue;

          Table ta = buildIconsTable(product);

          float width = drawPlaceText(
              Core.bundle.format("bar.efficiency", (int)(optEff * 100f)),
              x, y + line, valid
          );
          float dx = x * Vars.tilesize + offset - width/2f, dy = y * Vars.tilesize + offset + size * Vars.tilesize / 2f + 5 - line*8f;
          ta.setPosition(dx - ta.getWidth()/8f, dy - ta.getHeight()/16f);
          ta.setTransform(true);
          ta.setScale(1f/8f);
          ta.draw();
          line++;
        }
      }
    }

    drawPlaceText(
        c == 1 && valid? Core.bundle.format("bar.efficiency", (int)(eff * 100f)): valid? Core.bundle.get("infos.placeValid"): Core.bundle.get("infos.placeInvalid"),
        x, y + line, valid
    );
  }

  @NotNull
  private static Table buildIconsTable(ObjectMap.Entry<BaseConsumers, BaseProducers> product) {
    iconsTable.clear();

    boolean first = true;
    for (BaseProduce<? extends ConsumerBuildComp> produce : product.value.all()) {
      if (!produce.hasIcons()) continue;

      if (!first) iconsTable.add("+").fillX().pad(4);
      iconsTable.table(ca -> {
        ca.defaults().padLeft(3).fill();

        produce.buildIcons(ca);
      }).fill();

      first = false;
    }
    iconsTable.pack();
    return iconsTable;
  }

  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotation) {
    float eff = 0;
    int c = 0;

    for (ObjectMap.Entry<BaseConsumers, BaseProducers> product : optionalProducts) {
      SglConsumeFloor<?> cons = product.key.get(SglConsumeType.floor);
      if (cons != null) {
        if (product.key.optionalAlwaysValid && cons.getEff(FloorCrafterBuildComp.getFloors(tile, this)) > 0) return true;
      }
    }

    for (BaseConsumers consumer : consumers()) {
      SglConsumeFloor<?> cons = consumer.get(SglConsumeType.floor);
      if (cons != null) {
        c++;
        eff = cons.getEff(FloorCrafterBuildComp.getFloors(tile, this));
      }
    }

    if (c == 0) eff = 1;
    for (ObjectFloatMap.Entry<BaseConsumers> boost : boosts) {
      SglConsumeFloor<?> cons = boost.key.get(SglConsumeType.floor);
      if (cons != null) {
        c++;
        eff *= cons.getEff(FloorCrafterBuildComp.getFloors(tile, this));
      }
    }

    return c > 0 && eff > 0;
  }

  @Annotations.ImplEntries
  public class FloorCrafterBuild extends NormalCrafterBuild implements FloorCrafterBuildComp {

  }
}
