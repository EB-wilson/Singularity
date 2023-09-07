package singularity.world.blocks.product;

import arc.Core;
import arc.func.Floatf;
import arc.func.Floatp;
import arc.math.Mathf;
import arc.util.Strings;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.meta.Stat;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;

public class BoosterCrafter extends NormalCrafter{

  public BoosterCrafter(String name) {
    super(name);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public BaseConsumers newBooster(float boost){
    Floatf<BoosterCrafterBuild>[] fun = new Floatf[1];
    BaseConsumers res = newOptionalConsume((BoosterCrafterBuild e, BaseConsumers c) -> {
      e.currBoost = fun[0];
    }, (s, c) -> {
      s.add(Stat.boostEffect, t -> {
        t.table(req -> {
          req.left().defaults().left().padLeft(3);
          for (BaseConsume<? extends ConsumerBuildComp> co : c.all()) {
            co.buildIcons(req);
          }
        }).left().padRight(40);
        t.add(Core.bundle.get("misc.efficiency") + Strings.autoFixed(boost*100, 1) + "%").growX().right();
      });
    });

    fun[0] = e -> {
      float mul = 1;
      for (BaseConsume cons : res.all()) {
        mul *= cons.efficiency(e);
      }
      return boost*mul*Mathf.clamp(e.consumer.consEfficiency)*e.consumer.getOptionalEff(res);
    };

    consume.customDisplayOnly = true;
    consume.optionalAlwaysValid = false;

    return res;
  }

  @Override
  public void setBars() {
    super.setBars();
    addBar("efficiency", (BoosterCrafterBuild e) -> new Bar(
        () -> Core.bundle.get("misc.efficiency") + ": " + Mathf.round(e.workEfficiency()*100) + "%",
        () -> Pal.accent,
        e::workEfficiency
    ));
  }

  public class BoosterCrafterBuild extends NormalCrafterBuild{
    public Floatf<BoosterCrafterBuild> currBoost = e -> 1;

    @Override
    public float consEfficiency() {
      float eff = super.consEfficiency();
      return eff*currBoost.get(this);
    }
  }
}
