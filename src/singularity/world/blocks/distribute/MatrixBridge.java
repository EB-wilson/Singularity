package singularity.world.blocks.distribute;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.Sgl;
import singularity.Singularity;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.world.components.distnet.DistElementBlockComp;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.meta.SglStat;
import singularity.world.modules.SglLiquidModule;

import java.util.Iterator;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class MatrixBridge extends DistNetBlock{
  private static final ObjectSet<MatrixBridgeBuild> temps = new ObjectSet<>();

  public Color effectColor = SglDrawConst.matrixNet;
  public TextureRegion linkRegion, topRegion;
  
  public int linkRange = 16;
  public int maxLinks = 8;
  public float transportItemTime = 1;
  public float linkStoke = 8f;
  public boolean crossLinking = false;

  public float maxLiquidCapacity = -1;

  public MatrixBridge(String name){
    super(name);
    configurable = true;
    topologyUse = 0;
    hasItems = true;
    hasLiquids = outputsLiquid = true;
    isNetLinker = true;
  }

  @Override
  public void appliedConfig(){
    super.appliedConfig();
    
    config(IntSeq.class, (MatrixBridgeBuild e, IntSeq seq) -> {
      if(seq.get(0) != -1){
        Point2 p = Point2.unpack(seq.get(0));
        e.linkElementPos = p.set(e.tile.x + p.x, e.tile.y + p.y).pack();
      }
      if(seq.get(1) != -1){
        Point2 p = Point2.unpack(seq.get(1));
        e.linkNextPos = p.set(e.tile.x + p.x, e.tile.y + p.y).pack();
      }
    });
    
    config(Point2.class, (MatrixBridgeBuild e, Point2 p) -> {
      int pos = p.pack();
      Building target = Vars.world.build(pos);

      if(target instanceof DistNetworkCoreComp){
        e.linkNextPos = -1;
      }

      if(target instanceof MatrixBridgeBuild){
        if(e.linkElement instanceof DistNetworkCoreComp){
          e.linkElementPos = -1;
        }

        if(((MatrixBridgeBuild) target).linkNext == e){
          ((MatrixBridgeBuild) target).linkNextPos = -1;
          ((MatrixBridgeBuild) target).deLink(e);
          ((MatrixBridgeBuild) target).linkNext = null;
        }
        else e.linkNextLerp = 0;
        e.linkNextPos = e.linkNextPos == pos? -1: target.pos();
      }
      else if(target instanceof DistElementBuildComp){
        e.linkElementPos = e.linkElementPos == pos? -1: target.pos();
        e.linkElementLerp = 0;
      }
    });
  }
  
  @Override
  public void init(){
    super.init();
    clipSize = Math.max(clipSize, linkRange * tilesize * 2);
    if(maxLiquidCapacity == -1) maxLiquidCapacity = liquidCapacity*4;
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(Stat.linkRange, linkRange, StatUnit.blocks);
    stats.add(SglStat.maxMatrixLinks, maxLinks);
  }
  
  @Override
  public void load(){
    super.load();
    linkRegion = Core.atlas.find(name + "_link", Singularity.getModAtlas("matrix_link_laser"));
    topRegion = Core.atlas.find(name + "_top", Singularity.getModAtlas("matrix_link_light"));
  }
  
  public boolean linkInlerp(Tile origin, Tile other, float range){
    if(origin == null || other == null) return false;
    if(crossLinking){
      int xDistance = Math.abs(origin.x - other.x),
          yDistance = Math.abs(origin.y - other.y);

      int linkLength = Math.min(linkRange, other.block() instanceof MatrixBridge m? m.linkRange: linkRange);

      return (yDistance < linkLength + size/2f + offset && origin.x == other.x && origin.y != other.y)
          || (xDistance < linkLength + size/2f + offset && origin.x != other.x && origin.y == other.y);
    }
    return Intersector.overlaps(Tmp.cr1.set(origin.drawx(), origin.drawy(), range), other.getHitbox(Tmp.r1));
  }
  
  @Override
  public void drawPlanConfigTop(BuildPlan req, Eachable<BuildPlan> list){
    IntSeq seq = (IntSeq) req.config;
    if(seq == null) return;
    Point2[] p = new Point2[]{
        seq.get(0) == -1? null: Point2.unpack(seq.get(0)),
        seq.get(1) == -1? null: Point2.unpack(seq.get(1))
    };
    BuildPlan[] links = new BuildPlan[2];
    list.each(plan -> {
      if(p[1] != null && plan.block instanceof MatrixBridge){
        if(p[1].cpy().set(req.x + p[1].x, req.y + p[1].y).pack() == Point2.pack(plan.x, plan.y)){
          links[0] = plan;
        }
      }
      else if(p[0] != null && plan.block instanceof DistElementBlockComp){
        if(p[0].cpy().set(req.x + p[0].x, req.y + p[0].y).pack() == Point2.pack(plan.x, plan.y)){
          links[1] = plan;
        }
      }
    });

    Draw.rect(topRegion, req.drawx(), req.drawy());

    for(BuildPlan plan : links){
      if(plan != null){
        SglDraw.drawLaser(
            req.drawx(), req.drawy(),
            plan.drawx(), plan.drawy(),
            linkRegion,
            null,
            linkStoke
        );
      }
    }
  }
  
  public void doCanLink(Tile origin, float range, Cons<MatrixBridgeBuild> cons){
    Geometry.circle(origin.x, origin.y, (int) (range*Vars.tilesize), (x, y) -> {
      Building e = Vars.world.build(x, y);
      if(e instanceof MatrixBridgeBuild && temps.add((MatrixBridgeBuild) e)){
        cons.get((MatrixBridgeBuild) e);
      }
    });
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    super.drawPlace(x, y, rotation, valid);

    if(crossLinking){
      Tmp.v1.set(1, 0);
      for(int i = 0; i < 4; i++){
        float dx = x*tilesize + offset + Geometry.d4x(i)*size*tilesize/2f;
        float dy = y*tilesize + offset + Geometry.d4y(i)*size*tilesize/2f;

        Drawf.dashLine(
            Pal.accent,
            dx,
            dy,
            dx + Geometry.d4x(i)*linkRange*tilesize,
            dy + Geometry.d4y(i)*linkRange*tilesize
        );

        for(int d = 1; d <= linkRange; d++){
          Tmp.v1.setLength(d);
          Building t = world.build(x + (int) Tmp.v1.x, y + (int) Tmp.v1.y);

          if(t != null && linkInlerp(world.tile(x, y), t.tile, linkRange*tilesize) && (t.block instanceof MatrixBridge)){
            Drawf.select(t.x, t.y, t.block.size * tilesize / 2f + 2f + Mathf.absin(Time.time, 4f, 1f), Pal.breakInvalid);
          }
        }
        Tmp.v1.rotate90(1);
      }
    }
    else{
      Lines.stroke(1f);
      Draw.color(Pal.placing);
      Drawf.circles(x*tilesize + offset, y*tilesize + offset, linkRange*tilesize);
    }
  }

  public class MatrixBridgeBuild extends DistNetBuild{
    Rand rand = new Rand();

    public float transportCounter;
    public int linkNextPos = -1, linkElementPos = -1;
    public MatrixBridgeBuild linkNext;
    public DistElementBuildComp linkElement;
    public float linkNextLerp, linkElementLerp;

    public Seq<EffTask> drawEffs = new Seq<>();

    float netEfficiency;

    @Override
    public BlockStatus status() {
      return !enabled? BlockStatus.logicDisable:
          distributor.network.netValid()? BlockStatus.active:
          distributor.network.netStructValid()? distributor.network.topologyUsed <= distributor.network.totalTopologyCapacity? BlockStatus.noInput: BlockStatus.noOutput: consumer.status();
    }

    @Override
    public MatrixBridge block(){
      return MatrixBridge.this;
    }

    public boolean canLink(Building other){
      if(crossLinking){
        if(!(other instanceof MatrixBridgeBuild m)) return false;

        float linkLength = Math.min(linkRange, m.block().linkRange);
        if(m.block().crossLinking){
          int xDistance = Math.abs(tileX() - other.tileX()),
              yDistance = Math.abs(tileY() - other.tileY());

          if(!((yDistance < linkLength + getBlock().size/2f + getBlock().offset && tileX() == other.tileX() && tileY() != other.tileY())
              || (xDistance < linkLength + getBlock().size/2f + getBlock().offset && tileX() != other.tileX() && tileY() == other.tileY())))
            return false;
        }
        else{
          if(!Intersector.overlaps(Tmp.cr1.set(x, y, linkLength*tilesize), other.tile.getHitbox(Tmp.r1))) return false;
        }
      }
      else{
        if(!MatrixBridge.this.linkInlerp(tile, other.tile, linkRange*Vars.tilesize)) return false;
      }
      if(other instanceof MatrixBridgeBuild && other.block == block) return true;
      return other instanceof DistElementBuildComp comp && linkable(comp) && comp.linkable(this);
    }

    @Override
    public void onProximityAdded(){
      super.onProximityAdded();
      updateNetLinked();
    }

    @Override
    public void updateTile(){
      super.updateTile();
      if(linkNextPos != -1 && (linkNext == null || !linkNext.isAdded() || linkNextPos != linkNext.pos())){
        if(linkNext != null){
          if(!linkNext.isAdded()){
            linkNextPos = -1;
          }
          deLink(linkNext);
          linkNext = null;
        }
        Building build = linkNextPos == -1? null: world.build(linkNextPos);
        if(build instanceof MatrixBridgeBuild){
          linkNext = (MatrixBridgeBuild) build;
          linkNextPos = linkNext.pos();//对齐偏移距离
          link(linkNext);
        }
      }
      else if(linkNextPos == -1){
        if(linkNext != null){
          deLink(linkNext);
          linkNext = null;
        }
        linkNextLerp = 0;
      }

      netEfficiency = Mathf.lerpDelta(netEfficiency, drawEff(), 0.02f);
      
      if(linkElementPos != -1 && (linkElement == null || !linkElement.getBuilding().isAdded() || linkElementPos != linkElement.getBuilding().pos())){
        if(linkElement != null){
          if(!linkElement.getBuilding().isAdded()){
            linkElementPos = -1;
          }
          deLink(linkElement);
          linkElement = null;
        }
        Building build = linkElementPos == -1? null: world.build(linkElementPos);
        if(build instanceof DistElementBuildComp){
          linkElement = (DistElementBuildComp) build;
          linkElementPos = linkElement.getBuilding().pos();//对齐偏移距离
          link(linkElement);
        }
      }
      else if(linkElementPos == -1){
        if(linkElement != null){
          deLink(linkElement);
          linkElement = null;
        }
        linkElementLerp = 0;
      }

      if(linkElementPos != -1 && linkElement != null && linkElementLerp < 0.99f){
        linkElementLerp = Mathf.lerpDelta(linkElementLerp, 1, 0.04f);
      }

      if(linkNextPos != -1 && linkNext != null && linkNextLerp < 0.99f){
        linkNextLerp = Mathf.lerpDelta(linkNextLerp, 1, 0.04f);
      }

      if(linkNext == null){
        doDump();
      }
      else if(!distributor.network.netValid() && consumeValid()){
        doTransport(linkNext);
      }

      updateEff();
    }

    @Override
    public void pickedUp(){
      linkElementPos = -1;
      linkNextPos = -1;
      updateTile();
    }

    public void updateEff(){
      Iterator<EffTask> itr = drawEffs.iterator();
      while(itr.hasNext()){
        EffTask eff = itr.next();
        if(eff.progress >= 1){
          itr.remove();
          Pools.free(eff);
        }
        eff.update();
      }

      float scl = (Vars.renderer.getScale() - Vars.renderer.minZoom)/(Vars.renderer.maxZoom - Vars.renderer.minZoom);

      if(Sgl.config.animateLevel < 3) return;

      if(linkNext != null){
        if(rand.random(1f) <= 0.05f*linkNextLerp*netEfficiency*Time.delta*scl){
          makeEff(x, y, linkNext.x, linkNext.y);
        }
      }

      if(linkElement != null){
        if(rand.random(1f) <= 0.05f*linkElementLerp*netEfficiency*Time.delta*scl){
          if(linkElement instanceof DistNetworkCoreComp){
            makeEff(x, y, linkElement.getBuilding().x, linkElement.getBuilding().y);
          }
          else makeEff(linkElement.getBuilding().x, linkElement.getBuilding().y, x, y);
        }
      }
    }

    public void makeEff(float fromX, float fromY, float toX, float toY){
      Tmp.v1.setAngle(rand.random(0f, 360f));
      Tmp.v1.setLength(rand.random(2f, 5f));

      drawEffs.add(EffTask.make(
          fromX + Tmp.v1.x, fromY + Tmp.v1.y,
          toX + Tmp.v1.x, toY + Tmp.v1.y,
          rand.random(0.3f, 1.2f),
          rand.random(0.125f, 0.4f),
          rand.random(180),
          rand.random(-0.6f, 0.6f),
          effectColor
      ));
    }
    
    public void doDump(){
      dumpAccumulate();
      dumpLiquid();
    }
    
    public void doTransport(MatrixBridgeBuild next){
      transportCounter += delta()*consEfficiency();
      while(transportCounter >= transportItemTime){
        Item item = items.take();
        if(item != null){
          if(next.acceptItem(this, item)){
            next.handleItem(this, item);
          }
          else{
            items.add(item, 1);
            items.undoFlow(item);
          }
        }

        transportCounter -= transportItemTime;
      }
      
      liquids.each((l, a) -> {
        moveLiquid(next, l);
      });
    }
  
    @Override
    public boolean onConfigureBuildTapped(Building other){
      if(other == null) return true;
      
      if(other == this){
        if(linkNext != null) configure(Point2.unpack(linkNext.pos()));
        if(linkElement != null) configure(Point2.unpack(linkElement.getBuilding().pos()));
        return true;
      }
      
      if(canLink(other)){
        if(distributor.distNetLinks.size >= maxLinks
        || other instanceof MatrixBridgeBuild o && o.distributor.distNetLinks.size >= o.block().maxLinks) return false;

        if(other instanceof DistElementBuildComp){
          configure(Point2.unpack(other.pos()));
        }
        return false;
      }
      
      return true;
    }

    @Override
    public void drawConfigure(){
      if(crossLinking){
        Drawf.square(x, y, size*tilesize, Pal.accent);

        if(linkNext != null){
          Tmp.v2.set(
                  Tmp.v1.set(linkNext.getBuilding().x, linkNext.getBuilding().y)
                      .sub(x, y)
                      .setLength(size*tilesize/2f))
              .setLength(linkNext.getBlock().size*tilesize/2f);

          Drawf.square(
              linkNext.getBuilding().x,
              linkNext.getBuilding().y,
              linkNext.getBlock().size*tilesize,
              45,
              Pal.accent
          );
          Lines.stroke(3, Pal.gray);
          Lines.line(
              x + Tmp.v1.x,
              y + Tmp.v1.y,
              linkNext.getBuilding().x - Tmp.v2.x,
              linkNext.getBuilding().y - Tmp.v2.y
          );
          Lines.stroke(1, Pal.accent);
          Lines.line(
              x + Tmp.v1.x,
              y + Tmp.v1.y,
              linkNext.getBuilding().x - Tmp.v2.x,
              linkNext.getBuilding().y - Tmp.v2.y
          );
        }
      }
      else{
        Drawf.circles(x, y, tile.block().size*tilesize/2f + 1f + Mathf.absin(Time.time, 4f, 1f));
        Drawf.circles(x, y, linkRange*tilesize);
      }

      Building last = null;
      for(int x = tile.x - linkRange - 2; x <= tile.x + linkRange + 2; x++){
        for(int y = tile.y - linkRange - 2; y <= tile.y + linkRange + 2; y++){
          Building link = world.build(x, y);
          if(last == link) continue;
          last = link;
          if(link != null && link != this && canLink(link)){
            if(linkNext == link || linkElement == link){
              float radius = link.block.size*tilesize/2f + 1f;
              Drawf.square(link.x, link.y, radius, Pal.place);
              Tmp.v1.set(link.x, link.y).sub(this.x, this.y).setLength(radius);
              Drawf.dashLine(Pal.accent,
                  this.x + Tmp.v1.x, this.y + Tmp.v1.y,
                  link.x - Tmp.v1.x, link.y - Tmp.v1.y);
            }else{
              if(link instanceof MatrixBridgeBuild l && canLink(link)){
                drawLinkable(l);
              }
            }
          }
        }
      }

      Draw.reset();
    }

    private void drawLinkable(MatrixBridgeBuild link){
      if(link.linkNext != this){
        float radius = link.block.size*tilesize/2f + 1f + Mathf.absin(Time.time, 4f, 1f);
        Tmp.v1.set(0, 1).setLength(radius + 4).setAngle(Time.time);
        Drawf.circles(link.x, link.y, radius);
        for(int i = 0; i < 4; i++){
          Draw.color(Pal.gray);
          Fill.poly(link.x + Tmp.v1.x, link.y + Tmp.v1.y, 3, 3.5f, Time.time + i*90 + 60);
          Draw.color(Pal.accent);
          Fill.poly(link.x + Tmp.v1.x, link.y + Tmp.v1.y, 3, 1.5f, Time.time + i*90 + 60);
          Tmp.v1.rotate(90);
        }
      }
      else{
        Drawf.select(link.x, link.y, link.block.size * tilesize / 2f + 2f + Mathf.absin(Time.time, 4f, 1f), Pal.breakInvalid);
      }
    }

    @Override
    public void draw(){
      Draw.rect(region, x, y);

      Drawf.light(x, y, 16, Liquids.cryofluid.color, linkNextLerp*0.5f);

      float alp = 0.3f + 0.7f*netEfficiency;
      Draw.alpha(alp*Math.max(linkElementLerp, linkNextLerp));
      Draw.z(netEfficiency > 0.3f? Layer.effect: Layer.blockBuilding + 5f);
      Draw.rect(topRegion, x, y);
      if(linkNext != null){
        Draw.alpha(alp*linkNextLerp);
        Draw.rect(topRegion, linkNext.getBuilding().x, linkNext.getBuilding().y);
        Draw.z(Layer.power);
        SglDraw.drawLaser(
            x, y,
            linkNext.getBuilding().x, linkNext.getBuilding().y,
            linkRegion,
            null,
            linkStoke*linkNextLerp
        );
      }
      if(linkElement != null){
        Draw.alpha(alp*linkElementLerp);
        Draw.z(netEfficiency > 0.3f? Layer.effect: Layer.blockBuilding + 5f);
        Draw.rect(topRegion, linkElement.getBuilding().x, linkElement.getBuilding().y);
        Draw.z(Layer.power);
        SglDraw.drawLaser(
            x, y,
            linkElement.getBuilding().x, linkElement.getBuilding().y,
            linkRegion,
            null,
            linkStoke*linkElementLerp
        );
      }

      drawEffect();
    }

    public float drawEff(){
      return distributor.network.netStructValid()? distributor.network.netEfficiency(): consEfficiency();
    }

    public void drawEffect(){
      if(Sgl.config.animateLevel < 3) return;

      Draw.z(Layer.effect);
      for(EffTask eff: drawEffs){
        eff.draw();
      }
    }

    @Override
    public boolean acceptItem(Building source, Item item){
      return source.interactable(team) && items.get(item) < itemCapacity && (source.block == block || linkNext != null);
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.interactable(team) && liquids.get(liquid) < liquidCapacity && ((SglLiquidModule)liquids).total() < maxLiquidCapacity
          && (source.block == block || linkNext != null);
    }

    @Override
    public IntSeq config(){
      Point2 t = linkElementPos == -1? null: Point2.unpack(linkElementPos);
      Point2 m = linkNextPos == -1? null: Point2.unpack(linkNextPos);
      return IntSeq.with(
              t == null? -1: t.set(t.x - tile.x, t.y - tile.y).pack(),
              m == null? -1: m.set(m.x - tile.x, m.y - tile.y).pack()
      );
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      linkNextLerp = read.f();
      linkElementLerp = read.f();
      transportCounter = read.f();
      linkNextPos = read.i();
      linkElementPos = read.i();
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      write.f(linkNextLerp);
      write.f(linkElementLerp);
      write.f(transportCounter);
      write.i(linkNextPos);
      write.i(linkElementPos);
    }
  }

  public static class EffTask implements Pool.Poolable{
    float fromX, fromY;
    float toX, toY;
    float length;
    float radius;

    float speed;
    float angleSpeed;

    float rotate;
    float progress;

    Color color;

    public static EffTask make(float fromX, float fromY, float toX, float toY, float radius, float speed, float defAngle, float angleSpeed, Color color){
      EffTask res = Pools.obtain(EffTask.class, EffTask::new);
      res.fromX = fromX;
      res.fromY = fromY;
      res.toX = toX;
      res.toY = toY;
      res.length = Mathf.len(toX - fromX, toY - fromY);
      res.radius = radius;
      res.speed = speed;
      res.rotate = defAngle;
      res.angleSpeed = angleSpeed;
      res.color = color;

      return res;
    }

    public void update(){
      progress += length == 0? 0: speed/length*Time.delta;
      rotate += angleSpeed*Time.delta;
    }

    public void draw(){
      Tmp.v1.set(toX - fromX, toY - fromY);
      Tmp.v1.scl(progress);

      Draw.color(color);
      Draw.alpha(Mathf.clamp((progress > 0.5? (1 - progress): progress)/0.15f));
      Fill.square(fromX + Tmp.v1.x, fromY + Tmp.v1.y, radius, rotate);
    }

    @Override
    public void reset(){
      fromX = fromY = toX = toY = 0;
      length = 0;
      radius = 0;
      speed = 0;
      angleSpeed = 0;

      rotate = progress = 0;

      color = null;
    }
  }
}
