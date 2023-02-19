package singularity.world.unit;

import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.io.SaveFileReader;
import mindustry.io.SaveVersion;
import mindustry.mod.Mods;
import mindustry.type.UnitType;
import mindustry.world.meta.StatUnit;
import singularity.contents.OtherContents;
import singularity.world.meta.SglStat;
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
            .setTrigger(new TriggerEntry<>(EventType.Trigger.update, u -> {
              if (Vars.state.isGame()){
                update(u);
              }
            }))
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
            for (Unit u: Groups.unit) {
              if (!Mathf.equal(u.x, x) || !Mathf.equal(u.y, y)) continue;
              if (u.type.id != id) continue;

              unit = u;
              break;
            }

            if (unit == null){
              Log.err("emp index unit not found in (" + x + ", " + y + ")");
              continue;
            }

            EMPModel.EMPHealth heal = getInst(unit);
            heal.empHealth = health;
            healthMap.put(heal.unit, heal);

            unit.update();
          }
        }
      }
    });

    for (Mods.LoadedMod mod : Vars.mods.list()) {
      Fi assignFile = mod.root.child("sgl_assign_list.hjson");
      if (!assignFile.exists()) assignFile = mod.root.child("sgl_assign_list.json");
      if (!assignFile.exists()) continue;

      Jval list = Jval.read(assignFile.reader());
      for (ObjectMap.Entry<String, Jval> entry : list.get("empHealthModels").asObject()) {
        UnitType type = Vars.content.unit(entry.key);
        if (type == null) {
          Log.warn("unknow type with name \"" + entry.key + "\"");
          continue;
        }

        EMPModel model = new EMPModel();
        model.maxEmpHealth = entry.value.getFloat("maxEmpHealth", type.health/Mathf.pow(type.hitSize - type.armor, 2)*200);
        model.empArmor = entry.value.getFloat("empArmor", Mathf.clamp(type.armor/100));
        model.empRepair = entry.value.getFloat("empRepair", type.hitSize/60);
        model.empContinuousDamage = entry.value.getFloat("empContinuousDamage", type.hitSize/30);

        type.stats.add(SglStat.empHealth, model.maxEmpHealth);
        type.stats.add(SglStat.empArmor, model.empArmor*100, StatUnit.percent);
        type.stats.add(SglStat.empRepair, model.empRepair*60, StatUnit.perSecond);

        unitDefaultHealthMap.put(type, model);
      }
    }

    for (UnitType unit : Vars.content.units()) {
      getModel(unit);
    }
  }

  public byte version(){
    return 0;
  }

  public EMPModel getModel(UnitType type){
    return unitDefaultHealthMap.get(type, () -> {
      EMPModel res = new EMPModel();
      res.maxEmpHealth = type.health/Mathf.pow(type.hitSize - type.armor, 2)*200;
      res.empArmor = Mathf.clamp(type.armor/100);
      res.empRepair = type.hitSize/60;
      res.empContinuousDamage = res.empRepair*2;

      type.stats.add(SglStat.empHealth, res.maxEmpHealth);
      type.stats.add(SglStat.empArmor, res.empArmor*100, StatUnit.percent);
      type.stats.add(SglStat.empRepair, res.empRepair*60, StatUnit.perSecond);
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
