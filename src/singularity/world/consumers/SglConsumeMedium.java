package singularity.world.consumers;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.Bits;
import arc.util.Strings;
import mindustry.core.UI;
import mindustry.gen.Building;
import mindustry.ui.ReqImage;
import mindustry.ui.Styles;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.Stats;
import singularity.Singularity;
import singularity.world.components.MediumBuildComp;
import singularity.world.meta.SglStat;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.UncConsumeType;

import static mindustry.Vars.iconMed;

public class SglConsumeMedium<T extends Building & MediumBuildComp & ConsumerBuildComp> extends BaseConsume<T>{
  public final float request;

  public SglConsumeMedium(float request){
    this.request = request;
  }

  @Override
  public UncConsumeType<?> type(){
    return SglConsumeType.medium;
  }

  @Override
  public void consume(T t){}

  @Override
  public void update(T entity){
    entity.removeMedium(request*entity.edelta());
  }

  @Override
  public void display(Stats stats){
    stats.add(SglStat.special, table -> {
      table.row();
      table.table(t -> {
        t.defaults().left().fill().padLeft(6);
        t.add(Core.bundle.get("misc.input") + ":").left();
        float display = request*60;
        t.table(icon -> {
          icon.add(new Stack(){{
            add(new Image(Singularity.getModAtlas("medium")));

            if(request != 0){
              Table t = new Table().left().bottom();
              t.add(display > 1000 ? UI.formatAmount(((Number)display).longValue()) : Strings.autoFixed(display, 2) + "").style(Styles.outlineLabel);
              add(t);
            }
          }}).size(iconMed).padRight(3  + (request != 0 && Strings.autoFixed(display, 2).length() > 2 ? 8 : 0));

          icon.add(StatUnit.perSecond.localized()).padLeft(2).padRight(5).color(Color.lightGray).style(Styles.outlineLabel);

          icon.add(Core.bundle.get("misc.medium"));
        });
      }).left().padLeft(5);
    });
  }

  @Override
  public void build(T entity, Table table){
    table.add(new ReqImage(Singularity.getModAtlas("medium"),
        () -> entity.mediumContains() > request*parent.delta(entity)*multiple(entity) + 0.0001f)).padRight(8);
  }

  @Override
  public boolean valid(T entity){
    return entity.mediumContains() >= request*(entity.getBlock().hasPower && entity.getBuilding().power.status != 0?
        entity.delta()*entity.power.status: entity.getBuilding().delta())*multiple(entity);
  }

  @Override
  public Bits filter(T t){
    return null;
  }
}
