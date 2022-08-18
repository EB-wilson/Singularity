package singularity.world.blocks.distribute;

import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.ObjectSet;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import singularity.graphic.SglDraw;
import singularity.type.Gas;
import singularity.world.components.GasBuildComp;
import singularity.world.components.distnet.DistElementBlockComp;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class MatrixBridge extends DistNetBlock{
  private static final ObjectSet<MatrixBridgeBuild> temps = new ObjectSet<>();
  
  public TextureRegion linkRegion, capRegion;
  
  public float linkRange = 15;
  public float transportItemTime = 1;
  
  public MatrixBridge(String name){
    super(name);
    configurable = true;
    frequencyUse = 0;
    hasItems = true;
    hasLiquids = outputsLiquid = true;
    hasGases = outputGases = true;
  }
  
  @Override
  public void appliedConfig(){
    super.appliedConfig();
    
    config(IntSeq.class, (MatrixBridgeBuild e, IntSeq seq) -> {
      Point2[] ps = new Point2[]{
          seq.get(0) == -1? null: Point2.unpack(seq.get(0)),
          seq.get(1) == -1? null: Point2.unpack(seq.get(1))
      };
      if(ps[0] != null){
        e.linkElementPos = ps[0].set(e.tile.x + ps[0].x, e.tile.y + ps[0].y).pack();
      }
      if(ps[1] != null){
        e.linkNextPos = ps[1].set(e.tile.x + ps[1].x, e.tile.y + ps[1].y).pack();
      }
    });
    
    config(Point2.class, (MatrixBridgeBuild e, Point2 p) -> {
      int pos = p.pack();
      Building target = Vars.world.build(pos);
      if(target instanceof MatrixBridgeBuild){
        if(((MatrixBridgeBuild) target).linkNext == e){
          ((MatrixBridgeBuild) target).linkNextPos = -1;
          ((MatrixBridgeBuild) target).deLink((DistElementBuildComp) e);
          ((MatrixBridgeBuild) target).linkNext = null;
        }
        e.linkNextPos = e.linkNextPos == pos? -1: target.pos();
        e.linkNextLerp = 0;
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
  }
  
  @Override
  public void load(){
    super.load();
    linkRegion = Core.atlas.find(name + "_link");
    capRegion = Core.atlas.find(name + "_cap", (TextureRegion)null);
  }
  
  public boolean canLink(Tile origin, Tile other, float range){
    if(origin == null || other == null) return false;
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
  
    for(BuildPlan plan : links){
      if(plan != null) SglDraw.drawLink(req.tile(), req.block.offset, plan.tile(), plan.block.offset, linkRegion, capRegion, 1);
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
  
  public class MatrixBridgeBuild extends DistNetBuild{
    public float transportCounter;
    public int linkNextPos = -1, linkElementPos = -1;
    public MatrixBridgeBuild linkNext;
    public DistElementBuildComp linkElement;
    public float linkNextLerp, linkElementLerp;
  
    public boolean canLink(Building target){
      if(!MatrixBridge.this.canLink(tile, target.tile, linkRange*Vars.tilesize)) return false;
      if(target instanceof MatrixBridgeBuild && target.block == block) return true;
      return target instanceof DistElementBuildComp;
    }

    @Override
    public void onProximityAdded(){
      super.onProximityAdded();
      onDistNetRemoved();

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
          deLink((DistElementBuildComp) linkNext);
          linkNext = null;
        }
        Building build = linkNextPos == -1? null: world.build(linkNextPos);
        if(build instanceof MatrixBridgeBuild){
          linkNext = (MatrixBridgeBuild) build;
          linkNextPos = linkNext.pos();//对齐偏移距离
          link((DistElementBuildComp) linkNext);
        }
      }
      else if(linkNextPos == -1){
        if(linkNext != null){
          deLink((DistElementBuildComp) linkNext);
          linkNext = null;
        }
        linkNextLerp = 0;
      }
      
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
      if(linkNextPos != -1 && linkNext != null && linkNextLerp < 0.99f) linkNextLerp = Mathf.lerpDelta(linkNextLerp, 1, 0.04f);
      if(linkElementPos != -1 && linkElement != null && linkElementLerp < 0.99f) linkElementLerp = Mathf.lerpDelta(linkElementLerp, 1, 0.04f);
      
      if(consValid() && !distributor.network.netValid()){
        if(linkNext == null){
          doDump();
        }
        else{
          doTransport(linkNext);
        }
      }
    }
    
    public void doDump(){
      dumpAccumulate();
      dumpLiquid();
      dumpGas();
    }
    
    public void doTransport(MatrixBridgeBuild next){
      transportCounter += edelta();
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
  
      moveGas(next);
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
        if(other instanceof DistElementBuildComp){
          if(other instanceof MatrixBridgeBuild || !(linkElement instanceof DistNetworkCoreComp)) configure(Point2.unpack(other.pos()));
        }
        return false;
      }
      
      return true;
    }
  
    @Override
    public void drawConfigure(){
      Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
      Drawf.circles(x, y, linkRange * tilesize);
      float radius;
  
      for(int x = (int)(tile.x - linkRange - 2); x <= tile.x + linkRange + 2; x++){
        for(int y = (int)(tile.y - linkRange - 2); y <= tile.y + linkRange + 2; y++){
          Building link = world.build(x, y);
          if(link != null && link != this && canLink(link)){
            if(linkNext == link || linkElement == link){
              radius = link.block.size * tilesize / 2f + 1f;
              Drawf.square(link.x, link.y, radius, Pal.place);
              Tmp.v1.set(link.x, link.y).sub(this.x, this.y).setLength(radius);
              Drawf.dashLine(Pal.accent,
                  this.x + Tmp.v1.x, this.y + Tmp.v1.y,
                  link.x - Tmp.v1.x, link.y - Tmp.v1.y);
            }
            else{
              if(link.block == block){
                radius = tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f);
                Tmp.v1.set(0, 1).setLength(radius + 4).setAngle(Time.time);
                Drawf.circles(link.x, link.y, radius);
                for(int i=0; i<4; i++){
                  Draw.color(Pal.gray);
                  Fill.poly(link.x + Tmp.v1.x, link.y + Tmp.v1.y, 3, 3.5f, Time.time + i*90 + 60);
                  Draw.color(Pal.accent);
                  Fill.poly(link.x + Tmp.v1.x, link.y + Tmp.v1.y, 3, 1.5f, Time.time + i*90 + 60);
                  Tmp.v1.rotate(90);
                }
              }
            }
          }
        }
      }
  
      Draw.reset();
    }
  
    @Override
    public void draw(){
      Draw.rect(region, x, y);
      if(linkNext != null){
        SglDraw.drawLink(tile, linkNext.getBuilding().tile, linkRegion, capRegion, linkNextLerp);
      }
      if(linkElement != null){
        SglDraw.drawLink(tile, linkElement.getBuilding().tile, linkRegion, capRegion, linkElementLerp);
      }
    }
  
    @Override
    public boolean acceptItem(Building source, Item item){
      return source.interactable(team) && items.get(item) < itemCapacity && (source.block == block || linkNext != null);
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.interactable(team) && liquids.get(liquid) < liquidCapacity && (source.block == block || linkNext != null);
    }
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return source.getBuilding().interactable(team) && pressure() < maxGasPressure && (source.getBlock() == block || linkNext != null);
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
}
