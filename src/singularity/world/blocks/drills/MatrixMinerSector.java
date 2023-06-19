package singularity.world.blocks.drills;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.graphics.Trail;
import mindustry.type.Item;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.meta.StatUnit;
import singularity.Sgl;
import singularity.graphic.MathRenderer;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.util.MathTransform;
import singularity.world.SglFx;
import singularity.world.meta.SglStat;

import java.util.Iterator;

import static arc.util.Tmp.v1;
import static arc.util.Tmp.v2;
import static mindustry.Vars.tilesize;

public class MatrixMinerSector extends MatrixMinerPlugin{
  public float drillMoveSpeed = 0.05f;
  public Effect drillEffect = SglFx.matrixDrill;

  public float baseDrillTime = 30;


  public MatrixMinerSector(String name){
    super(name);
  }

  @Override
  public void setStats(){
    super.setStats();

    stats.add(SglStat.drillAngle, 90, StatUnit.degrees);
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    super.drawPlace(x, y, rotation, valid);

    if(valid){
      drawArea(x, y);
    }
  }

  public void drawArea(int x, int y){
    for(Point2 edge: Edges.getEdges(size)){
      Tile t = Vars.world.tile(x + edge.x, y + edge.y);
      if(t.build instanceof MatrixMiner.MatrixMinerBuild b && b.team == Vars.player.team() && (b.tile.x == x || b.tile.y == y)){
        int side = b.relativeTo(x, y);

        float l = range *4;
        v1.set(l, 0).setAngle(side*90);
        v2.set(v1).rotate90(1);
        Drawf.dashLine(Pal.accent, b.x, b.y, b.x + v1.x + v2.x, b.y + v1.y + v2.y);
        Drawf.dashLine(Pal.accent, b.x + v1.x - v2.x, b.y + v1.y - v2.y, b.x + v1.x + v2.x, b.y + v1.y + v2.y);
        Drawf.dashLine(Pal.accent, b.x + v1.x - v2.x, b.y + v1.y - v2.y, b.x, b.y);
      }
    }
  }

  public class MatrixMinerSectorBuild extends MatrixMinerPluginBuild{
    public final Vec2 tmp = new Vec2();
    public Vec2 drillPos = new Vec2(), nextPos;

    public int oreIndex = -1;
    public Integer drillingPos = 0;

    public float drillProgress;

    public Trail drillTrail = new Trail(28);

    public byte side = -1;

    public Seq<OreParticle> particles = new Seq<>(512);

    @Override
    public void drawConfigure(){
      super.drawConfigure();

      Draw.color(Pal.accent, 0.1f + Mathf.absin(5, 0.4f));

      float l = owner.drillRange*4;
      v1.set(l, 0).setAngle(side*90);
      v2.set(v1).rotate90(1);
      Fill.tri(
          owner.x, owner.y,
          owner.x + v1.x + v2.x, owner.y + v1.y + v2.y,
          owner.x + v1.x - v2.x, owner.y + v1.y - v2.y
      );
    }

    @Override
    public void setOwner(MatrixMiner.MatrixMinerBuild miner){
      super.setOwner(miner);
      side = miner.relativeTo(this);
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      drillPos.set(tile.drawx(), tile.drawy());
      return super.init(tile, team, shouldAdd, rotation);
    }

    @Override
    public void updateTile(){
      warmup = Mathf.approachDelta(warmup, updateValid() && consumeValid() && nextPos != null? owner.distributor.network.netEfficiency(): 0, 0.04f);
      if(Sgl.config.animateLevel >= 2) drillTrail.update(drillPos.x, drillPos.y);
      super.updateTile();
      if(owner == null) side = -1;

      Iterator<OreParticle> itr = particles.iterator();
      while(itr.hasNext()){
        OreParticle particle = itr.next();
        if(particle.removed){
          itr.remove();
          Pools.free(particle);
          continue;
        }

        particle.update();
      }
    }

    @Override
    public void updatePlugin(MatrixMiner.MatrixMinerBuild owner){
      if(nextPos == null){
        if(oreIndex < 0) oreIndex = Mathf.random(owner.orePosArr.length);

        int c = 0, len = owner.orePosArr.length;

        while(c < len){
          c++;
          oreIndex = (oreIndex + 1)%len;

          drillingPos = owner.orePosArr[oreIndex];
          Tile tile = Vars.world.tile(drillingPos);

          if(tile == null || (!owner.pierce && tile.build != null)
          || Math.abs(tile.x - owner.tileX()) > owner.drillRange/2 || Math.abs(tile.y - owner.tileY()) > owner.drillRange/2
          || !owner.angleValid(Angles.angle(tile.worldx() - owner.x, tile.worldy() - owner.y))) continue;

          Item ore;
          if((ore = owner.ores.get(drillingPos)) != null && owner.drillItem.contains(ore)){
            nextPos = tmp.set(tile.worldx(), tile.worldy());
            break;
          }
        }
      }
      else{
        drillPos.lerpDelta(nextPos, drillMoveSpeed*warmup);

        if(Mathf.len(nextPos.x - drillPos.x, nextPos.y - drillPos.y) <= 6){
          drillProgress += 1/baseDrillTime*consEfficiency()*warmup*owner.boost;

          while(drillProgress >= 1){
            drillProgress--;

            nextPos = null;

            int half = owner.drillSize/2;
            int off = (owner.drillSize + 1)%2;

            int ox = Point2.x(drillingPos) - half + off;
            int oy = Point2.y(drillingPos) - half + off;

            for(int x = 0; x < owner.drillSize; x++){
              for(int y = 0; y < owner.drillSize; y++){
                Tile t = Vars.world.tile(Point2.pack(ox + x, oy + y));
                if(t == null || (!owner.pierce && t.build != null)) continue;

                Item ore = owner.ores.get(t.pos());
                if(ore == null || !owner.drillItem.contains(ore)) continue;

                OreParticle particle = Pools.obtain(OreParticle.class, OreParticle::new);
                particle.color = ore.color;
                particle.sides = Mathf.random(3, 5);
                particle.alpha = 1;
                particle.rotateSpeed = Mathf.random(0.6f, 5);
                particle.speed = Mathf.random(1f, 2.5f);
                particle.position.set(t.worldx(), t.worldy()).add(v1.rnd(Mathf.random(5f)));
                particle.targetPos.set(owner.x, owner.y).add(v1.rnd(Mathf.random(5f)));
                particle.size = Mathf.random(1f, 3f);

                if(Sgl.config.animateLevel >= 3) particles.add(particle);

                drillEffect.at(t.worldx(), t.worldy(), ore.color);

                if(owner.itemBuffer.remainingCapacity() > 0) owner.offload(ore);
              }
            }
          }
        }
      }
    }

    @Override
    public void draw(){
      super.draw();
      drawParticle();
      Draw.z(Layer.effect);
      Fill.circle(x, y, 5*warmup);
      Lines.stroke(1.2f*warmup, SglDrawConst.matrixNet);
      SglDraw.dashCircle(x, y, 8, 6, 180, -Time.time);
      if(updateValid()) drawDrill();
    }

    public void drawParticle(){
      if(Sgl.config.animateLevel < 3) return;

      for(OreParticle particle: particles){
        particle.draw();
      }
    }

    public void drawDrill(){
      if(Sgl.config.animateLevel >= 2) drillTrail.draw(SglDrawConst.matrixNet, 4.5f*warmup);

      Lines.stroke(2.6f*warmup, SglDrawConst.matrixNet);
      Lines.line(x, y, drillPos.x, drillPos.y);

      int half = owner.drillSize/2;
      int off = (owner.drillSize + 1)%2;

      int ox = Point2.x(drillingPos) - half + off;
      int oy = Point2.y(drillingPos) - half + off;

      for(int x = 0; x < owner.drillSize; x++){
        for(int y = 0; y < owner.drillSize; y++){
          Tile t = Vars.world.tile(Point2.pack(ox + x, oy + y));
          if(t == null || (!owner.pierce && t.build != null)) continue;

          Item ore = owner.ores.get(t.pos());
          if(ore == null || !owner.drillItem.contains(ore)) continue;

          Lines.stroke(1*warmup*drillProgress, SglDrawConst.matrixNet);
          Lines.line(drillPos.x, drillPos.y, (ox + x)*tilesize, (oy + y)*tilesize);
          Draw.color(ore.color);
          Fill.circle((ox + x)*tilesize, (oy + y)*tilesize, 2*drillProgress*warmup);
        }
      }

      if(Sgl.config.animateLevel >= 3){
        Draw.draw(Draw.z(), () -> {
          Draw.color(SglDrawConst.matrixNet);
          MathRenderer.setDispersion(0.23f*warmup);
          MathRenderer.setThreshold(0.5f, 0.7f);
          for(int i = 0; i < 4; i++){
            MathRenderer.drawSin(
                x, y, Mathf.randomSeed(id + i, 2f, 3.4f),
                drillPos.x, drillPos.y,
                1.6f,
                Mathf.randomSeed(id + i + 1, 500f, 800f),
                Mathf.randomSeed(id + i + 2, 2f, 3.6f)*Time.time
            );
          }
        });
      }

      Draw.color(SglDrawConst.matrixNet);

      Fill.circle(drillPos.x, drillPos.y, 3*warmup);
      SglDraw.drawDiamond(drillPos.x, drillPos.y, 6 + 14*warmup, 3f*warmup, Time.time);
      SglDraw.drawDiamond(drillPos.x, drillPos.y, 8 + 16*warmup, 4f*warmup, -Time.time*1.2f);

      if(Sgl.config.animateLevel < 2) return;
      Lines.stroke(1.8f*warmup);
      SglDraw.drawCornerTri(
          drillPos.x, drillPos.y,
          24*warmup,
          5*warmup,
          Time.time*1.5f,
          true
      );
    }

    @Override
    public boolean angleValid(float angle){
      return side >= 0 && MathTransform.innerAngle(angle, side*90) < 45;
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.f(drillProgress);
      write.i(oreIndex);
      write.i(drillingPos);
      write.f(drillPos.x);
      write.f(drillPos.y);

      write.bool(nextPos != null);
      if(nextPos != null){
        write.f(nextPos.x);
        write.f(nextPos.y);
      }
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      drillProgress = read.f();
      oreIndex = read.i();
      drillingPos = read.i();
      drillPos.set(read.f(), read.f());

      if(read.bool()){
        nextPos = tmp.set(read.f(), read.f());
      }
    }
  }

  protected static class OreParticle implements Pool.Poolable{
    public Vec2 position = new Vec2(), targetPos = new Vec2();
    public float rotation, alpha;
    public float size, speed, rotateSpeed;
    public int sides;
    public Color color;

    public boolean removed;

    public void update(){
      Vec2 tar = targetPos;

      position.approachDelta(tar, speed);
      rotation += rotateSpeed*Time.delta;

      float distance = Mathf.len(tar.x - position.x, tar.y - position.y);
      alpha = Mathf.clamp(distance/16);

      if(distance <= 2){
        removed = true;
      }
    }

    public void draw(){
      Lines.stroke(0.5f*alpha, color);
      Draw.z(Layer.effect);
      Lines.poly(position.x, position.y, sides, size, rotation);
      Draw.z(Layer.bullet - 1);
      Draw.alpha(0.55f*alpha);
      Fill.poly(position.x, position.y, sides, size, rotation);
      Draw.z(Layer.blockAdditive);
      Draw.reset();
    }

    @Override
    public void reset(){
      position.setZero();
      targetPos.setZero();
      rotation = 0;
      alpha = 0;
      size = 0;
      speed = 0;
      rotateSpeed = 0;
      sides = 0;
      color = null;
      removed = false;
    }
  }
}
