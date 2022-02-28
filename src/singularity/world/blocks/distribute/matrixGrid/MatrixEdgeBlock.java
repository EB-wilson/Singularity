package singularity.world.blocks.distribute.matrixGrid;

import arc.Core;
import arc.func.Cons2;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.graphic.SglDraw;
import universeCore.annotations.Annotations;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class MatrixEdgeBlock extends Block implements MatrixEdgeLinker{
  public int linkLength = 16;
  
  public TextureRegion linkRegion;
  
  public MatrixEdgeBlock(String name){
    super(name);
    update = true;
    configurable = true;
    
    config(Point2.class, (Cons2<MatrixEdgeBuild, Point2>) this::link);
  }
  
  @Override
  public void link(MatrixGridEdge entity, Point2 pos){
    MatrixEdgeLinker.super.link(entity, pos);
    ((MatrixEdgeBuild)entity).linkLerp = 0;
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
  
  @Annotations.ImplEntries
  public class MatrixEdgeBuild extends Building implements MatrixGridEdge{
    protected MatrixEdgeContainer edges = new MatrixEdgeContainer();
    
    public float linkLerp;
  
    @Override
    public void updateTile(){
      if(nextEdge() != null){
        if(!nextEdge().getBuilding().isAdded()) delink(nextEdge());
        linkLerp = Mathf.lerpDelta(linkLerp, 1, 0.02f);
      }
    }
  
    @Override
    public boolean onConfigureTileTapped(Building other){
      if(canLink(this, other)){
        configure(new Point2(other.tile().x, other.tile().y));
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
    public void draw(){
      super.draw();
      if(nextEdge() != null) SglDraw.drawLink(tile, nextEdge().tile(), linkRegion(), null, linkLerp);
    }
  }
}
