package singularity.world.blocks.distribute.matrixGrid;

import arc.Core;
import arc.func.Cons2;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Point2;
import arc.util.Eachable;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import singularity.Singularity;
import singularity.graphic.SglDraw;
import singularity.world.components.EdgeLinkerBuildComp;
import singularity.world.components.EdgeLinkerComp;
import universecore.annotations.Annotations;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class MatrixEdgeBlock extends Block implements EdgeLinkerComp{
  public int linkLength = 16;
  public float linkOffset;
  
  public TextureRegion linkRegion, linkCapRegion;
  public TextureRegion linkLightRegion, linkLightCapRegion;
  
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
    entity.linkLerp(0);
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
    linkRegion = Core.atlas.find(name + "_link", Singularity.getModAtlas("matrix_grid_edge"));
    linkCapRegion = Core.atlas.find(name + "_cap", Singularity.getModAtlas("matrix_grid_cap"));
    linkLightRegion = Core.atlas.find(name + "_light_link", Singularity.getModAtlas("matrix_grid_light_edge"));
    linkLightCapRegion = Core.atlas.find(name + "_light_cap", Singularity.getModAtlas("matrix_grid_light_cap"));
  }
  
  @Override
  public void drawPlanConfigTop(BuildPlan req, Eachable<BuildPlan> list){
    Point2 pos = (Point2) req.config;
    if(pos == null) return;
    
    list.each(plan -> {
      if(Point2.pack(req.x + pos.x, req.y + pos.y) == Point2.pack(plan.x, plan.y)){
        if(plan.block instanceof EdgeLinkerComp b){
          SglDraw.drawLink(req.tile(), req.block.offset, linkOffset,
              plan.tile(), plan.block.offset, b.linkOffset(), linkRegion, null, 1);
        }
      }
    });
  }
  
  @Annotations.ImplEntries
  public class MatrixEdgeBuild extends Building implements EdgeLinkerBuildComp{
    public boolean loaded;
    public int nextPos = -1;

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
      if(other instanceof EdgeLinkerBuildComp && canLink(this, (EdgeLinkerBuildComp) other)){
        configure(other.pos());
        return false;
      }
      return true;
    }
  
    @Override
    public Point2 config(){
      Point2 p = Point2.unpack(nextPos);
      return p.set(p.x - tile.x, p.y - tile.y);
    }
  }
}
