package singularity.world.blocks.distribute.matrixGrid;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Polygon;
import arc.math.geom.Vec2;
import arc.struct.*;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Tile;
import singularity.Singularity;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.world.blocks.distribute.TargetConfigure;
import singularity.world.components.EdgeLinkerBuildComp;
import singularity.world.components.EdgeLinkerComp;
import singularity.world.components.distnet.IOPointComp;
import singularity.world.distribution.GridChildType;
import singularity.world.meta.SglStat;
import universecore.UncCore;
import universecore.annotations.Annotations;
import universecore.util.DataPackable;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

@Annotations.ImplEntries
public class MatrixGridCore extends MatrixGridBlock implements EdgeLinkerComp{
  public int linkLength = 16;
  public int maxEdges = 8;
  
  public TextureRegion linkRegion, linkCapRegion;
  public TextureRegion linkLightRegion, linkLightCapRegion;
  public float linkOffset;
  public TextureRegion childLinkRegion;
  public ObjectMap<GridChildType, Color> linkColors = new ObjectMap<>();

  public MatrixGridCore(String name){
    super(name);
    
    config(Integer.class, this::link);
  }

  @Override
  public void parseConfigObjects(SglBuilding e, Object obj){
    MatrixGridCoreBuild entity = (MatrixGridCoreBuild) e;
    if(obj instanceof MatrixGridCoreBuild.LinkPair linkPair){
      Point2 p = linkPair.linking;
      entity.nextPos = Point2.pack(e.tileX() + p.x, e.tileY() + p.y);
    }
    super.parseConfigObjects(e, obj);
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(SglStat.maxChildrenNodes, maxEdges);
  }

  @Override
  public void load(){
    super.load();
    linkRegion = Core.atlas.find(name + "_link", Singularity.getModAtlas("matrix_grid_edge"));
    linkCapRegion = Core.atlas.find(name + "_cap", Singularity.getModAtlas("matrix_grid_cap"));
    linkLightRegion = Core.atlas.find(name + "_light_link", Singularity.getModAtlas("matrix_grid_light_edge"));
    linkLightCapRegion = Core.atlas.find(name + "_light_cap", Singularity.getModAtlas("matrix_grid_light_cap"));
    childLinkRegion = Core.atlas.find(name + "_child_linker", Singularity.getModAtlas("matrix_grid_child_linker"));
  }

  @Override
  public Object pointConfig(Object config, Cons<Point2> transformer){
    if(config instanceof byte[] b && DataPackable.readObject(b) instanceof MatrixGridCoreBuild.LinkPair cfg){
      cfg.handleConfig(transformer);
      return cfg.pack();
    }
    return config;
  }

  static MatrixGridCoreBuild.LinkPair pair = new MatrixGridCoreBuild.LinkPair();
  @Override
  public void drawPlanConfigTop(BuildPlan req, Eachable<BuildPlan> list){
    byte[] bytes = (byte[]) req.config;
    if(bytes == null) return;

    pair.read(bytes);

    Point2 p = pair.linking;
    list.each(plan -> {
      if(Point2.pack(req.x + p.x, req.y + p.y) == Point2.pack(plan.x, plan.y)){
        if(plan.block instanceof EdgeLinkerComp b){
          SglDraw.drawLink(
              req.drawx(), req.drawy(), linkOffset,
              plan.drawx(), plan.drawy(), b.linkOffset(),
              linkRegion, null, 1
          );
        }
      }
    });

    pair.reset();
  }

  @Override
  public void link(EdgeLinkerBuildComp entity, Integer pos){
    EdgeLinkerComp.super.link(entity, pos);
    entity.linkLerp(0);
  }

  @Override
  public boolean linkable(EdgeLinkerComp other){
    return other instanceof MatrixEdgeBlock;
  }

  @Override
  public void init(){
    super.init();
    clipSize = Math.max(clipSize, linkLength * tilesize * 2);
    initColor();
  }

  public void initColor(){
    linkColors.put(GridChildType.input, Pal.heal);
    linkColors.put(GridChildType.output, Pal.accent);
    linkColors.put(GridChildType.acceptor, SglDrawConst.matrixNet);
    linkColors.put(GridChildType.container, SglDrawConst.matrixNet);
  }

  @Annotations.ImplEntries
  public class MatrixGridCoreBuild extends MatrixGridBuild implements EdgeLinkerBuildComp{
    private static final IntSet TMP_EXI = new IntSet();

    protected EdgeContainer edges = new EdgeContainer();
    protected Polygon lastPoly;
    protected Vec2[] vertices;
    protected FloatSeq verticesSeq;
    
    protected int nextPos = -1;
    protected boolean loaded;

    protected IntMap<float[]> childLinkWarmup = new IntMap<>();

    @Override
    public void updateTile(){
      if(lastPoly != edges.getPoly()){
        lastPoly = edges.getPoly();
        if(lastPoly != null){
          Polygon poly = edges.getPoly();
          float[] vert = poly.getVertices();
          verticesSeq = FloatSeq.with(vert);
          vertices = new Vec2[vert.length/2];
          for(int i = 0; i < vertices.length; i++){
            vertices[i] = new Vec2(vert[i*2], vert[i*2 + 1]);
          }
        }
      }

      for(float[] a: childLinkWarmup.values()){
        a[0] = Mathf.lerpDelta(a[0], 1, 0.02f);
      }

      super.updateTile();
    }

    @Override
    public void releaseRequest(){
      super.releaseRequest();

      TMP_EXI.clear();
      for(TargetConfigure config: configs()){
        float[] warmup = childLinkWarmup.get(config.offsetPos);
        if(warmup == null){
          warmup = new float[1];
          childLinkWarmup.put(config.offsetPos, warmup);
        }
        TMP_EXI.add(config.offsetPos);
      }

      for(IntMap.Entry<float[]> entry: childLinkWarmup){
        if(!TMP_EXI.contains(entry.key)) childLinkWarmup.remove(entry.key);
      }
    }

    @Override
    public void linked(EdgeLinkerBuildComp next){
      if(loaded)linkLerp(0);
    }

    @Override
    public void delinked(EdgeLinkerBuildComp next){
      if(loaded) linkLerp(0);
    }

    @Override
    public void edgeUpdated(){}

    @Annotations.EntryBlocked
    @Override
    public void draw(){
      super.draw();
      drawLink();
      for(IntMap.Entry<float[]> entry: childLinkWarmup){
        TargetConfigure cfg = configMap.get(entry.key);
        if(cfg == null || entry.value[0] <= 0.01f) continue;

        Tile t = world.tile(tileX() + Point2.x(entry.key), tileY() + Point2.y(entry.key));
        if (t == null) return;

        Draw.alpha(0.7f*entry.value[0]);
        ObjectMap<GridChildType, ObjectMap<ContentType, ObjectSet<UnlockableContent>>> map = cfg.get();

        Color c = null;
        for(ObjectMap.Entry<GridChildType, ObjectMap<ContentType, ObjectSet<UnlockableContent>>> mapEntry: map){
          boolean bool = false;
          for(ObjectMap.Entry<ContentType, ObjectSet<UnlockableContent>> setEntry: mapEntry.value){
            if(!setEntry.value.isEmpty()){
              bool = true;
              break;
            }
          }

          if(bool){
            if(c == null){
              c = linkColors.get(mapEntry.key, Color.white);
            }
            else{
              c = Color.white;
              break;
            }
          }
        }
        Draw.color(c);
        Draw.z(Layer.bullet - 5);
        SglDraw.drawLaser(x, y, t.drawx(), t.drawy(), childLinkRegion, null, 8*entry.value[0]);
        Draw.z(Layer.effect);
        Fill.circle(t.drawx(), t.drawy(), 1.5f*entry.value[0]);
      }
      Draw.z(Layer.blockBuilding);
      Draw.reset();
    }

    @Override
    public void drawLink(){
      EdgeLinkerBuildComp.super.drawLink();
      if(nextEdge() != null){
        float l = Draw.z();
        Draw.z(Layer.effect);
        Draw.alpha(0.65f);
        SglDraw.drawLink(
            x, y, linkOffset,
            nextEdge().tile().drawx(), nextEdge().tile().drawy(), nextEdge().getEdgeBlock().linkOffset(),
            linkLightRegion, linkLightCapRegion, linkLerp()
        );
        Draw.z(l);
      }
    }

    @Override
    public void updateLinking(){
      EdgeLinkerBuildComp.super.updateLinking();
      loaded = true;
    }

    @Override
    public boolean onConfigureBuildTapped(Building other){
      boolean result = super.onConfigureBuildTapped(other);
      if(other instanceof EdgeLinkerBuildComp && canLink(this, (EdgeLinkerBuildComp) other)){
        configure(other.pos());
        return false;
      }
      return result;
    }
  
    @Override
    public boolean gridValid(){
      return super.gridValid() && edges.isClosure() && getEdges().all.size <= maxEdges;
    }
  
    @Override
    public void drawConfigure(){
      super.drawConfigure();
      for(IOPointComp io: ioPoints()){
        if(UncCore.secConfig.getConfiguring() == io) continue;
        float radius = io.getBlock().size * tilesize / 2f + 1f;
        Building building = io.getBuilding();
        Drawf.square(building.x, building.y, radius, Pal.accent);
  
        Tmp.v1.set(-1, 1).setLength(radius + 1).scl((Time.time%60)/60*1.41421f);
        Tmp.v2.set(1, 0).setLength(radius + 1).add(Tmp.v1);
        for(int i=0; i<4; i++){
          Draw.color(Pal.gray);
          Fill.square(building.x + Tmp.v2.x, building.y + Tmp.v2.y, 2f, 45);
          Draw.color(Pal.place);
          Fill.square(building.x + Tmp.v2.x, building.y + Tmp.v2.y, 1.25f, 45);
          Tmp.v2.rotate(90);
        }
      }
    }
  
    @Override
    public boolean tileValid(Tile tile){
      return !edges.isClosure() || edges.inLerp(tile);
    }
  
    @Override
    public void drawValidRange(){
      //TODO: draw it
    }
  
    @Override
    public byte[] config(){
      LinkPair pair = new LinkPair();
      pair.configs.clear();
      for (IntMap.Entry<TargetConfigure> entry : configMap) {
        Building build = nearby(Point2.x(entry.key), Point2.y(entry.key));
        if (build != null && !(build instanceof IOPointComp io && !ioPoints().contains(io))){
          pair.configs.put(entry.key, entry.value);
        }
      }
      pair.linking = new Point2(Point2.x(nextPos) - tileX(), Point2.y(nextPos) - tileY());

      return pair.pack();
    }

    public static class LinkPair extends MatrixGridBuild.PosCfgPair{
      public static final long typeID = 5463757638164667648L;
  
      @Override
      public long typeID(){
        return typeID;
      }
  
      Point2 linking;
  
      @Override
      public void read(Reads read){
        linking = Point2.unpack(read.i());
        super.read(read);
      }
  
      @Override
      public void write(Writes write){
        write.i(linking.pack());
        super.write(write);
      }

      @Override
      public void reset(){
        super.reset();
        linking = null;
      }

      @Override
      public void handleConfig(Cons<Point2> handler){
        super.handleConfig(handler);
        handler.get(linking);
      }
    }
  }
}
