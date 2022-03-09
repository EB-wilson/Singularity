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
import arc.struct.FloatSeq;
import arc.struct.IntSeq;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.graphic.SglDraw;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.blocks.distribute.IOPointBlock;
import universeCore.annotations.Annotations;
import universeCore.util.DataPackable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

@Annotations.ImplEntries
public class MatrixGridCore extends MatrixGridBlock implements MatrixEdgeLinker{
  static {
    DataPackable.assignType(MatrixGridCoreBuild.LinkPair.typeID, p -> ((MatrixGridCoreBuild)p[0]).new LinkPair());
  }
  
  public int linkLength = 16;
  public int maxEdges = 8;
  
  public TextureRegion linkRegion;
  
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
  public void load(){
    super.load();
    linkRegion = Core.atlas.find(name + "_link");
  }

  @Override
  public void drawRequestConfigTop(BuildPlan req, Eachable<BuildPlan> list){
    byte[] bytes = (byte[]) req.config;
    if(bytes == null) return;
    Reads r = new Reads(new DataInputStream(new ByteArrayInputStream(bytes)));
    r.l();
    int offset = r.i();

    int length = r.i();
    IntSeq configs = new IntSeq();
    for(int i = 0; i < length; i++){
      Point2 p = Point2.unpack(r.i());
      int pos = Point2.pack(req.x + p.x, req.y + p.y);
      DistTargetConfigTable.TargetConfigure cfg = new DistTargetConfigTable.TargetConfigure();
      int len = r.i();
      cfg.read(r.b(len));
      if(!cfg.isContainer()) configs.add(pos);
    }

    for(int i = 0; i < configs.size; i++){
      Tile tile = world.tile(configs.get(i));
      if(tile != null){
        Sgl.ioPoint.drawRequestRegion(new BuildPlan(tile.x, tile.y, 0, Sgl.ioPoint), list);
      }
    }
    
    list.each(plan -> {
      Point2 p = Point2.unpack(offset);
      if(Point2.pack(req.x + p.x, req.y + p.y) == Point2.pack(plan.x, plan.y)){
        if(plan.block instanceof MatrixEdgeLinker){
          SglDraw.drawLink(req.tile(), req.block.offset, plan.tile(), plan.block.offset, linkRegion, null, 1);
        }
      }
    });
  }
  
  @Override
  public void link(MatrixGridEdge entity, Integer pos){
    MatrixEdgeLinker.super.link(entity, pos);
    ((MatrixGridCoreBuild)entity).linkLerp = 0;
  }
  
  @Override
  public void init(){
    super.init();
    clipSize = Math.max(clipSize, linkLength * tilesize * 2);
  }
  
  @Annotations.ImplEntries
  public class MatrixGridCoreBuild extends MatrixGridBuild implements MatrixGridEdge{
    protected MatrixEdgeContainer edges = new MatrixEdgeContainer();
    protected Polygon lastPoly;
    protected Vec2[] vertices;
    protected FloatSeq verticesSeq;
    
    protected int nextPos = -1;
    public boolean loaded;
    protected float alpha, linkLerp;
  
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
      
      alpha = Mathf.lerpDelta(alpha, configIOPoint? 1: 0, 0.02f);
      if(nextPos != -1){
        if(nextEdge() != null && !nextEdge().getBuilding().isAdded()) delink(nextEdge());
        linkLerp = Mathf.lerpDelta(linkLerp, 1, 0.02f);
      }
      else{
        linkLerp = 0;
      }
    }

    @Override
    public void linked(MatrixGridEdge next){
      if(loaded)linkLerp = 0;
    }

    @Override
    public void delinked(MatrixGridEdge next){
      if(loaded) linkLerp = 0;
    }

    @Override
    public void updateLinking(){
      MatrixGridEdge.super.updateLinking();
      loaded = true;
    }
  
    @Override
    public boolean onConfigureTileTapped(Building other){
      boolean result = super.onConfigureTileTapped(other);
      if(canLink(this, other)){
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
      for(IOPointBlock.IOPoint io: ioPoints.values()){
        if(Sgl.ui.secConfig.getConfiguring() == io) continue;
        float radius = io.block.size * tilesize / 2f + 1f;
        Drawf.square(io.x, io.y, radius, Pal.accent);
  
        Tmp.v1.set(-1, 1).setLength(radius + 1).scl((Time.time%60)/60*1.41421f);
        Tmp.v2.set(1, 0).setLength(radius + 1).add(Tmp.v1);
        for(int i=0; i<4; i++){
          Draw.color(Pal.gray);
          Fill.square(io.x + Tmp.v2.x, io.y + Tmp.v2.y, 2f, 45);
          Draw.color(Pal.place);
          Fill.square(io.x + Tmp.v2.x, io.y + Tmp.v2.y, 1.25f, 45);
          Tmp.v2.rotate(90);
        }
      }
      
      drawConfiguring(this);
    }
  
    @Override
    public void draw(){
      super.draw();
      if(nextEdge() != null) SglDraw.drawLink(tile, nextEdge().tile(), linkRegion(), null, linkLerp);
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
      pair.configs = configMap;
      pair.linking = nextPos;
      return pair.pack();
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      linkLerp = read.f();
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      write.f(linkLerp);
    }
  
    protected class LinkPair extends MatrixGridBuild.PosCfgPair{
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
    }
  }
}
