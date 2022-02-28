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
import arc.util.Time;
import arc.util.Tmp;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.graphic.SglDraw;
import singularity.world.blocks.distribute.IOPointBlock;
import universeCore.annotations.Annotations;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class MatrixGridCore extends MatrixGridBlock implements MatrixEdgeLinker{
  public int linkLength = 16;
  public int maxEdges = 8;
  
  public TextureRegion linkRegion;
  
  public MatrixGridCore(String name){
    super(name);
  
    config(Point2.class, this::link);
  }
  
  @Override
  public void load(){
    super.load();
    linkRegion = Core.atlas.find(name + "_link");
  }
  
  @Override
  public void link(MatrixGridEdge entity, Point2 pos){
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
      if(nextEdge() != null){
        if(!nextEdge().getBuilding().isAdded()) delink(nextEdge());
        linkLerp = Mathf.lerpDelta(linkLerp, 1, 0.02f);
      }
      else{
        linkLerp = 0;
      }
    }
    
    @Override
    public boolean onConfigureTileTapped(Building other){
      boolean result = super.onConfigureTileTapped(other);
      if(canLink(this, other)){
        configure(new Point2(other.tile().x, other.tile().y));
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
      for(IOPointBlock.IOPoint<?> io: ioPoints.values()){
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
  }
}
