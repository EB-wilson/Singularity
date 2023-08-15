package singularity.world.blocks.turrets;

import arc.func.Cons;
import arc.func.Func;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustry.entities.bullet.BulletType;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.meta.StatUnit;
import singularity.world.consumers.SglConsumers;
import singularity.world.meta.SglStat;
import universecore.util.Empties;
import universecore.world.consumers.BaseConsumers;

public class ProjectileTurret extends SglTurret{
  public ObjectMap<BaseConsumers, CoatingModel> coatings = new ObjectMap<>();

  private final ObjectMap<BulletType, ObjectMap<BaseConsumers, BulletType>> realAmmos = new ObjectMap<>();

  public int maxBufferCoatings = 10;

  public ProjectileTurret(String name){
    super(name);
  }

  @Override
  public void init(){
    super.init();
    for(ObjectMap.Entry<BaseConsumers, AmmoDataEntry> type: ammoTypes){
      for(ObjectMap.Entry<BaseConsumers, CoatingModel> coating: coatings){
        realAmmos.get(type.value.bulletType, ObjectMap::new).put(coating.key, coating.value.coatingFunc.get(type.value.bulletType));
      }
    }
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(SglStat.maxCoatingBuffer, maxBufferCoatings);
  }

  @Override
  public void setBars(){
    super.setBars();
    addBar("coatings", (ProjectileTurretBuild e) -> new Bar(
        () -> "< " + (e.coatCursor <= 0? "EMPTY" : coatings.get(e.currentAmmoCons[e.coatCursor - 1]).name) + " >",
        () -> e.coatCursor <= 0? Pal.bar: coatings.get(e.currentAmmoCons[e.coatCursor - 1]).color,
        () -> (float) e.coatCursor/maxBufferCoatings
    ));
  }

  public void newAmmoCoating(String name, Color color, Func<BulletType, BulletType> ammoType, Cons<Table> display){
    newAmmoCoating(name, color, ammoType, display, 1);
  }

  public void newAmmoCoating(String name, Color color, Func<BulletType, BulletType> ammoType, Cons<Table> display, int amount){
    consume = new SglConsumers(true){
      {
        showTime = false;
      }

      @Override
      public BaseConsumers time(float time){
        showTime = false;
        craftTime = time;
        return this;
      }
    };
    consume.optionalDef = (e, c) -> {};
    consume.display = (s, c) -> {
      s.add(SglStat.bulletCoating, t -> {
        t.row();
        t.add("< " + name + " >").color(Pal.accent).left().padLeft(15);
        t.row();
        t.table(ta -> {
          ta.defaults().left().padLeft(15).padTop(4);
          display.get(ta);
        });
      });
      s.add(SglStat.coatingTime, c.craftTime/60, StatUnit.seconds);
    };
    optionalCons().add(consume);

    BaseConsumers cons = consume;
    consume.setConsTrigger((ProjectileTurretBuild e) -> {
      for (int i = 0; i < amount; i++) {
        e.applyShootType(cons);
      }
    });
    consume.consValidCondition((ProjectileTurretBuild e) -> e.coatCursor + amount <= maxBufferCoatings);

    CoatingModel model = new CoatingModel();
    model.name = name;
    model.color = color;
    model.coatingFunc = ammoType;

    coatings.put(consume, model);
  }

  public class ProjectileTurretBuild extends SglTurretBuild{
    public BaseConsumers[] currentAmmoCons = new BaseConsumers[maxBufferCoatings];
    public int coatCursor;

    public void applyShootType(BaseConsumers type){
      currentAmmoCons[coatCursor++] = type;
    }

    @Override
    public void doShoot(BulletType type){
      if (coatCursor <= 0) {
        super.doShoot(type);
      }
      else {
        coatCursor--;
        BaseConsumers cons = currentAmmoCons[coatCursor];
        currentAmmoCons[coatCursor] = null;
        BulletType b = realAmmos.get(type, Empties.nilMapO()).get(cons);

        super.doShoot(b == null? type: b);
      }
    }

    @Override
    public boolean shouldConsumeOptions(){
      return super.shouldConsumeOptions() || coatCursor < maxBufferCoatings;
    }
  }

  public static class CoatingModel{
    public String name;
    public Color color;
    public Func<BulletType, BulletType> coatingFunc;
  }
}
