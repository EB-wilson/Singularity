package singularity.world.blocks.distribute.matrixGrid;

import arc.Core;
import arc.func.Cons2;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.util.Eachable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.graphic.SglDraw;
import singularity.world.components.EdgeLinkerBuildComp;
import singularity.world.components.EdgeLinkerComp;
import universecore.annotations.Annotations;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class MatrixEdgeBlock extends Block implements EdgeLinkerComp{
  public int linkLength = 16;
  
  public TextureRegion linkRegion;
  
  public MatrixEdgeBlock(String name){
    super(name);
    update = true;
    configurable = true;
    
    config(Point2.class, (MatrixEdgeBuild e, Point2 p) -> e.nextPos = Point2.pack(e.tile.x + p.x, e.tile.y + p.y));
    config(Integer.class, (Cons2<MatrixEdgeBuild, Integer>) this::link);
  }
  
  @Override
  public void link(EdgeLinkerBuildComp entity, Integer pos){
    EdgeLinkerComp.super.link(entity, pos);
    ((MatrixEdgeBuild)entity).linkLerp = 0;
  }

  @Override
  public boolean linkable(EdgeLinkerComp other){
    return other instanceof MatrixEdgeBlock || other instanceof MatrixGridCore;
  }

  @Override
  public void init(){
    super.init();
    clipSize = Math.max(clipSize, linkLength * tilesize * 2);
  }
  
  @Override
  public void load(){
    super.load();
    linkRegion = Core.atlas.find(name + "_link");
  }
  
  @Override
  public void drawRequestConfigTop(BuildPlan req, Eachable<BuildPlan> list){
    Point2 pos = (Point2) req.config;
    if(pos == null) return;
    
    list.each(plan -> {
      if(Point2.pack(req.x + pos.x, req.y + pos.y) == Point2.pack(plan.x, plan.y)){
        if(plan.block instanceof EdgeLinkerComp){
          SglDraw.drawLink(req.tile(), req.block.offset, plan.tile(), plan.block.offset, linkRegion, null, 1);
        }
      }
    });
  }
  
  @Annotations.ImplEntries
  public class MatrixEdgeBuild extends Building implements EdgeLinkerBuildComp{
    public float linkLerp;
    public boolean loaded;
    public int nextPos = -1;

    @Override
    public void linked(EdgeLinkerBuildComp next){
      if(loaded)linkLerp = 0;
    }

    @Override
    public void delinked(EdgeLinkerBuildComp next){
      if(loaded) linkLerp = 0;
    }

    @Override
    public void updateLinking(){
      EdgeLinkerBuildComp.super.updateLinking();
      loaded = true;
    }

    @Override
    public void updateTile(){
      if(nextPos != -1){
        if(nextEdge() != null && !nextEdge().getBuilding().isAdded()) delink(nextEdge());
        linkLerp = Mathf.lerpDelta(linkLerp, 1, 0.02f);
      }
      else{
        linkLerp = 0;
      }
    }
  
    @Override
    public boolean onConfigureTileTapped(Building other){
      if(other instanceof EdgeLinkerComp && canLink(this, (EdgeLinkerBuildComp) other)){
        configure(other.pos());
        return false;
      }
      return true;
    }
  
    @Override
    public void drawConfigure(){
      super.drawConfigure();
      drawConfiguring(this);
    }
  
    @Override
    public Point2 config(){
      Point2 p = Point2.unpack(nextPos);
      return p.set(p.x - tile.x, p.y - tile.y);
    }
  
    @Override
    public void draw(){
      super.draw();
      if(nextEdge() != null) SglDraw.drawLink(tile, nextEdge().tile(), linkRegion(), null, linkLerp);
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
  }
}
