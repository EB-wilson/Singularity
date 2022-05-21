package singularity.world.products;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.core.UI;
import mindustry.gen.Building;
import mindustry.ui.Styles;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.Stats;
import singularity.Singularity;
import singularity.world.components.MediumBuildComp;
import singularity.world.meta.SglStat;
import universecore.components.blockcomp.ProducerBuildComp;
import universecore.world.producers.BaseProduce;
import universecore.world.producers.ProduceType;

import static mindustry.Vars.iconMed;

public class ProduceMedium<T extends Building & ProducerBuildComp & MediumBuildComp> extends BaseProduce<T>{
  public final float product;

  public ProduceMedium(float product){
    this.product = product;
  }

  @Override
  public ProduceType<?> type(){
    return SglProduceType.medium;
  }

  @Override
  public void produce(T entity){}

  @Override
  public void update(T entity){
    entity.mediumContains(entity.mediumContains() + Math.min(entity.remainingMediumCapacity(), product*parent.delta(entity)*multiple(entity)));
  }

  @Override
  public void display(Stats stats){
    stats.add(SglStat.special, table -> {
      table.row();
      table.table(t -> {
        t.defaults().left().fill().padLeft(6);
        t.add(Core.bundle.get("misc.output") + ":").left();
        float display = product*60;
        t.table(icon -> {
          icon.add(new Stack(){{
            add(new Image(Singularity.getModAtlas("medium")));

            if(product != 0){
              Table t = new Table().left().bottom();

              t.add(display > 1000 ? UI.formatAmount(((Number)display).longValue()) : Strings.autoFixed(display, 2) + "").style(Styles.outlineLabel);
              add(t);
            }
          }}).size(iconMed).padRight(3  + (product != 0 && Strings.autoFixed(display, 2).length() > 2 ? 8 : 0));

          icon.add(StatUnit.perSecond.localized()).padLeft(2).padRight(5).color(Color.lightGray).style(Styles.outlineLabel);

          icon.add(Core.bundle.get("misc.medium"));
        });
      }).left().padLeft(5);
    });
  }

  @Override
  public void dump(T entity){
    entity.dumpMedium();
  }

  @Override
  public boolean valid(T entity){
    return entity.remainingMediumCapacity() > 0.001f;
  }
}
