package singularity.world.blocks.product;

import arc.Core;
import arc.math.Mathf;
import arc.util.Strings;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.meta.Stat;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;

public class BoosterCrafter extends NormalCrafter{
  public boolean optionalBoost = false;

  public BoosterCrafter(String name) {
    super(name);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public BaseConsumers newBooster(float boost){
    BaseConsumers res = newOptionalConsume((BoosterCrafterBuild e, BaseConsumers c) -> {
      float mul = 1;
      for (BaseConsume cons : c.all()) {
        mul *= cons.efficiency(e);
      }
      e.boostEff = Mathf.approachDelta(e.boostEff, boost*mul*Mathf.clamp(e.consumer.consEfficiency)*e.consumer.getOptionalEff(c), 0.04f);
      e.boostMarker = true;
    }, (s, c) -> {
      s.add(Stat.boostEffect, Core.bundle.get("misc.efficiency") + Strings.autoFixed(boost*100, 1) + "%");
    });
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
    public float boostEff = 1;
    public boolean boostMarker;

    @Override
    public void updateTile() {
      super.updateTile();
      if(boostMarker){
        boostMarker = false;
      }
      else boostEff = Mathf.approachDelta(boostEff, 1, 0.04f);
    }

    @Override
    public float optionalConsEff(BaseConsumers consumers) {
      return super.optionalConsEff(consumers)*(optionalBoost? boostEff: 1);
    }

    @Override
    public float consEfficiency() {
      return super.consEfficiency()*boostEff;
    }
  }
}
