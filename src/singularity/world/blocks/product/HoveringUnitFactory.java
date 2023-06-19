package singularity.world.blocks.product;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmaps;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Sized;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.units.RepairTurret;
import mindustry.world.draw.DrawDefault;
import singularity.graphic.MathRenderer;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.util.MathTransform;
import singularity.world.SglFx;
import universecore.world.producers.ProducePayload;
import universecore.world.producers.ProduceType;

import static mindustry.Vars.tilesize;
import static mindustry.type.UnitType.shadowTX;
import static mindustry.type.UnitType.shadowTY;

public class HoveringUnitFactory extends SglUnitFactory{
  public float outputRange = 0;

  public float defHoverRadius;
  public float laserOffY;
  public float hoverMoveMinRadius, hoverMoveMaxRadius;
  public float beamWidth = 0.6f;
  public float pulseRadius = 3f;
  public float pulseStroke = 1f;
  public String hoverTextureSuffix = "_hover";

  public TextureRegion laser;
  public TextureRegion laserEnd;
  public TextureRegion laserTop;
  public TextureRegion laserTopEnd;
  public TextureRegion hover;

  public HoveringUnitFactory(String name) {
    super(name);

    draw = new DrawDefault();
  }

  @Override
  public void load() {
    super.load();
    hover = new TextureRegion(new Texture(Pixmaps.outline(Core.atlas.getPixmap(name + hoverTextureSuffix), Pal.darkOutline, 3)));
    laser = Core.atlas.find("laser-white");
    laserEnd = Core.atlas.find("laser-white-end");
    laserTop = Core.atlas.find("laser-top");
    laserTopEnd = Core.atlas.find("laser-top-end");
  }

  @Override
  public void init() {
    super.init();
    configurable |= outputRange > size*tilesize;
    rotate = outputRange <= size*tilesize;
  }

  public class HoveringUnitFactoryBuild extends SglUnitFactoryBuild{
    public final Vec2 payloadReleasePos = new Vec2();
    public final HoveringStat[] hoveringStats = new HoveringStat[4];

    @Nullable public Building currentOutputTarget;

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation) {
      super.init(tile, team, shouldAdd, rotation);
      for (int i = 0; i < hoveringStats.length; i++) {
        Point2 p = Geometry.d4(i);

        HoveringStat stat = hoveringStats[i] = new HoveringStat();
        stat.idOff = i;

        stat.defx = x + p.x*defHoverRadius;
        stat.defy = y + p.y*defHoverRadius;

        stat.pos.set(stat.defx, stat.defy);

        stat.angelVec.set(p.x, p.y).scl(-1);
      }

      return this;
    }

    @Override
    public boolean onConfigureTapped(float x, float y) {
      float dst = dst(x, y);
      if (dst > outputRange) return false;
      else if (dst < size*tilesize/2f){
        payloadReleasePos.setZero();
        return true;
      }
      else {
        Building building = Vars.world.buildWorld(x, y);

        if (building != null && (building.block.acceptsPayload || building.block.outputsPayload) && building.interactable(team)){
          payloadReleasePos.set(building.x - this.x, building.y - this.y);
        }
        else payloadReleasePos.set(x - this.x, y - this.y);
        return true;
      }
    }

    @Override
    public void drawConfigure() {
      super.drawConfigure();
      if (outputRange > size*tilesize) Drawf.circles(x, y, outputRange, Pal.accent);

      if (Math.abs(payloadReleasePos.x) > size*tilesize/2f || Math.abs(payloadReleasePos.y) > size*tilesize/2f){
        float dx = x + payloadReleasePos.x;
        float dy = y + payloadReleasePos.y;
        Building building = Vars.world.buildWorld(dx, dy);

        Drawf.line(Pal.accent, x, y, dx, dy);
        Drawf.square(dx, dy, 14, 45);

        float lerp = (Time.time%120f)/120f;
        Lines.stroke(3*(1 - lerp), Pal.accent);
        Lines.square(dx, dy, 14 + 48*lerp, 45);

        if (building != null && (building.block.acceptsPayload || building.block.outputsPayload)){
          Draw.rect(Icon.download.getRegion(), dx, dy);
          Drawf.square(building.x, building.y, building.block.size*tilesize*1f + Mathf.absin(4, 8), 45, Pal.accent);
        }
        else if (getCurrentTask() != null && (building == null || !building.interactable(team) || getCurrentTask().buildUnit.flying || getCurrentTask().buildUnit.canBoost || !building.block.solid)){
          Drawf.square(dx, dy, 8, MathTransform.gradientRotateDeg(Time.time, 45), Pal.accent);
        }
        else if (getCurrentTask() != null){
          Draw.color(Pal.accent, Color.crimson, Mathf.absin(4, 1));
          Draw.rect(Icon.warning.getRegion(), dx, dy);
        }
      }

      Draw.reset();
    }

    @Override
    public Vec2 outputtingOffset() {
      return payloadReleasePos;
    }

    @Override
    public void craftTrigger() {
      super.craftTrigger();
      Tmp.v1.set(3, 0).setAngle(Mathf.randomSeed(id, 360f) - Time.time);
      if (!payloads().isEmpty()) getPayload().set(x + Tmp.v1.x, y + Tmp.v1.y, 90);
    }

    @Override
    public void drawConstructingPayload() {
      float z = Draw.z();

      Draw.z(Layer.flyingUnit - 1);
      ProducePayload<?> p;
      if (producer.current != null && (p = producer.current.get(ProduceType.payload)) != null){
        Tmp.v1.set(3, 0).setAngle(Mathf.randomSeed(id, 360f) - Time.time);
        float dx = x + Tmp.v1.x, dy = y + Tmp.v1.y;
        Draw.draw(Draw.z(), () -> Drawf.construct(dx, dy, p.payloads[0].item.fullIcon, 0, progress(), warmup(), totalProgress()%(20*Mathf.PI2)));

        Draw.z(Math.min(Layer.darkness, z - 1f));
        float x = dx + shadowTX, y = dy + shadowTY;

        Draw.color(Pal.shadow, Pal.shadow.a*progress());
        Draw.rect(p.payloads[0].item.fullIcon, x, y);
        Draw.color();
      }

      Draw.z(z);
    }

    @Override
    public void released(Payload payload) {
      SglFx.spreadDiamond.at(payload.x(), payload.y(), payload.size(), team.color);
    }

    @Override
    public void drawPayload() {
      if (outputting() != null) {
        float dst = dst(outputting());
        float lerp = Mathf.clamp(dst/(size*tilesize));

        float prog = dst/payloadReleasePos.len();
        Building tar = currentOutputTarget;

        Draw.color(team.color);
        Draw.alpha(0.6f*lerp);

        Lines.stroke(1.6f*lerp);
        Lines.circle(outputting().x(), outputting().y(), outputting().size());

        Draw.draw(Draw.z(), () -> {
          MathRenderer.setDispersion((0.18f + Mathf.absin(Time.time/3f, 6, 0.4f))*lerp*Mathf.clamp((1 - prog)/0.5f));
          MathRenderer.setThreshold(0.4f, 0.8f);
          MathRenderer.drawSin(x, y, 6, outputting().x(), outputting().y(), 5, 120, -2.5f*Time.time);
          MathRenderer.drawSin(x, y, 6, outputting().x(), outputting().y(), 5, 150, -3.2f*Time.time);
        });

        float z = Draw.z();
        Draw.z(Math.min(Layer.darkness, z - 1f));
        float sh = outputting() instanceof UnitPayload u && u.unit.type.flying && tar == null? 1: 1 - lastOutputProgress;
        float x = outputting().x() + shadowTX*sh, y = outputting().y() + shadowTY*sh;

        Draw.color(Pal.shadow);
        Draw.rect(outputting().icon(), x, y, outputting().rotation() - 90);
        Draw.color();
        Draw.z(z);
      }

      super.drawPayload();
    }

    @Override
    public void draw() {
      super.draw();

      drawPayload();
      drawConstructingPayload();

      for (HoveringStat stat : hoveringStats) {
        stat.draw();
      }
    }

    @Override
    public void updateTile() {
      super.updateTile();

      float dx = x + payloadReleasePos.x;
      float dy = y + payloadReleasePos.y;
      currentOutputTarget = Vars.world.buildWorld(dx, dy);

      //对齐方块输出坐标
      if (currentOutputTarget != null && currentOutputTarget.interactable(team)
      && (currentOutputTarget.block.acceptsPayload || currentOutputTarget.block.outputsPayload)){
        payloadReleasePos.set(currentOutputTarget.x - x, currentOutputTarget.y - y);
      }
      else currentOutputTarget = null;

      for (HoveringStat stat : hoveringStats) {
        stat.update();
      }
    }

    @Override
    public void write(Writes write) {
      super.write(write);

      write.f(payloadReleasePos.x);
      write.f(payloadReleasePos.y);
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);

      payloadReleasePos.set(
          read.f(),
          read.f()
      );
    }

    Sized mark = new Sized(){
      @Override
      public float hitSize() {
        return getCurrentTask() != null? getCurrentTask().buildUnit.hitSize: 0;
      }

      @Override
      public float getX() {
        return x + Angles.trnsx(Mathf.randomSeed(id, 360f) - Time.time, 3, 0);
      }

      @Override
      public float getY() {
        return y + Angles.trnsy(Mathf.randomSeed(id, 360f) - Time.time, 3, 0);
      }
    };

    public class HoveringStat{
      public float defx, defy;
      public int idOff;
      public final Vec2 pos = new Vec2();
      public final Vec2 targetPos = new Vec2();
      public final Vec2 angelVec = new Vec2();
      public final Vec2 halfMark = new Vec2();

      public final Vec2 last = new Vec2();
      public final Vec2 offset = new Vec2();

      public float lerp, off;

      public void update(){
        if (Mathf.chanceDelta(0.004f*lerp) || targetPos.isZero()){
          targetPos.set(Mathf.random(hoverMoveMinRadius, hoverMoveMaxRadius), 0).setAngle(Mathf.random(360f));
        }

        pos.lerpDelta(Mathf.lerp(defx, x + targetPos.x, warmup()), Mathf.lerp(defy, y + targetPos.y, warmup()), 0.02f);
        lerp = Mathf.approachDelta(lerp, 1 - Mathf.clamp(Mathf.len(x + targetPos.x - pos.x, y + targetPos.y - pos.y)/24), 0.03f);

        off += Time.delta*lerp*2;
        Tmp.v1.set(3f, 0).setAngle(off).scl(lerp);
        pos.add(Tmp.v1);

        Tmp.v1.set(pos.x - x, pos.y - y).scl(-1).nor();
        angelVec.lerpDelta(Tmp.v1, 0.03f);

        if (halfMark.isZero()){
          //初始化半长控制点的位置
          halfMark.set(x + (pos.x - x)/2f, y + (pos.y - y)/2f);
        }
        else halfMark.lerpDelta(x + (pos.x - x)/2f, y + (pos.y - y)/2f, 0.04f);

        if (Mathf.chanceDelta(0.07f*lerp*warmup())){
          SglFx.constructSpark.at(last.x, last.y, SglDrawConst.matrixNetDark);
        }

        if (Mathf.chanceDelta(0.08f*lerp*warmup())){
          SglFx.moveDiamondParticle.at(pos.x, pos.y, Tmp.v1.angle(), SglDrawConst.matrixNetDark, Mathf.len(pos.x - x, pos.y - y));
        }
      }

      public void draw(){
        float z = Draw.z();

        Draw.color(team.color, 0.4f + Mathf.absin(5, 0.2f));
        Lines.stroke((1 + Mathf.absin(6, 0.5f))*warmup());

        float x1 = (defx + halfMark.x)/2, y1 = (defy + halfMark.y)/2;
        float x2 = (pos.x + halfMark.x)/2, y2 = (pos.y + halfMark.y)/2;

        Tmp.v1.set(4 + Mathf.absin(4, 3), 0).setAngle(off);

        Lines.curve(defx, defy, x1 + Tmp.v1.x, y1 + Tmp.v1.y, x2 - Tmp.v1.x, y2 - Tmp.v1.y, pos.x, pos.y, Math.max(18, (int)(Mathf.dst(x, y, pos.x, pos.y) / 6)));

        Draw.alpha(1);
        Draw.z(Layer.effect);
        Fill.circle(defx, defy, 1.6f*warmup());

        Draw.color();

        Draw.z(Layer.flyingUnit);
        float rot = angelVec.angle();
        Draw.rect(hover, pos.x, pos.y, rot - 90);

        SglDraw.drawTransform(pos.x, pos.y, laserOffY, 0, angelVec.angle(), (x, y, r) -> {
          RepairTurret.drawBeam(x, y, r, 4, id + idOff, getCurrentTask() != null? mark: null, team, warmup(),
              pulseStroke, pulseRadius, beamWidth, last, offset, team.color, Color.white,
              laser, laserEnd, laserTop, laserTopEnd
          );
        });
        Draw.z(z);
      }
    }
  }
}
