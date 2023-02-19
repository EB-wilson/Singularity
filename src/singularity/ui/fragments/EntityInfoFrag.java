package singularity.ui.fragments;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.WidgetGroup;
import arc.struct.ObjectSet;
import arc.struct.OrderedMap;
import arc.struct.OrderedSet;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.input.Binding;
import mindustry.ui.Fonts;
import singularity.Sgl;
import singularity.graphic.SglDrawConst;
import singularity.world.components.ExtraVariableComp;
import universecore.annotations.Annotations;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

public class EntityInfoFrag{
  public OrderedSet<EntityEntry<?>> alphaQueue = new OrderedSet<>(){{
    orderedItems().ordered = false;
  }};
  public OrderedMap<EntityInfoDisplay<?>, Boolf<Entityc>> displayMatcher = new OrderedMap<>();

  public ObjectSet<Entityc> hovering = new ObjectSet<>();
  public boolean showTargetInfo = true;

  Entityc hold;
  float timer, delta;

  boolean wasHold, showRange, mark;
  float holdTime;

  String showModeTip = "";
  float modeTipAlpha;

  boolean resizing;
  float sclAlpha;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void build(WidgetGroup parent){
    parent.fill((x, y, w, h) -> {
      if (!showTargetInfo){
        hovering.clear();
        alphaQueue.clear();

        return;
      }

      update();

      for (int i = alphaQueue.size - 1; i >= 0; i--) {
        EntityEntry<?> entry = alphaQueue.orderedItems().get(i);
        if(entry.alpha <= 0.001f) continue;

        float size = entry.entity instanceof Hitboxc hit ? hit.hitSize()/2 : entry.entity instanceof Buildingc b ? b.block().size / 2f : 10;

        Vec2 v = Core.input.mouse();
        Core.camera.unproject(Tmp.v1.set(v));
        Tmp.v1.y += size;
        float heightOff = Core.camera.project(Tmp.v1).y - v.y + 8;
        float maxWight = 0;

        for (EntityInfoDisplay<?> display : entry.display) {
          maxWight = Math.max(maxWight, display.wight(Sgl.config.showInfoScl));
        }

        for (EntityInfoDisplay<?> display : entry.display) {
          heightOff += ((EntityInfoDisplay)display).draw(entry, Vars.player.team(), maxWight, heightOff, entry.alpha, Sgl.config.showInfoScl);
        }
      }

      if (modeTipAlpha > 0.001) {
        float alpha = 1 - Mathf.pow(1 - modeTipAlpha, 4);
        Fonts.def.draw(showModeTip, Core.graphics.getWidth()/2f, Core.graphics.getHeight()*0.3f - 1f, Tmp.c1.set(Color.gray).a(alpha), Scl.scl(1.4f), true, Align.center);
        Fonts.def.draw(showModeTip, Core.graphics.getWidth()/2f, Core.graphics.getHeight()*0.3f, Tmp.c1.set(Color.white).a(alpha), Scl.scl(1.4f), true, Align.center);
      }

      if (sclAlpha > 0.001) {
        float alpha = 1 - Mathf.pow(1 - sclAlpha, 4);
        String str = Strings.autoFixed(Sgl.config.showInfoScl, 2) + "x";
        Fonts.def.draw(str, Core.graphics.getWidth()/2f, Core.graphics.getHeight()*0.24f - 1f, Tmp.c1.set(Color.gray).a(alpha), Scl.scl(1.4f), true, Align.center);
        Fonts.def.draw(str, Core.graphics.getWidth()/2f, Core.graphics.getHeight()*0.24f, Tmp.c1.set(Color.white).a(alpha), Scl.scl(1.4f), true, Align.center);
      }

      if (showRange){
        Vec2 v = Core.input.mouse();

        Lines.stroke(4, Color.lightGray);
        Draw.alpha(0.3f + Mathf.absin(Time.globalTime, 5, 0.3f));
        Lines.dashCircle(v.x, v.y, Sgl.config.holdDisplayRange);
      }
    });

    Runnable add = () -> {
      Sgl.ui.toolBar.addTool(
          "changeMode",
          () -> Core.bundle.get("infos.changeMode"),
          () -> showRange? SglDrawConst.showRange: wasHold? SglDrawConst.hold: SglDrawConst.defaultShow,
          this::changeMode,
          () -> false
      );
      Sgl.ui.toolBar.addTool(
          "infoScl",
          () -> Core.bundle.get("infos.resizeInfoScl"),
          () -> Icon.resize,
          () -> {
            resizing = !resizing;
            if (!resizing) {
              try {
                Sgl.config.save();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          },
          () -> resizing
      );
    };
    Sgl.ui.toolBar.addTool(
        "showInfos",
        () -> Core.bundle.get(showTargetInfo? "infos.showInfos": "infos.hideInfos"),
        () -> showTargetInfo? SglDrawConst.showInfos: Icon.zoom,
        () -> {
          showTargetInfo = !showTargetInfo;
          if (showTargetInfo){
            add.run();
          }
          else {
            Sgl.ui.toolBar.removeTool("changeMode");
            Sgl.ui.toolBar.removeTool("infoScl");
          }
        },
        () -> showTargetInfo
    );
    add.run();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void update(){
    if (resizing && Core.input.isTouched()){
      sclAlpha = 1;
      Sgl.config.showInfoScl += Core.input.deltaY()/320f;
      Sgl.config.showInfoScl = Mathf.clamp(Sgl.config.showInfoScl, 0.5f, 4f);
    }

    if (Core.input.alt()){
      if (!mark) holdTime = Time.globalTime;
      mark = true;
    }
    else{
      if (mark && Time.globalTime - holdTime < 30){
        changeMode();
      }

      mark = false;
    }

    boolean touched = Core.input.keyDown(Binding.select) && Core.input.alt();
    if (touched){
      hold = null;
      mark = false;
    }

    delta += Time.delta;
    if (Time.globalTime - timer < Sgl.config.flushInterval) return;
    timer = Time.globalTime;

    Vec2 v = Core.input.mouseWorld();

    hovering.clear();
    float dist = 0;
    for (Entityc e : Groups.all) {
      float size = showRange? 0: e instanceof Hitboxc h ? h.hitSize() / 2 : e instanceof Buildingc b ? b.block().size / 2f : 10;

      if (showRange) {
        Core.camera.project(Tmp.v1.set(v));
        Tmp.v1.x += Sgl.config.holdDisplayRange;
      }
      float range = showRange? Math.abs(Core.camera.unproject(Tmp.v1).x - v.x): 0;

      if (e instanceof Posc ent
          && ((!showRange && Math.abs(ent.x() - v.x) < size && Math.abs(ent.y() - v.y) < size)
          || (showRange && ent.dst(v.x, v.y) < range))){
        hovering.add(e);

        if (touched) {
          float dis = ent.dst(v);
          if (dis < dist || hold == null) {
            hold = e;
            dist = dis;
          }
        }
      }
    }

    for (Entityc hover: hovering){
      EntityEntry entry = Pools.obtain(EntityEntry.class, EntityEntry::new);
      entry.entity = hover;
      entry.alpha = 0;

      if (!alphaQueue.contains(entry)) {
        displayMatcher.each((display, cons) -> {
          if (cons.get(hover)) entry.display.add(display);
        });

        if (!entry.display.isEmpty()) {
          alphaQueue.add(entry);
        }
      }
      else Pools.free(entry);
    }

    alphaQueue.orderedItems().sort((a, b) -> {
      if (a.entity instanceof Posc pa && b.entity instanceof Posc pb) {
        Vec2 pos = Core.input.mouseWorld();
        return (int) (pa.dst(pos) - pb.dst(pos));
      }

      return 0;
    });

    int count = 0;
    Iterator<EntityEntry<?>> itr = alphaQueue.iterator();
    while(itr.hasNext()){
      EntityEntry<?> entry = itr.next();
      if (count >= Sgl.config.maxDisplay){
        entry.alpha = Mathf.approach(entry.alpha, 0, 0.025f*delta);
        continue;
      }

      boolean isHovered = entry.entity.isAdded() && ((wasHold && !showRange) || hovering.contains(entry.entity) || hold == entry.entity || Core.input.alt());
      entry.alpha = Mathf.approach(entry.alpha, isHovered ? 1: 0, (isHovered? 0.1f: 0.025f)*delta);

      if (!isHovered && entry.alpha < 0.001f){
        itr.remove();
        Pools.free(entry);
        continue;
      }

      for (EntityInfoDisplay display : entry.display) {
        display.updateVar(entry, delta);
      }

      count++;
    }

    modeTipAlpha = Mathf.approach(modeTipAlpha, 0, 0.004f*delta);
    sclAlpha = Mathf.approach(sclAlpha, 0, 0.004f*delta);
    delta = 0;
  }

  private void changeMode() {
    hold = null;

    if (!wasHold){
      wasHold = true;

      showModeTip = Core.bundle.get("infos.holdShowed");
    }
    else{
      if (showRange){
        wasHold = showRange = false;

        showModeTip = Core.bundle.get("infos.holdOff");
      }
      else{
        showRange = true;

        showModeTip = Core.bundle.get("infos.holdShowRang");
      }
    }
    modeTipAlpha = 1;
  }

  @Annotations.ImplEntries
  public static class EntityEntry<T extends Entityc> implements Pool.Poolable, ExtraVariableComp {
    T entity;
    float alpha;
    Seq<EntityInfoDisplay<T>> display = new Seq<>();

    @Override
    public void reset() {
      entity = null;
      alpha = 0;
      extra().clear();
      display.clear();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof EntityEntry e)) return false;
      return entity == e.entity;
    }

    @Override
    public int hashCode() {
      return Objects.hash(entity);
    }
  }
}
