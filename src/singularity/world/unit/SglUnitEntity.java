package singularity.world.unit;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Hitboxc;
import mindustry.gen.UnitEntity;
import singularity.world.unit.abilities.ICollideBlockerAbility;
import universecore.annotations.Annotations;
import universecore.components.ExtraVariableComp;

@SuppressWarnings({"unchecked", "rawtypes"})
@Annotations.ImplEntries
public class SglUnitEntity extends UnitEntity implements ExtraVariableComp {

  @Override
  public int classId() {
    return 51;
  }

  @Override
  public boolean collides(Hitboxc other) {
    for (Ability ability : abilities) {
      if (ability instanceof ICollideBlockerAbility blocker && blocker.blockedCollides(this, other)) return false;
    }

    return super.collides(other);
  }

  @Override
  public void add() {
    super.add();
    if (type instanceof SglUnitType sglUnitType) sglUnitType.init(this);
    else throw new RuntimeException("Unit type must be SglUnitType");
  }

  @Override
  public void read(Reads read) {
    super.read(read);
    if (type instanceof SglUnitType sglUnitType) sglUnitType.read(this, read, read.i());
    else throw new RuntimeException("Unit type must be SglUnitType");
  }

  @Override
  public void write(Writes write) {
    super.write(write);
    if (type instanceof SglUnitType sglUnitType){
      write.i(sglUnitType.version());
      sglUnitType.write(this, write);
    }
    else throw new RuntimeException("Unit type must be SglUnitType");
  }
}
