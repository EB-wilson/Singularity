package singularity.game;

import arc.Core;
import arc.Events;
import arc.Graphics;
import arc.func.Boolp;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Structs;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.ui.fragments.HintsFragment;
import mindustry.world.Block;
import mindustry.world.Build;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.world.blocks.distribute.DistNetCore;
import singularity.world.blocks.distribute.MatrixBridge;
import singularity.world.blocks.distribute.matrixGrid.MatrixEdgeBlock;
import singularity.world.blocks.distribute.matrixGrid.MatrixGridBlock;
import singularity.world.blocks.distribute.matrixGrid.MatrixGridCore;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistributeNetwork;
import universecore.components.blockcomp.SpliceBlockComp;

import java.util.ArrayList;

import static mindustry.Vars.player;
import static mindustry.Vars.tilesize;
import static singularity.contents.DistributeBlocks.*;

public class SglHint implements HintsFragment.Hint {
  public static final OrderedMap<Block, Point2> retentionBlocks = new OrderedMap<>();

  public static final Seq<SglHint> all = new Seq<>();

  public static final HintsFragment.Hint[] EMP = new HintsFragment.Hint[0];

  static {
    Events.on(EventType.BlockBuildEndEvent.class, e -> {
      if (!e.breaking && e.unit == player.unit()){
        retentionBlocks.put(e.tile.block(), Point2.unpack(e.tile.pos()));
      }
    });

    Events.on(EventType.ResetEvent.class, e -> {
      retentionBlocks.clear();
    });
  }

  public static final SglHint
      spliceStructure = new SglHint("spliceStructure", 2,
        () -> retentionBlocks.orderedKeys().contains(e -> e instanceof SpliceBlockComp),
        () -> false),

      //matrix distribute network
      matrixCorePlaced = new SglHint("matrixCorePlaced", 2,
        () -> retentionBlocks.containsKey(matrix_core) && matrix_bridge.unlockedNow() && matrix_energy_manager.unlockedNow() && matrix_power_interface.unlockedNow(),
        () -> DistributeNetwork.activityNetwork.orderedItems().contains(DistributeNetwork::netValid)){
        @Override
        public void draw(int page, Color color) {
          Point2 pos = retentionBlocks.get(matrix_core);
          if (!(Vars.world.build(pos.pack()) instanceof DistNetCore.DistNetCoreBuild b) || b.pos() != pos.pack()) return;

          float wx = (pos.x)*tilesize + matrix_core.offset, wy = pos.y*tilesize + matrix_core.offset;

          Lines.stroke(2, Pal.accent);
          Draw.alpha(color.a);
          Lines.square(wx, wy, matrix_core.size*tilesize, 45);

          if (page >= 1) {
            Draw.color();

            float prog = (Time.time%300)/300;
            float al = 1 - Mathf.clamp((prog - 0.7f)/0.22f);

            Draw.alpha(color.a*0.65f*Mathf.clamp(prog/0.22f)*al);
            drawBlock(matrix_bridge, pos.x + 6, pos.y);
            drawBlock(matrix_bridge, pos.x + 10, pos.y);
            drawBlock(matrix_energy_manager, pos.x + 14, pos.y);
            drawBlock(matrix_power_interface, pos.x + 17, pos.y);

            Draw.alpha(color.a*0.65f*Mathf.clamp((prog - 0.4f)/0.22f)*al);
            drawLink(((MatrixBridge) matrix_bridge).linkRegion, null, matrix_bridge, 0, matrix_bridge, 0, pos.x, pos.y, pos.x + 6, pos.y, 1);
            drawLink(((MatrixBridge) matrix_bridge).linkRegion, null, matrix_bridge, 0, matrix_bridge, 0, pos.x + 6, pos.y, pos.x + 10, pos.y, 1);
            drawLink(((MatrixBridge) matrix_bridge).linkRegion, null, matrix_bridge, 0, matrix_bridge, 0, pos.x + 10, pos.y, pos.x + 14, pos.y, 1);
          }
        }
      },
      matrixUsageTip = new SglHint("matrixUsageTip", 2, new HintsFragment.Hint[]{matrixCorePlaced},
          () -> {
            if (!retentionBlocks.containsKey(matrix_core)) return false;
            if (!(Vars.world.build(retentionBlocks.get(matrix_core).pack()) instanceof DistNetCore.DistNetCoreBuild b)) return false;
            return b.distributor().network.netValid();
          }, () -> false){
        @Override
        public void draw(int page, Color color) {
          Point2 pos = retentionBlocks.get(matrix_core);
          if (!(Vars.world.build(pos.pack()) instanceof DistNetCore.DistNetCoreBuild b) || b.pos() != pos.pack()) return;

          float wx = (pos.x)*tilesize + matrix_core.offset, wy = pos.y*tilesize + matrix_core.offset;

          Lines.stroke(2, Pal.accent);
          Draw.alpha(color.a);
          Lines.square(wx, wy, matrix_core.size*tilesize/2f);

          if (page >= 1){
            Draw.color();
            Draw.alpha(color.a*0.7f*(0.5f + Mathf.absin(7, 0.5f)));
            drawBlock(matrix_topology_container, pos.x + 5, (pos.y + 2));
            drawBlock(matrix_topology_container, pos.x + 5, (pos.y - 2));
            drawBlock(matrix_topology_container, pos.x - 5, (pos.y + 2));
            drawBlock(matrix_topology_container, pos.x - 5, (pos.y - 2));
            drawBlock(matrix_process_unit, pos.x + 2, (pos.y + 5));
            drawBlock(matrix_process_unit, pos.x + 2, (pos.y - 4));
            drawBlock(matrix_process_unit, pos.x - 1, (pos.y + 5));
            drawBlock(matrix_process_unit, pos.x - 1, (pos.y - 4));
          }
        }
      },
      matrixGridPlaced = new SglHint("matrixGridPlaced", 2, new HintsFragment.Hint[]{matrixUsageTip},
          () -> DistributeNetwork.activityNetwork.orderedItems()
              .contains(e -> e.netValid() && e.allElem.orderedItems()
                  .contains(el -> el instanceof MatrixGridCore.MatrixGridCoreBuild b && b.block == matrix_controller)
              )
          && retentionBlocks.containsKey(matrix_controller),
          () -> {
            Building build = Vars.world.build(retentionBlocks.get(matrix_controller).pack());
            if (!(build instanceof MatrixGridCore.MatrixGridCoreBuild grid)) return false;

            return grid.gridValid();
          }
      ){
        final Point2[] arr1 = {new Point2(0, 0), new Point2(10, 0), new Point2(10, 10), new Point2(0, 10)};
        final Point2[] arr2 = {new Point2(0, 0), new Point2(7, 0), new Point2(12, 0), new Point2(12, 7), new Point2(12, 12), new Point2(7, 12), new Point2(0, 12), new Point2(0, 7)};
        final Point2[] arr3 = {new Point2(0, 0), new Point2(7, 0), new Point2(14, 0), new Point2(14, 7), new Point2(7, 7), new Point2(7, 14), new Point2(0, 14), new Point2(0, 7)};

        private void drawEdges(Point2 pos, Point2[] arr, float ab, float al, float p){
          for (int i = 0; i < arr.length; i++) {
            Point2 th = arr[i];
            Point2 to = arr[(i + 1)%arr.length];

            Draw.alpha(ab);
            if (th.x != 0 || th.y != 0) drawBlock(matrix_grid_node, pos.x + th.x, pos.y + th.y);

            Draw.alpha(al);
            if (th.x == 0 && th.y == 0){
              drawLink(
                  ((MatrixGridCore) matrix_controller).linkRegion, ((MatrixGridCore) matrix_controller).linkCapRegion,
                  matrix_controller, ((MatrixGridCore) matrix_controller).linkOffset, matrix_grid_node, ((MatrixEdgeBlock) matrix_grid_node).linkOffset,
                  pos.x + th.x, pos.y + th.y, pos.x + to.x, pos.y + to.y, p
              );
            }
            else if (to.x == 0 && to.y == 0){
              drawLink(
                  ((MatrixEdgeBlock) matrix_grid_node).linkRegion, ((MatrixEdgeBlock) matrix_grid_node).linkCapRegion,
                  matrix_grid_node, ((MatrixEdgeBlock) matrix_grid_node).linkOffset, matrix_controller, ((MatrixGridCore) matrix_controller).linkOffset,
                  pos.x + th.x, pos.y + th.y, pos.x + to.x, pos.y + to.y, p
              );
            }
            else {
              drawLink(
                  ((MatrixEdgeBlock) matrix_grid_node).linkRegion, ((MatrixEdgeBlock) matrix_grid_node).linkCapRegion,
                  matrix_grid_node, ((MatrixEdgeBlock) matrix_grid_node).linkOffset, matrix_grid_node, ((MatrixEdgeBlock) matrix_grid_node).linkOffset,
                  pos.x + th.x, pos.y + th.y, pos.x + to.x, pos.y + to.y, p
              );
            }
          }
        }

        @Override
        public void draw(int page, Color color) {
          Point2 pos = retentionBlocks.get(matrix_controller);
          if (!(Vars.world.build(pos.pack()) instanceof MatrixGridCore.MatrixGridCoreBuild b) || b.pos() != pos.pack()) return;

          float wx = (pos.x)*tilesize + matrix_controller.offset, wy = pos.y*tilesize + matrix_controller.offset;

          Lines.stroke(2, Pal.accent);
          Draw.alpha(color.a);
          Lines.square(wx, wy, matrix_controller.size*tilesize/2f);

          if (page >= 1){
            float prog = (Time.time%1200)/1200;
            Draw.color();

            if (prog < 0.33f){
              float p = prog/0.33f;
              float al = 1 - Mathf.clamp((p - 0.7f)/0.22f);

              drawEdges(pos, arr1, color.a*0.65f*Mathf.clamp(p/0.22f)*al, color.a*0.65f*Mathf.clamp((p - 0.4f)/0.22f)*al, Mathf.clamp(p/0.6f));
            }
            else if (prog < 0.66f){
              float p = (prog - 0.33f)/0.33f;
              float al = 1 - Mathf.clamp((p - 0.7f)/0.22f);

              drawEdges(pos, arr2, color.a*0.65f*Mathf.clamp(p/0.22f)*al, color.a*0.65f*Mathf.clamp((p - 0.4f)/0.22f)*al, Mathf.clamp(p/0.6f));
            }
            else {
              float p = (prog - 0.66f)/(1 - 0.66f);
              float al = 1 - Mathf.clamp((p - 0.7f)/0.22f);

              drawEdges(pos, arr3, color.a*0.65f*Mathf.clamp(p/0.22f)*al, color.a*0.65f*Mathf.clamp((p - 0.4f)/0.22f)*al, Mathf.clamp(p/0.6f));
            }
          }
        }
      },
      matrixGridActivating = new SglHint("matrixGridActivating", 3, new HintsFragment.Hint[]{matrixGridPlaced},
          () -> {
            Point2 p = retentionBlocks.get(matrix_controller);
            if (p == null) return false;
            if (!(Vars.world.build(p.pack()) instanceof MatrixGridCore.MatrixGridCoreBuild b) || b.pos() != p.pack()) return false;
            return b.gridValid() && b.distributor().network.netValid();
          },
          () -> DistributeNetwork.activityNetwork.orderedItems().contains(e -> {
            if (!e.netValid()) return false;
            return e.allElem.orderedItems().contains(el -> el instanceof MatrixGridCore.MatrixGridCoreBuild b && b.gridValid() && !b.configs().isEmpty());
          })
      ){
        static final Vec2 tmp = new Vec2();

        @Override
        public void draw(int page, Color color) {
          Point2 pos = retentionBlocks.get(matrix_controller);
          if (!(Vars.world.build(pos.pack()) instanceof MatrixGridCore.MatrixGridCoreBuild b) || b.pos() != pos.pack() || !b.gridValid()) return;

          float wx = (pos.x)*tilesize + matrix_controller.offset, wy = pos.y*tilesize + matrix_controller.offset;

          Lines.stroke(2, Pal.accent);
          Draw.alpha(color.a);
          Lines.square(wx, wy, matrix_controller.size*tilesize/2f);

          if (page == 1){
            float prog = (Time.time%200)/200;
            float al = 1 - Mathf.clamp((prog - 0.8f)/0.12f);
            Draw.color();

            Draw.alpha(color.a*0.65f);
            float[] vecs = b.getEdges().getPoly().getVertices();
            Vec2 p = Geometry.polygonCentroid(vecs, 0, vecs.length, tmp);
            int px = (int) (p.x/tilesize);
            int py = (int) (p.y/tilesize);
            drawBlock(io_point, px, py);

            float pr = Mathf.clamp(prog/0.2f);
            Draw.alpha(color.a*0.65f*pr*al);
            tmp.set(wx + 24*(1 - pr), wy - 18*(1 - pr)).lerp(px*tilesize, py*tilesize, Mathf.clamp((prog - 0.32f)/0.3f));

            float click = prog < 0.3f? Mathf.clamp((prog - 0.22f)/0.05f): Mathf.clamp((prog - 0.65f)/0.05f);
            click = (0.5f - Math.abs(click - 0.5f))*2;
            Draw.rect(SglDrawConst.cursor, tmp.x, tmp.y, 16 - 4*click, 16 - 4*click);
          }
        }
      };

  private static void drawBlock(Block block, int x, int y) {
    Draw.rect(block.region, x*tilesize + block.offset, y*tilesize + block.offset);
  }

  private static void drawLink(TextureRegion linkRegion, TextureRegion cap, Block block, float off, Block toBlock, float toOff, int x, int y, int toX, int toY, float l) {
    SglDraw.drawLink(
      x*tilesize + block.offset, y*tilesize + block.offset, off,
      toX*tilesize + toBlock.offset, toY*tilesize + toBlock.offset, toOff,
      linkRegion, cap, l
    );
  }

  public static void resetCompletedHints(){
    Vars.ui.hints.hints.removeAll(all);
    for (HintsFragment.Hint hint : all) {
      Core.settings.remove(hint.name() + "-hint-done");
    }
    Vars.ui.hints.hints.addAll(all);
  }

  public static void resetAllCompletedHints(){
    Vars.ui.hints.hints.clear();

    Seq<HintsFragment.Hint> list = new Seq<>(HintsFragment.DefaultHint.values());
    list.addAll(all);
    for (HintsFragment.Hint hint : list) {
      Core.settings.remove(hint.name() + "-hint-done");
    }

    Vars.ui.hints.hints.addAll(list);
  }

  private final String name;
  private final String[] localized;

  private final HintsFragment.Hint[] dependencies;
  private final Boolp shouldShow;
  private final Boolp isComplete;
  private final Boolp valid;
  private final int id;

  SglHint(String text, int pages, HintsFragment.Hint[] dependencies, Boolp shouldShow, Boolp isComplete) {
    this(text, pages, dependencies, shouldShow, isComplete, () -> true);
  }

  SglHint(String text, int pages, Boolp shouldShow, Boolp isComplete) {
    this(text, pages, EMP, shouldShow, isComplete, () -> true);
  }

  SglHint(String text, int pages, Boolp shouldShow, Boolp isComplete, Boolp valid) {
    this(text, pages, EMP, shouldShow, isComplete, valid);
  }

  SglHint(String name, int pages, HintsFragment.Hint[] dependencies, Boolp shouldShow, Boolp isComplete, Boolp valid) {
    this.name = name;
    if (pages == 1) localized = new String[]{Core.bundle.get("hints." + name)};
    else {
      this.localized = new String[pages];
      for (int i = 0; i < pages; i++) {
        localized[i] = Core.bundle.get("hints." + name + "-" + i);
      }
    }
    this.dependencies = dependencies;
    this.shouldShow = shouldShow;
    this.isComplete = isComplete;
    this.valid = valid;

    id = HintsFragment.DefaultHint.values().length + all.size;
    all.add(this);
    Vars.ui.hints.hints.add(this);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String text() {
    return localized[0];
  }

  public String text(int index){
    return localized[index];
  }

  public int pages(){
    return localized.length;
  }

  @Override
  public boolean complete() {
    return isComplete.get();
  }

  @Override
  public boolean show() {
    return shouldShow.get() && (dependencies.length == 0 || !Structs.contains(dependencies, d -> !d.finished()));
  }

  @Override
  public int order() {
    return id;
  }

  @Override
  public boolean valid() {
    return valid.get();
  }

  public void draw(int page, Color color){}
}
