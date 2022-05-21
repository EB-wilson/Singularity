package singularity.ui.tables;

import arc.Core;
import arc.func.Prov;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Strings;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.meta.SglStatUnit;
import universecore.ui.elements.chart.Chart;
import universecore.ui.elements.chart.LineChart;

public class DistNetMonitor extends Table{
  protected final static String DELTA = "_delta";
  protected final static String PUT = "_put";
  protected final static String READ = "_read";

  private final int maxValueCount;

  private final Seq<BaseBuffer<?, ?, ?>> targetBuffer = new Seq<>();
  private final ObjectMap<BaseBuffer<?, ?, ?>, LineChart> childCharts = new ObjectMap<>();
  private final ObjectMap<Object, Chart.StatGroup> detailGroups = new ObjectMap<>();

  private final ObjectMap<BaseBuffer<?, ?, ?>, ObjectMap<String, Chart.StatGroup>> statGroups = new ObjectMap<>();

  private final Interval timer = new Interval(2);

  public float flushTime = 30;
  public int displayCount = 20;

  public float defaultMax = 16, defaultMin = 0;

  protected LineChart defChart;

  LineChart currentChart;
  Table chartTable, botTable, bufferList;
  BaseBuffer<?, ?, ?> currentSelect;

  Runnable changedChart;

  boolean groupCreated = true;

  public DistNetMonitor(int maxCount){
    this.maxValueCount = maxCount;
    displayCount = Math.min(maxCount, displayCount);

    update(() -> {
      if(timer.get(flushTime)){
        for(BaseBuffer<?, ?, ?> buffer: targetBuffer){
          if(buffer.putRate() >= 0 && buffer.readRate() >= 0){
            ObjectMap<String, Chart.StatGroup> groups = statGroups.get(buffer, ObjectMap::new);
            groups.get(DELTA).putValue((buffer.putRate() - buffer.readRate())*60);
            groups.get(PUT).putValue((buffer.putRate())*60);
            groups.get(READ).putValue((buffer.readRate())*60);

            for(BaseBuffer.Packet<?, ?> packet: buffer){
              if(packet.putRate() >= 0 && packet.readRate() >= 0){
                detailGroups.get(packet.get(), () -> {
                  Chart.StatGroup g = childCharts.get(buffer).newStatGroup();
                  g.color = packet.color();
                  g.hide();

                  groupCreated = true;

                  return g;
                }).putValue(packet.putRate()*60 - packet.readRate()*60);
              }
            }
          }
        }
        currentChart.updateValueBound();
      }
    });

    table(main -> {
      main.table(Tex.buttonTrans, t -> {
        chartTable = t.table(c -> {
          changedChart = () -> {
            c.clear();
            if(defChart == null){
              defChart = new LineChart(maxCount);
              defChart.horizontal = Core.bundle.get("misc.time");
              defChart.vertical = SglStatUnit.bytePreSecond.localized();
            }

            currentChart = c.add(currentSelect == null ? defChart : childCharts.get(currentSelect)).grow().margin(8).get();
            c.row();
            botTable = c.table().padTop(6).fillY().growX().get();

            if(currentSelect != null){
              Chart.StatGroup upG = statGroups.get(currentSelect).get(PUT), downG = statGroups.get(currentSelect).get(READ);
              botTable.clear();
              botTable.defaults().right().padRight(0).fill();

              botTable.right().button(b -> {
                b.defaults().pad(4);
                b.image().color(Pal.heal).size(24);
                b.add("misc.upload").padLeft(3);
              }, Styles.fullTogglet, () -> {
                if(upG.isShown()){
                  upG.hide();
                }
                else upG.show();
              }).update(b -> b.setChecked(upG.isShown()));

              botTable.right().button(b -> {
                b.defaults().pad(4);
                b.image().color(Pal.accent).size(24);
                b.add("misc.download").padLeft(3);
              }, Styles.fullTogglet, () -> {
                if(downG.isShown()){
                  downG.hide();
                }
                else downG.show();
              }).update(b -> b.setChecked(downG.isShown())).padLeft(5);
            }
          };

          changedChart.run();
        }).grow().get();
      }).grow();
      main.table(Tex.buttonTrans, i -> i.pane(t -> bufferList = t.table().grow().colspan(1).get()).growY().fillX()).padLeft(10).fillX().growY();
    }).grow();

    for(BaseBuffer<?, ?, ?> buffer: targetBuffer){
      childCharts.get(buffer, () -> new LineChart(20));
    }
  }

  public void startStat(){
    for(BaseBuffer<?, ?, ?> buffer: targetBuffer){
      buffer.startCalculate(false);
    }

    rebuild();
  }

  public void endStat(){
    for(BaseBuffer<?, ?, ?> buffer: targetBuffer){
      buffer.endCalculate();
    }
  }

  protected void rebuild(){
    statGroups.clear();
    defChart.clear();
    childCharts.clear();
    detailGroups.clear();
    bufferList.clear();

    currentSelect = null;
    changedChart.run();

    defChart.minValue = defaultMin;
    defChart.maxValue = defaultMax;

    bufferList.defaults().left().width(420).height(50).pad(0).margin(0);
    for(BaseBuffer<?, ?, ?> buffer: targetBuffer){
      ObjectMap<String, Chart.StatGroup> groups = statGroups.get(buffer, ObjectMap::new);
      Chart.StatGroup delta, put, read;

      groups.put(DELTA, delta = defChart.newStatGroup());
      delta.color = buffer.displayColor();

      Prov<LineChart> prov = () -> {
        LineChart r = new LineChart(maxValueCount);
        r.minValue = defaultMin;
        r.maxValue = defaultMax;

        r.horizontal = Core.bundle.get("misc.time");
        r.vertical = SglStatUnit.bytePreSecond.localized();
        return r;
      };

      groups.put(PUT, put = childCharts.get(buffer, prov).newStatGroup());
      put.color = Pal.heal;
      groups.put(READ, read = childCharts.get(buffer, prov).newStatGroup());
      read.color = Pal.accent;

      bufferList.button(b -> {
        b.add(new Element(){
          @Override
          public void draw(){
            super.draw();
            Draw.color(buffer.displayColor());
            Draw.alpha(color.a * parentAlpha);

            Fill.square(x + width/2, y + height/2, width/3, 45);
          }
        }).size(36).left().padLeft(0);
        b.table(f -> {
          f.left().defaults().left().padLeft(0);
          f.add(buffer.localization()).color(Pal.accent);
          f.row();
          f.add("").update(l -> l.setText(Core.bundle.format("infos.bufferIO",
              (buffer.putRate() >= 0? Strings.fixed(buffer.putRate()*60, 1): "--") + SglStatUnit.bytePreSecond.localized(),
              (buffer.readRate() >= 0? Strings.fixed(buffer.readRate()*60, 1): "--") + SglStatUnit.bytePreSecond.localized())));
        }).left().padLeft(6).grow();
      }, Styles.underlineb, () -> {
        setCurrBuffer(currentSelect == buffer? null: buffer);
        buffer.startCalculate(currentSelect != null);
      }).update(b -> b.setChecked(currentSelect == buffer));
      bufferList.row();
      Cell<Table> cell = bufferList.table(items -> {
        items.image().color(Pal.gray).width(4).growY().pad(0);
        items.pane(p -> {
          p.update(() -> {
            if(currentSelect != buffer || !groupCreated) return;
            groupCreated = false;

            p.clearChildren();
            p.defaults().growX().height(50);
            for(BaseBuffer.Packet<?, ?> packet: buffer){
              Chart.StatGroup g = detailGroups.get(packet.get());
              if(g == null) continue;

              p.button(b -> {
                b.image(packet.icon()).size(36).left().padLeft(0);
                b.table(f -> {
                  f.left().defaults().left().padLeft(0);
                  f.add(packet.localization()).color(Pal.accent);
                  f.row();
                  f.add("").update(l -> l.setText(Core.bundle.format("infos.bufferIO",
                      (packet.putRate() >= 0? Strings.fixed(packet.putRate()*60, 1): "--") + SglStatUnit.bytePreSecond.localized(),
                      (packet.readRate() >= 0? Strings.fixed(packet.readRate()*60, 1): "--") + SglStatUnit.bytePreSecond.localized())));
                }).left().padLeft(6).grow();
              }, Styles.underlineb, () -> {
                if(g.isShown()){
                  g.hide();
                }
                else g.show();
              }).update(b -> b.setChecked(g.isShown()));
              p.row();
            }
          });
        }).grow().top();
      }).height(0).growX().top();

      float[] width = {0};
      cell.update(s -> {
        if(currentSelect == buffer){
          width[0] = Mathf.lerpDelta(width[0], 300, 0.045f);
          s.color.a = Mathf.lerpDelta(s.color.a, 1, 0.06f);
        }
        else{
          width[0] = Mathf.lerpDelta(width[0], 0, 0.045f);
          s.color.a = Mathf.lerpDelta(s.color.a, 0, 0.06f);
        }

        cell.height(width[0]);
        s.invalidateHierarchy();
      });

      bufferList.row();
    }
  }

  protected void setCurrBuffer(BaseBuffer<?, ?, ?> buffer){
    chartTable.clearActions();
    currentSelect = buffer;

    chartTable.actions(
        Actions.alpha(0, 0.5f),
        Actions.run(() -> {
          changedChart.run();
          chartTable.setColor(chartTable.color.a(0));

          chartTable.addAction(Actions.alpha(1, 0.5f));
        })
    );
  }

  public void addMonitor(BaseBuffer<?, ?, ?> buffer){
    buffer.startCalculate(false);
    targetBuffer.add(buffer);
  }

  public void removeMonitor(BaseBuffer<?, ?, ?> buffer){
    buffer.endCalculate();
    targetBuffer.remove(buffer);
    statGroups.remove(buffer);
    childCharts.remove(buffer);
    detailGroups.clear();
  }

  public void clear(){
    for(BaseBuffer<?, ?, ?> buffer: targetBuffer){
      buffer.endCalculate();
    }

    targetBuffer.clear();
    statGroups.clear();
    childCharts.clear();
    detailGroups.clear();
  }

  public void setBuffers(Iterable<BaseBuffer<?, ?, ?>> buffers){
    clear();

    for(BaseBuffer<?, ?, ?> buffer: buffers){
      targetBuffer.add(buffer);
    }
  }
}
