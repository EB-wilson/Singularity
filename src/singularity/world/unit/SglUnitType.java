package singularity.world.unit;

import arc.util.Nullable;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;

public class SglUnitType extends UnitType {
  public @Nullable ItemStack[] requirements;

  public SglUnitType(String name) {
    super(name);
  }

  @Override
  public ItemStack[] getFirstRequirements() {
    return requirements == null? super.getFirstRequirements(): requirements;
  }
}
