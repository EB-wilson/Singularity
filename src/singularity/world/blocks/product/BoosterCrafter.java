package singularity.world.blocks.product;

import arc.Core;
import arc.func.Floatf;
import arc.util.Strings;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.meta.Stat;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.producers.BaseProduce;
import universecore.world.producers.BaseProducers;

public class BoosterCrafter extends NormalCrafter{
  public BoosterCrafter(String name) {
    super(name);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public BaseConsumers newBooster(float boost){
    return newOptionalConsume((BoosterCrafterBuild e, BaseConsumers c) -> {
      float mul = 1;
      for (BaseConsume cons : c.all()) {
        mul *= cons.efficiency(e);
      }
      e.boostEff = boost*mul;
    }, (s, c) -> {
      s.add(Stat.boostEffect, Core.bundle.get("misc.efficiency") + boost*100 + "%");
    });
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void init() {
    super.init();
    for (BaseConsumers cons : consumers()) {
      for (BaseConsume<? extends ConsumerBuildComp> c : cons.all()) {
        final Floatf old = c.consMultiplier;
        c.consMultiplier = old == null? e -> ((BoosterCrafterBuild)e).boostEff: e -> ((BoosterCrafterBuild)e).boostEff*old.get(e);
      }
    }

    for (BaseProducers producer : producers()) {
      for (BaseProduce<?> p : producer.all()) {
        final Floatf old = p.prodMultiplier;
        p.prodMultiplier = old == null? e -> ((BoosterCrafterBuild)e).boostEff: e -> ((BoosterCrafterBuild)e).boostEff*old.get(e);
      }
    }
  }

  @Override
  public void setBars() {
    super.setBars();
    addBar("efficiency", (BoosterCrafterBuild e) -> new Bar(
        () -> Core.bundle.get("misc.efficiency") + ": " + Strings.autoFixed(e.boostEff*e.workEfficiency()*100, 0) + "%",
        () -> Pal.accent,
        () -> e.boostEff*e.workEfficiency()
    ));
  }

  public class BoosterCrafterBuild extends NormalCrafterBuild{
    public float boostEff = 1;

    @Override
    public void updateTile() {
      super.updateTile();
      boostEff = 1;
    }
  }
}
