package singularity.world.blocks.turrets;

import arc.func.Cons;
import arc.func.Func;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.entities.bullet.BulletType;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.meta.StatUnit;
import singularity.world.consumers.SglConsumers;
import singularity.world.meta.SglStat;
import universecore.util.Empties;
import universecore.world.consumers.BaseConsumers;

public class ProjectileTurret extends SglTurret{
  public ObjectMap<BaseConsumers, Func<BulletType, WarpedBulletType>> warheadCoatings = new ObjectMap<>();
  public ObjectMap<BaseConsumers, String> coatingsNames = new ObjectMap<>();

  private final ObjectMap<BulletType, ObjectMap<BaseConsumers, WarpedBulletType>> realAmmos = new ObjectMap<>();

  public int maxBufferCoatings = 10;

  public ProjectileTurret(String name){
    super(name);
  }

  @Override
  public void init(){
    super.init();
    for(ObjectMap.Entry<BaseConsumers, AmmoDataEntry> type: ammoTypes){
      for(ObjectMap.Entry<BaseConsumers, Func<BulletType, WarpedBulletType>> coating: warheadCoatings){
        realAmmos.get(type.value.bulletType, ObjectMap::new).put(coating.key, coating.value.get(type.value.bulletType));
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
        () -> "< " + (e.currentAmmoCons.isEmpty()? "EMPTY" : coatingsNames.get(e.currentAmmoCons.peek())) + " >",
        () -> Pal.bar,
        () -> (float) e.currentAmmoCons.size/maxBufferCoatings
    ));
  }

  public void newAmmoCoating(String name, Func<BulletType, WarpedBulletType> ammoType, Cons<Table> display){
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
    optionalCons.add(consume);

    BaseConsumers cons = consume;
    consume.setConsTrigger((ProjectileTurretBuild e) -> {
      e.applyShootType(cons);
    });
    consume.consValidCondition((ProjectileTurretBuild e) -> e.currentAmmoCons.size < maxBufferCoatings);
    warheadCoatings.put(consume, ammoType);
    coatingsNames.put(consume, name);
  }

  public class ProjectileTurretBuild extends SglTurretBuild{
    Seq<BaseConsumers> currentAmmoCons = new Seq<>();

    public void applyShootType(BaseConsumers type){
      currentAmmoCons.add(type);
    }

    @Override
    public void doShoot(BulletType type){
      BulletType b = currentAmmoCons.isEmpty()? type: realAmmos.get(type, Empties.nilMapO()).get(currentAmmoCons.pop());
      super.doShoot(b == null? type: b);
    }

    @Override
    public boolean shouldConsumeOptions(){
      return super.shouldConsumeOptions() || currentAmmoCons.size < maxBufferCoatings;
    }
  }
}
