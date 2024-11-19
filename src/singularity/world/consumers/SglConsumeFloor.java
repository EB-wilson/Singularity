package singularity.world.consumers;

import arc.Core;
import arc.math.Mathf;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectFloatMap;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.Stats;
import singularity.graphic.SglDrawConst;
import singularity.world.components.FloorCrafterBuildComp;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.ConsumeType;

public class SglConsumeFloor<T extends Building & ConsumerBuildComp & FloorCrafterBuildComp> extends BaseConsume<T> {
  public final ObjectFloatMap<Floor> floorEff = new ObjectFloatMap<>();

  public float baseEfficiency = 1;

  public SglConsumeFloor(Object... floors){
    for (int i = 0; i < floors.length; i+=2) {
      Floor floor = (Floor) floors[i];
      Float effInc = (Float) floors[i + 1];

      floorEff.put(floor, effInc);
    }
  }

  public SglConsumeFloor(Attribute attribute, float scl){
    this(true, true, attribute, scl);
  }

  public SglConsumeFloor(boolean checkDeep, boolean checkLiquid, Attribute attribute, float scl){
    for (Block block : Vars.content.blocks()) {
      if (!(block instanceof Floor f) || (checkDeep && f.isDeep()) || (checkLiquid && f.isLiquid) || f.attributes.get(attribute) <= 0) continue;

      floorEff.put(f, block.attributes.get(attribute)*scl);
    }
  }

  public SglConsumeFloor(boolean checkDeep, boolean checkLiquid, Object[] attributes){
    for (Block block : Vars.content.blocks()) {
      for (int i = 0; i < attributes.length; i+=2) {
        Attribute attribute = (Attribute) attributes[i];
        float scl = (float) attributes[i + 1];

        if (!(block instanceof Floor f) || (checkDeep && f.isDeep()) || (checkLiquid && f.isLiquid) || f.attributes.get(attribute) <= 0) continue;

        floorEff.put(f, floorEff.get(f, 1f)*block.attributes.get(attribute)*scl);
      }
    }
  }

  public float getEff(ObjectIntMap<Floor> floorCount){
    float res = baseEfficiency;

    for (ObjectIntMap.Entry<Floor> entry : floorCount) {
      res += floorEff.get(entry.key, 0)*entry.value;
    }

    return res;
  }

  @Override
  public ConsumeType<?> type() {
    return SglConsumeType.floor;
  }

  @Override
  public void buildIcons(Table table) {
    table.image(Icon.terrain);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void merge(BaseConsume<T> baseConsume) {
    if (baseConsume instanceof SglConsumeFloor cons){
      for (Object o : cons.floorEff) {
        if (o instanceof ObjectFloatMap map){
          for (ObjectFloatMap.Entry<Floor> entry : ((ObjectFloatMap<Floor>) map)) {
            floorEff.put(entry.key, floorEff.get(entry.key, 1)*entry.value);
          }
        }
      }

      return;
    }
    throw new IllegalArgumentException("only merge consume with same type");
  }

  @Override
  public void consume(T t) {
    //no action
  }

  @Override
  public void update(T t) {
    //no action
  }

  @Override
  public void display(Stats stats) {
    stats.add(Stat.tiles, st -> {
      st.row().table(SglDrawConst.grayUIAlpha, t -> {
        t.clearChildren();
        t.defaults().pad(5).left();

        int c = 0;
        for (ObjectFloatMap.Entry<Floor> entry : floorEff) {
          t.stack(
              new Image(entry.key.uiIcon).setScaling(Scaling.fit),
              new Table(table -> {
                table.top().right().add((entry.value < 0 ? "[scarlet]" : baseEfficiency == 0 ? "[accent]" : "[accent]+") + (int)(entry.value*100) + "%").style(Styles.outlineLabel);
                table.top().left().add("/" + StatUnit.blocks.localized()).color(Pal.gray);
              })
          ).fill().padRight(4);
          t.add(entry.key.localizedName).left().padLeft(0);
          c++;

          if (c != 0 && c % 3 == 0){
            t.row();
          }
        }
      }).fill();
    });
  }

  @Override
  public void build(T entity, Table table) {/*none*/}

  @Override
  public void buildBars(T t, Table bars) {
    bars.row();
    bars.add(new Bar(
        () -> Core.bundle.get("infos.floorEfficiency") + ": " + Strings.autoFixed(Mathf.round(efficiency(t)*100), 0) + "%",
        () -> Pal.accent,
        () -> Mathf.clamp(efficiency(t))
    )).growX().height(18f).pad(4);
    bars.row();
  }

  @Override
  public float efficiency(T t) {
    return getEff(t.floorCount());
  }

  @Override
  public Seq<Content> filter() {
    return null;
  }
}
