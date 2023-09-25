package singularity.world.blocks.nuclear;

import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.WindowedMean;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;
import arc.struct.IntFloatMap;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.*;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.input.Placement;
import mindustry.io.TypeIO;
import mindustry.world.Block;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.graphic.MathRenderer;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.world.components.NuclearEnergyBuildComp;

import static mindustry.Vars.*;
import static mindustry.core.Renderer.laserOpacity;

public class NuclearNode extends NuclearBlock{
  protected static final Seq<NuclearEnergyBuildComp> tempNuclearEntity = new Seq<>();
  protected static final Rand rand = new Rand();
  protected BuildPlan otherReq;

  public float lightRadius = 3.5f;
  public float linkLeaserStroke = 2f;

  /**节点的最大连接范围*/
  public float linkRange = 16;
  /**最大连接数*/
  public int maxLinks = 10;
  public float moveAlphaRate = 8;
  
  public Color linkColor = SglDrawConst.matrixNet;
  public Color linkColor2 = SglDrawConst.fexCrystal;
  public float linkGradientScl = 20;

  private int returnInt;

  private final int timeID = timers++;

  @Nullable public NuclearNodeBuild lastPlaced;

  public NuclearNode(String name){
    super(name);
    drawDisabled = false;
    schematicPriority = -10;
    envEnabled |= Env.space;
    swapDiagonalPlacement = true;
    energyCapacity = 60;
    
    configurable = true;
  }
  
  @Override
  public void appliedConfig(){
    super.appliedConfig();
    config(Point2.class, (NuclearNodeBuild entity, Point2 value) -> {
      Tile tile = Vars.world.tile(value.x, value.y);
      if(tile == null || !(tile.build instanceof NuclearEnergyBuildComp other) || !((NuclearEnergyBuildComp)tile.build).hasEnergy()) return;

      value.x = tile.x;
      value.y = tile.y;

      if(entity.linked.contains(value.pack())){
        entity.deLink(other);
      }
      else{
        if(entity.linksCount() >= maxLinks) return;
        entity.link(other);
      }
    });
  }
  
  @Override
  public void init(){
    super.init();
    clipSize = Math.max(clipSize, linkRange*tilesize*2);
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(Stat.linkRange, linkRange, StatUnit.blocks);
    stats.add(Stat.maxConsecutive, maxLinks);
  }

  @Override
  public void drawPlanConfigTop(BuildPlan req, Eachable<BuildPlan> list){
    if(req.config instanceof Point2[] plans){
      for(Point2 point : plans){
        int px = req.x + point.x, py = req.y + point.y;
        otherReq = null;
        list.each(other -> {
          if(other.block != null
              && (px >= other.x - ((other.block.size-1)/2) && py >= other.y - ((other.block.size-1)/2) && px <= other.x + other.block.size/2 && py <= other.y + other.block.size/2)
              && other != req && other.block instanceof NuclearBlock){
            otherReq = other;
          }
        });
        
        if(otherReq == null || otherReq.block == null) continue;

        setLaserColor();
        Fill.circle(req.drawx(), req.drawy(), lightRadius);
        drawLink(req.drawx(), req.drawy(), size, otherReq.drawx(), otherReq.drawy(), otherReq.block.size);
      }
      Draw.color();
    }
  }

  @Override
  public void changePlacementPath(Seq<Point2> points, int rotation, boolean diagonalOn) {
    Placement.calculateNodes(points, this, rotation, (point, other) -> inRange(world.tile(point.x, point.y), world.tile(other.x, other.y), linkRange*tilesize));
  }

  public void drawLink(NuclearEnergyBuildComp entity, NuclearEnergyBuildComp other){
    drawLink(entity.getBuilding().x, entity.getBuilding().y, entity instanceof NuclearNodeBuild? 0: size, other.getBuilding().x, other.getBuilding().y, other instanceof NuclearNodeBuild? 0: other.getBlock().size);
    if (!(other instanceof NuclearNodeBuild)){
      Fill.circle(Tmp.v1.x, Tmp.v1.y, lightRadius/2);
    }
  }
  
  public void drawLink(float x1, float y1, int size1, float x2, float y2, int size2){
    Draw.z(Layer.effect);
    setLaserColor();

    Tmp.v1.set(x1, y1).sub(x2, y2).setLength(size1*tilesize/2f - 1.5f).scl(-1);
    Tmp.v2.set(x2, y2).sub(x1, y1).setLength(size2*tilesize/2f - 1.5f).scl(-1);

    float xs = x1 + Tmp.v1.x, ys = y1 + Tmp.v1.y;
    float xo = x2 + Tmp.v2.x, yo = y2 + Tmp.v2.y;

    Tmp.v1.set(xo, yo);
  
    Lines.stroke(linkLeaserStroke*laserOpacity);
    Lines.line(xs, ys, xo, yo, false);
    Lines.stroke(1f);
  }

  public void setLaserColor() {
    Draw.color(linkColor, linkColor2, Mathf.absin(linkGradientScl, 1));
  }

  @Override
  public void changePlacementPath(Seq<Point2> points, int rotation){
    Placement.calculateNodes(points, this, rotation, (point, other) -> inRange(world.tile(point.x, point.y), world.tile(other.x, other.y), linkRange*tilesize));
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    Tile tile = world.tile(x, y);
    
    if(tile == null) return;
    
    Lines.stroke(1f);
    Draw.color(Pal.placing);
    Drawf.circles(x * tilesize + offset, y * tilesize + offset, linkRange * tilesize);

    getPotentialLink(tile, player.team(), other -> {
      setLaserColor();
      Draw.alpha(0.5f);

      if (!(other instanceof NuclearNodeBuild)) {
        Fill.circle(Tmp.v1.x, Tmp.v1.y, lightRadius / 2);
      }

      float len = Mathf.len(other.getBuilding().x - tile.worldx() + offset, other.getBuilding().y - tile.worldy() + offset);

      Drawf.arrow(tile.worldx() + offset, tile.worldy() + offset,
          other.getBuilding().x, other.getBuilding().y, len*(Time.time%120)/120, linkLeaserStroke*2, linkColor);
      Drawf.line(linkColor, tile.worldx() + offset, tile.worldy() + offset, other.getBuilding().x, other.getBuilding().y);
      Drawf.square(other.getBuilding().x, other.getBuilding().y, other.getBlock().size * tilesize / 2f + 2f, Pal.place);
    });

    if (lastPlaced != null && lastPlaced.isAdded()
        && inRange(tile, lastPlaced.tile, linkRange*tilesize)
        && inRange(lastPlaced.tile, tile, lastPlaced.block().linkRange*tilesize)
        && lastPlaced.linksCount() < lastPlaced.block().maxLinks){
      setLaserColor();
      Draw.alpha(0.5f);

      Drawf.dashLine(linkColor, tile.worldx() + offset, tile.worldy() + offset, lastPlaced.x, lastPlaced.y);
      Drawf.circles(lastPlaced.x, lastPlaced.y, lastPlaced.block.size*tilesize/1.8f + Mathf.absin(4, 2));
    }
    
    Draw.reset();
  }

  public void getPotentialLink(Tile tile, Team team, Cons<NuclearEnergyBuildComp> cons){
    Boolf<NuclearEnergyBuildComp> valid = other -> other != null && other.getBuilding().tile() != tile && other.energy() != null
        && other.consumeEnergy() && inRange(tile, other.getBuilding().tile(), linkRange * tilesize)
        && other.getBuilding().team == team
        && !Structs.contains(Edges.getEdges(size), p -> { //do not link to adjacent buildings
          Tile t = world.tile(tile.x + p.x, tile.y + p.y);
          return t != null && t.build == other;
        });

    tempNuclearEntity.clear();

    Geometry.circle(tile.x, tile.y, (int)(linkRange + 2), (x, y) -> {
      Building other = world.build(x, y);
      if(!(other instanceof NuclearEnergyBuildComp)) return;
      if(valid.get((NuclearEnergyBuildComp) other) && !tempNuclearEntity.contains((NuclearEnergyBuildComp) other)){
        tempNuclearEntity.add((NuclearEnergyBuildComp) other);
      }
    });

    tempNuclearEntity.sort((a, b) -> Float.compare(a.getBuilding().dst2(tile), b.getBuilding().dst2(tile)));

    returnInt = 0;
    tempNuclearEntity.each(valid, e -> {
      if(returnInt++ < maxLinks){
        cons.get(e);
      }
    });
  }
  
  public static Seq<NuclearEnergyBuildComp> getNodeLinks(Tile tile, Block block, Team team){
    Boolf<NuclearEnergyBuildComp> valid = other -> other != null && other.getBuilding().tile() != tile
        && other instanceof NuclearNodeBuild n && n.linksCount() < other.getBlock(NuclearNode.class).maxLinks
        && other.getBlock(NuclearNode.class).inRange(other.getBuilding().tile, tile, other.getBlock(NuclearNode.class).linkRange*tilesize)
        && other.getBuilding().team == team
        && !Structs.contains(Edges.getEdges(block.size), p -> {
          Tile t = world.tile(tile.x + p.x, tile.y + p.y);
          return t != null && t.build == other;
        });
  
    tempNuclearEntity.clear();
    
    Geometry.circle(tile.x, tile.y, 20, (x, y) -> {
      Building other = world.build(x, y);
      if(other instanceof NuclearEnergyBuildComp){
        if(valid.get((NuclearEnergyBuildComp) other) && !tempNuclearEntity.contains((NuclearEnergyBuildComp) other)){
          tempNuclearEntity.add((NuclearEnergyBuildComp) other);
        }
      }
    });
  
    tempNuclearEntity.sort((a, b) -> {
      int type = -Boolean.compare(a.getBlock() instanceof NuclearNode, b.getBlock() instanceof NuclearNode);
      if(type != 0) return type;
      return Float.compare(a.getBuilding().dst2(tile), b.getBuilding().dst2(tile));
    });
   
    Seq<NuclearEnergyBuildComp> seq = new Seq<>();
    tempNuclearEntity.each(valid, seq::add);
    
    return seq;
  }

  public boolean inRange(Tile origin, Tile other, float range){
    if(origin == null || other == null) return false;
    return Intersector.overlaps(Tmp.cr1.set(origin.drawx(), origin.drawy(), range), other.getHitbox(Tmp.r1));
  }
  
  public boolean inRange(Building origin, Building other, float range){
    return inRange(origin.tile, other.tile, range);
  }
  
  public boolean inRange(NuclearEnergyBuildComp origin, NuclearEnergyBuildComp other, float range){
    return inRange(origin.getBuilding(), other.getBuilding(), range);
  }
  
  /**判断从一个点到另一个点是否可以进行核能连接*/
  public boolean canLink(NuclearEnergyBuildComp from, NuclearEnergyBuildComp to){
    if(from.getBuilding().team != to.getBuilding().team || from == to || !from.hasEnergy() || !to.hasEnergy()) return false;
    return inRange(from, to, linkRange*tilesize) || (to instanceof NuclearNodeBuild && inRange(to, from, ((NuclearNodeBuild) to).block().linkRange*tilesize));
  }
  
  public class NuclearNodeBuild extends SglBuilding{
    public final IntFloatMap smoothAlpha = new IntFloatMap();
    public final IntFloatMap chanceFlow = new IntFloatMap();
    public final IntFloatMap flowing = new IntFloatMap();
    public final IntMap<WindowedMean> flowMean = new IntMap<>();

    public IntSeq linked = new IntSeq(){{ordered = false;}};
    public IntSeq linkThis = new IntSeq(){{ordered = false;}};

    @Override
    public NuclearNode block(){
      return (NuclearNode)super.block();
    }

    public void link(NuclearEnergyBuildComp target){
      int linkingPos = target.getBuilding().pos();

      if (linked.contains(linkingPos)) return;

      if (linkThis.contains(linkingPos)) {
        linkThis.removeValue(linkingPos);
        if (target instanceof NuclearNodeBuild n) n.linked.removeValue(pos());
      }

      linked.add(linkingPos);
      if (target instanceof NuclearNodeBuild n) n.linkThis.add(pos());
    }

    public void deLink(NuclearEnergyBuildComp target){
      int delinkingPos = target.getBuilding().pos();

      if (!linked.contains(delinkingPos)) return;

      linked.removeValue(delinkingPos);
      if (target instanceof NuclearNodeBuild n) n.linkThis.removeValue(pos());
    }

    public int linksCount() {
      return linked.size + linkThis.size;
    }
  
    @Override
    public void placed(){
      if(net.client()) return;

      getPotentialLink(tile, team, e -> {
        if(!linked.contains(e.getBuilding().pos())) configure(Point2.unpack(e.getBuilding().pos()));
      });

      if (lastPlaced != null && lastPlaced.isAdded()){
        if (canLink(this, lastPlaced)){
          lastPlaced.configure(Point2.unpack(pos()));
        }
      }

      lastPlaced = this;
  
      super.placed();
    }

    @Override
    public boolean onConfigureBuildTapped(Building other){
      if(!(other instanceof NuclearEnergyBuildComp)) return true;

      if(canLink(this, (NuclearEnergyBuildComp)other)){
        configure(Point2.unpack(other.pos()));
        return false;
      }
      
      if(other == this){
        if (linked.size > 0) {
          while (!linked.isEmpty()) {
            configure(Point2.unpack(linked.get(0)));
          }
        }
        else{
          getPotentialLink(tile, team, e -> configure(Point2.unpack(e.getBuilding().pos())));
        }
        return false;
      }
      
      return true;
    }

    public Seq<NuclearEnergyBuildComp> getNodeDumps(){
      Seq<NuclearEnergyBuildComp> res = proximityNuclearBuilds().filter(e -> e.consumeEnergy());

      for (int i = 0; i < linked.size; i++) {
        int pos = linked.get(i);

        Building b = world.build(pos);

        if (b instanceof NuclearEnergyBuildComp n){
          res.add(n);
        }
        else linked.removeIndex(i);
      }

      return res;
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      if(timer(timeID, 10)){
        flowing.clear();
        for (int i = 0; i < linked.size; i++) {
          int pos = linked.get(i);

          WindowedMean mean;
          mean = flowMean.get(pos, () -> new WindowedMean(5));
          mean.add(chanceFlow.get(pos, 0f));
          flowing.put(pos, mean.mean()/10);
          smoothAlpha.put(pos, Mathf.lerpDelta(smoothAlpha.get(pos), Mathf.clamp(flowing.get(pos)/moveAlphaRate), 0.02f));

          chanceFlow.put(pos, 0);
        }
      }

      dumpEnergy(getNodeDumps());
    }

    @Override
    public void energyMoved(NuclearEnergyBuildComp next, float rate) {
      chanceFlow.increment(next.getBuilding().pos(), rate);
    }

    @Override
    public void draw(){
      super.draw();
      Draw.z(Layer.effect);

      setLaserColor();
      Fill.circle(x, y, lightRadius);
      Lines.stroke(0.3f);
      SglDraw.dashCircle(x, y, lightRadius*1.5f, Time.time*2);

      if(linked.isEmpty()) return;

      setLaserColor();
      Lines.stroke(4f);
      for(int i = 0; i < linked.size; i++){
        int pos = linked.get(i);
        Building entity = world.build(pos);
        if(entity == null) continue;
        drawLink(this, (NuclearEnergyBuildComp) entity);

        float tx = Tmp.v1.x;
        float ty = Tmp.v1.y;

        int fi = i + 1;
        Draw.draw(Draw.z(), () -> {
          float alpha = smoothAlpha.get(pos)*laserOpacity;

          setLaserColor();
          MathRenderer.setThreshold(0.5f, 0.8f);

          rand.setSeed((long) id*fi);
          for (int ig = 0; ig < 3; ig++) {
            Draw.color(linkColor, linkColor2, Mathf.absin(rand.random(linkGradientScl/2, linkGradientScl*2), 1));

            MathRenderer.setDispersion(rand.random(0.22f, 0.34f)*alpha);
            MathRenderer.drawSin(
                x, y, tx, ty,
                rand.random(linkLeaserStroke*1.2f, linkLeaserStroke*1.8f)*alpha*(0.9f + 0.1f*Mathf.sin(Time.time/rand.random(10f, 15f))),
                rand.random(360f, 720f),
                -rand.random(5f, 8f)*Time.time
            );
          }

          Draw.reset();
        });

        if (!(entity instanceof NuclearNodeBuild)) {
          Fill.circle(tx, ty, lightRadius*smoothAlpha.get(pos));
          Lines.stroke(0.3f);
          SglDraw.dashCircle(tx, ty, lightRadius*1.5f*smoothAlpha.get(pos), Time.time*2);
        }
      }
      Draw.reset();
    }
  
    @Override
    public void dropped(){
      while (!linked.isEmpty()){
        configure(Point2.unpack(linked.get(0)));
      }
    }
  
    @Override
    public void drawConfigure(){
      Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
      Drawf.circles(x, y, linkRange * tilesize);

      for (int i = 0; i < linked.size; i++) {
        int pos = linked.get(i);

        Building link = world.build(pos);
        if(!(link instanceof NuclearEnergyBuildComp)) continue;
        float len = Mathf.len(link.x - x, link.y - y);

        Drawf.arrow(x, y, link.x, link.y, len*(Time.time%120)/120, linkLeaserStroke*2, linkColor);
        Drawf.line(linkColor, x, y, link.x, link.y);
        Drawf.square(link.x, link.y, link.block.size * tilesize / 2f + 1f, Pal.place);
      }

      for (int i = 0; i < linkThis.size; i++) {
        int pos = linkThis.get(i);

        Building other = world.build(pos);
        if(!(other instanceof NuclearEnergyBuildComp)) continue;
        float len = Mathf.len(other.x - x, other.y - y);

        Drawf.arrow(other.x, other.y, x, y, len*(Time.time%120)/120, linkLeaserStroke*2, linkColor);
        Drawf.dashLine(linkColor, x, y, other.x, other.y);
        Drawf.circles(other.x, other.y, other.block.size*tilesize/1.8f + Mathf.absin(4, 2), Pal.place);
      }
    
      Draw.reset();
    }

    @Override
    public Object config() {
      Point2[] lis = new Point2[linked.size];
      for (int i = 0; i < linked.size; i++) {
        lis[i] = Point2.unpack(linked.get(i));
      }

      return lis;
    }

    @Override
    public void drawSelect(){
      super.drawSelect();
    
      Lines.stroke(1f);
    
      Draw.color(Pal.accent);
      Drawf.circles(x, y, linkRange * tilesize);
      Draw.reset();
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.i(smoothAlpha.size);

      TypeIO.writeIntSeq(write, linked);
      TypeIO.writeIntSeq(write, linkThis);

      IntFloatMap.Keys keys = smoothAlpha.keys();
      while(keys.hasNext()){
        int p = keys.next();
        write.i(p);
        write.f(smoothAlpha.get(p));
        write.f(flowing.get(p));
        write.f(chanceFlow.get(p));
      }
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      int size = read.i();

      if (revision >= 2){
        linked = TypeIO.readIntSeq(read);
        linkThis = TypeIO.readIntSeq(read);
      }

      smoothAlpha.clear();
      flowing.clear();
      chanceFlow.clear();
      for(int i = 0; i < size; i++){
        int p = read.i();
        smoothAlpha.put(p, read.f());
        flowing.put(p, read.f());
        chanceFlow.put(p, read.f());
      }
    }
  }
}
