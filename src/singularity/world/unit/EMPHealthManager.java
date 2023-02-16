package singularity.world.unit;

import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.io.SaveFileReader;
import mindustry.io.SaveVersion;
import mindustry.type.UnitType;
import singularity.contents.OtherContents;
import universecore.UncCore;
import universecore.util.aspect.EntityAspect;
import universecore.util.aspect.triggers.TriggerEntry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class EMPHealthManager {
  private final ObjectMap<Unit, EMPModel.EMPHealth> healthMap = new ObjectMap<>();
  private final ObjectMap<UnitType, EMPModel> unitDefaultHealthMap = new ObjectMap<>();

  private Unit lastGetter;
  private EMPModel.EMPHealth lastGetted;

  private static final EMPModel.EMPHealth ZERO = new EMPModel.EMPHealth();

  public void init(){
    UncCore.aspects.addAspect(
        new EntityAspect<Unit>(EntityAspect.Group.unit, u -> true)
            .setEntryTrigger(u -> {
              healthMap.put(u, getInst(u));
            })
            .setExitTrigger(u -> {
              EMPModel.EMPHealth h = healthMap.remove(u);
              if (h == null) return;
              Pools.free(h);
            })
            .setTrigger(new TriggerEntry<>(EventType.Trigger.update, this::update))
    );

    SaveVersion.addCustomChunk("empHealth", new SaveFileReader.CustomChunk() {
      @Override
      public void write(DataOutput stream) throws IOException {
        try(Writes write = new Writes(stream)){
          write.b(version());
          write.i(healthMap.size);

          for (ObjectMap.Entry<Unit, EMPModel.EMPHealth> entry : healthMap) {
            write.f(entry.key.x);
            write.f(entry.key.y);
            write.i(entry.key.type.id);

            write.f(entry.value.empHealth);

            EMPHealthManager.this.write(entry.value, write);
          }
        }
      }

      @Override
      public void read(DataInput stream) throws IOException {
        try(Reads read = new Reads(stream)){
          byte revision = read.b();
          int len = read.i();

          for (int i = 0; i < len; i++) {
            float x = read.f();
            float y = read.f();
            float id = read.i();

            float health = read.f();

            Unit unit = null;
            float dist = 0;
            for (Unit u: Groups.unit.intersect(x - 4, y - 4, 8, 8)) {
              if (u.type.id != id) continue;

              float d = u.dst(x, y);

              if (unit == null || d < dist){
                unit = u;
                dist = d;
              }
            }

            if (unit == null){
              EMPHealthManager.this.read(new EMPModel.EMPHealth(), read, revision);
              continue;
            }

            EMPModel.EMPHealth heal = getInst(unit);
            heal.empHealth = health;
            healthMap.put(heal.unit, heal);

            EMPHealthManager.this.read(heal, read, revision);
          }
        }
      }
    });
  }

  public byte version(){
    return 0;
  }

  public EMPModel getModel(UnitType type){
    return unitDefaultHealthMap.get(type, () -> {
      EMPModel res = new EMPModel();
      res.maxEmpHealth = type.health/Mathf.pow(type.hitSize - type.armor, 2)*200;
      res.empArmor = Mathf.clamp(type.armor/100);
      res.empRepair = type.hitSize/30;
      res.empContinuousDamage = res.empRepair*2;
      return res;
    });
  }

  public EMPModel.EMPHealth getInst(Unit unit){
    return getModel(unit.type).generate(unit);
  }

  public EMPModel.EMPHealth zeroInst(Unit unit){
    ZERO.model = getModel(unit.type);
    ZERO.unit = unit;
    ZERO.empHealth = 0;
    return ZERO;
  }

  public void read(EMPModel.EMPHealth health, Reads read, int revision){
  }

  public void write(EMPModel.EMPHealth health, Writes write){
  }

  public void update(Unit unit){
    if (!unit.isAdded()){
      EMPModel.EMPHealth h = healthMap.remove(unit);
      if (h != null) Pools.free(h);
      return;
    }
    if (Vars.state.isPaused()) return;

    EMPModel.EMPHealth h = get(unit);

    if (!unit.hasEffect(OtherContents.emp_damaged)){
      if (h.empHealth <= 0){
        unit.shield = 0;
        Fx.unitShieldBreak.at(unit.x, unit.y, 0.0F, unit.team.color, unit);
        unit.damagePierce(unit.maxHealth*0.3f, true);

        unit.apply(OtherContents.emp_damaged, 660);
      }
      else if (h.empHealth < h.model.maxEmpHealth) h.empHealth += h.model.empRepair*Time.delta;
    }
  }

  public EMPModel.EMPHealth get(Unit unit){
    if (!unit.isAdded()) return zeroInst(unit);
    if (lastGetted != null && unit == lastGetter && lastGetted.bind) return lastGetted;

    return lastGetted = healthMap.get(lastGetter = unit, () -> getInst(unit));
  }

  public boolean empDamaged(Unit unit){
    EMPModel.EMPHealth h = get(unit);

    return h.empHealth < h.model.maxEmpHealth;
  }

  public float getHealth(Unit unit){
    return get(unit).empHealth;
  }

  public float healthPresent(Unit unit){
    EMPModel.EMPHealth h = get(unit);

    return Mathf.clamp(h.empHealth/h.model.maxEmpHealth);
  }

  public float empDamage(Unit unit, float damage, boolean realDam){
    EMPModel.EMPHealth h = get(unit);
    float real = Mathf.maxZero(realDam? damage: damage - damage*h.model.empArmor);
    float orig = h.empHealth;
    h.empHealth = Mathf.maxZero(h.empHealth - real);

    return orig - h.empHealth;
  }

  public void heal(Unit unit, float heal){
    EMPModel.EMPHealth h = get(unit);
    h.empHealth = Math.min(h.empHealth + heal, h.model.maxEmpHealth);
  }
}
