package singularity.world.blocks.distribute.matrixGrid;

import arc.Core;
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
import arc.util.pooling.Pools;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.Singularity;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.ui.tables.DistTargetConfigTable;
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
import static singularity.world.blocks.distribute.matrixGrid.MatrixGridCore.MatrixGridCoreBuild.LinkPair.typeID;

@Annotations.ImplEntries
public class MatrixGridCore extends MatrixGridBlock implements EdgeLinkerComp{
  static {
    DataPackable.assignType(typeID, param -> {
      MatrixGridCoreBuild.LinkPair res = Pools.obtain(MatrixGridCoreBuild.LinkPair.class, MatrixGridCoreBuild.LinkPair::new);
      if(param[0] instanceof MatrixGridBuild build) res.tile = build.tile;
      else if(param[0] instanceof Tile tile) res.tile = tile;
      return res;
    });
  }
  
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
    if(obj instanceof MatrixGridCoreBuild.LinkPair){
      entity.nextPos = ((MatrixGridCoreBuild.LinkPair) obj).linking;
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
  public void drawPlanConfigTop(BuildPlan req, Eachable<BuildPlan> list){
    byte[] bytes = (byte[]) req.config;
    if(bytes == null) return;

    MatrixGridCoreBuild.LinkPair pair = DataPackable.readObject(bytes, req.tile());
    for(DistTargetConfigTable.TargetConfigure config: pair.configs.values()){
      Tile tile = world.tile(config.position);
      if(tile != null){
        Sgl.ioPoint.drawPlanConfigTop(new BuildPlan(tile.x, tile.y, 0, Sgl.ioPoint), list);
      }
    }
    
    list.each(plan -> {
      Point2 p = Point2.unpack(pair.linking);
      if(Point2.pack(req.x + p.x, req.y + p.y) == Point2.pack(plan.x, plan.y)){
        if(plan.block instanceof EdgeLinkerComp b){
          SglDraw.drawLink(req.tile(), req.block.offset, linkOffset, plan.tile(), plan.block.offset, b.linkOffset(), linkRegion, null, 1);
        }
      }
    });

    Pools.free(pair);
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
    protected float alpha;

    protected IntMap<float[]> childLinkWarmup = new IntMap<>();
  
    @Override
    public void updateTile(){
      super.updateTile();
      
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
      
      alpha = Mathf.lerpDelta(alpha, configIOPoint? 1: 0, 0.02f);
    }

    @Override
    public void releaseRequest(){
      super.releaseRequest();

      TMP_EXI.clear();
      for(DistTargetConfigTable.TargetConfigure config: configs){
        float[] warmup = childLinkWarmup.get(config.position);
        if(warmup == null){
          warmup = new float[1];
          childLinkWarmup.put(config.position, warmup);
        }
        TMP_EXI.add(config.position);
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
        DistTargetConfigTable.TargetConfigure cfg = configMap.get(entry.key);
        if(cfg == null || entry.value[0] <= 0.01f) continue;

        Tile t = world.tile(entry.key);
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
        Draw.z(Layer.effect);
        Draw.alpha(0.65f);
        SglDraw.drawLink(
            tile, offset, linkOffset,
            nextEdge().tile(), nextEdge().getBlock().offset, nextEdge().getEdgeBlock().linkOffset(),
            linkLightRegion, linkLightCapRegion,
            linkLerp()
        );
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
      for(IOPointComp io: ioPoints.values()){
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
      return edges.inLerp(tile);
    }
  
    @Override
    public void drawValidRange(){
      if(edges.isClosure() && alpha > 0.001f){
        Color color = gridValid()? Pal.accent: Pal.redderDust;
        Lines.stroke(1.2f, color);
        Draw.alpha(alpha);
        Lines.poly(vertices, 0f, 0f, 1f);
        Draw.color(color);
        Draw.alpha(alpha*(0.2f + 0.25f*Mathf.absin(6f, 1)));
        Fill.poly(verticesSeq);
      }
    }
  
    @Override
    public byte[] config(){
      LinkPair pair = new LinkPair();
      pair.tile = tile;
      pair.configs = configMap;
      pair.linking = nextPos;
      return pair.pack();
    }

    protected static class LinkPair extends MatrixGridBuild.PosCfgPair{
      public static final long typeID = 5463757638164667648L;
  
      @Override
      public long typeID(){
        return typeID;
      }
  
      int linking;
  
      @Override
      public void read(Reads read){
        Point2 p = Point2.unpack(read.i());
        linking = Point2.pack(tile.x + p.x, tile.y + p.y);
        super.read(read);
      }
  
      @Override
      public void write(Writes write){
        Point2 p = Point2.unpack(linking);
        write.i(Point2.pack(p.x - tile.x, p.y - tile.y));
        super.write(write);
      }

      @Override
      public void reset(){
        super.reset();
        linking = -1;
      }
    }
  }
}
