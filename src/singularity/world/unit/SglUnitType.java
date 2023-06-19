package singularity.world.unit;

import arc.util.Nullable;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;

public class SglUnitType extends UnitType {
  public @Nullable ItemStack[] requirements;

  public SglUnitType(String name) {
    super(name);
  }

  public void requirements(Object... req){
    requirements = ItemStack.with(req);
  }

  @Override
  public ItemStack[] getRequirements(UnitType[] prevReturn, float[] timeReturn) {
    if (requirements == null) return super.getRequirements(prevReturn, timeReturn);

    if (totalRequirements != null) return totalRequirements;

    totalRequirements = requirements;
    buildTime = 0;
    prevReturn[0] = null;

    for(ItemStack stack: requirements){
      buildTime += stack.item.cost * stack.amount;
    }
    timeReturn[0] = buildTime;

    return requirements;
  }
}
