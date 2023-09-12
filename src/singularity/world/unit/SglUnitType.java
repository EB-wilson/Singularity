package singularity.world.unit;

import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
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
    if (prevReturn != null) prevReturn[0] = null;

    for(ItemStack stack: requirements){
      buildTime += stack.item.cost * stack.amount;
    }
    if (timeReturn != null) timeReturn[0] = buildTime;

    return requirements;
  }

  public int version(){
    return 0;
  }

  public void init(SglUnitEntity unit) {}

  public void read(SglUnitEntity sglUnitEntity, Reads read, int revision) {
  }

  public void write(SglUnitEntity sglUnitEntity, Writes write) {
  }
}
