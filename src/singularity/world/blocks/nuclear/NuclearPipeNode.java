package singularity.world.blocks.nuclear;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.WindowedMean;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.input.Placement;
import mindustry.world.Block;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.meta.Env;
import singularity.contents.NuclearBlocks;
import singularity.world.components.NuclearEnergyBlockComp;
import singularity.world.components.NuclearEnergyBuildComp;

import static mindustry.Vars.*;
import static mindustry.core.Renderer.laserOpacity;

public class NuclearPipeNode extends NuclearBlock{
  protected static final Seq<NuclearEnergyBuildComp> tempNuclearEntity = new Seq<>();
  protected BuildPlan otherReq;
  protected final static ObjectSet<NuclearEnergyNet> nets = new ObjectSet<>();
  
  public TextureRegion linkDraw, linkLeaser;
  public TextureRegion linkStart, linkEnd;
  
  /**管道节点的最大连接范围*/
  public float linkRange = 10;
  /**最大连接数*/
  public int maxLinks = 4;
  
  public Color linkColor = Color.white;
  public int returnInt;
  
  public NuclearPipeNode(String name){
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
    config(Point2.class, (e, value) -> {
      NuclearEnergyBuildComp entity = (NuclearEnergyBuildComp)e;
      Tile tile = Vars.world.tile(value.x, value.y);
      if(tile == null || !(tile.build instanceof NuclearEnergyBuildComp other) || !((NuclearEnergyBuildComp)tile.build).getNuclearBlock().hasEnergy()) return;

      value.x = tile.x;
      value.y = tile.y;

      if(entity.energy().linked.contains(value.pack())){
        entity.deLink(other);
      }
      else{
        if(!linkCountValid(entity, other)) return;
        entity.link(other);
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
    linkDraw = Core.atlas.find(name + "_link", ((NuclearPipeNode)NuclearBlocks.nuclear_pipe_node).linkDraw);
    linkLeaser = Core.atlas.find(name + "_leaser", ((NuclearPipeNode)NuclearBlocks.nuclear_pipe_node).linkLeaser);
    linkStart = Core.atlas.find(name + "_start", (TextureRegion)null);
    linkEnd = Core.atlas.find(name + "_end", ((NuclearPipeNode)NuclearBlocks.nuclear_pipe_node).linkEnd);
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
              && other != req && other.block.hasPower){
            otherReq = other;
          }
        });
        
        if(otherReq == null || otherReq.block == null) continue;
        
        drawLink(req.drawx(), req.drawy(), size, otherReq.drawx(), otherReq.drawy(), otherReq.block.size);
      }
      Draw.color();
    }
  }
  
  public void drawLink(NuclearEnergyBuildComp entity, NuclearEnergyBuildComp other){
    drawLink(entity.getBuilding().x, entity.getBuilding().y, size, other.getBuilding().x, other.getBuilding().y, other.getBlock().size);
  }
  
  public void drawLink(float x1, float y1, int size1, float x2, float y2, int size2){
    Draw.alpha(laserOpacity);
    Draw.z(Layer.power);
  
    Tmp.v1.set(x1, y1).sub(x2, y2).setLength(size1*tilesize/2f - 1.5f).scl(-1);
    Tmp.v2.set(x2, y2).sub(x1, y1).setLength(size2*tilesize/2f - 1.5f).scl(-1);
  
    float xs = x1 + Tmp.v1.x, ys = y1 + Tmp.v1.y;
    float xo = x2 + Tmp.v2.x, yo = y2 + Tmp.v2.y;
    
    Draw.rect(linkStart != null? linkStart: linkEnd, xs, ys, Tmp.v2.angle());
    Draw.rect(linkEnd, xo, yo, Tmp.v1.angle());
  
    Lines.stroke(8f);
    Lines.line(linkDraw, xs, ys, xo, yo, false);
    Lines.stroke(1f);
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
    
    getPotentialLink(tile, player.team()).each(other -> {
      Draw.color(linkColor, laserOpacity * 0.5f);
      drawLink(tile.worldx() + offset, tile.worldy() + offset, size, other.getBuilding().x, other.getBuilding().y, other.getBlock().size);
      
      Drawf.square(other.getBuilding().x, other.getBuilding().y, other.getBlock().size * tilesize / 2f + 2f, Pal.place);
    });
    
    Draw.reset();
  }
  
  public Seq<NuclearEnergyBuildComp> getPotentialLink(Tile tile, Team team){
    Seq<NuclearEnergyBuildComp> temp = new Seq<>();
    Boolf<NuclearEnergyBuildComp> valid = other -> other != null && other.getBuilding().tile() != tile && other.energy() != null &&
        (other.getNuclearBlock().outputEnergy() || other.getNuclearBlock().consumeEnergy() || other.getBlock() instanceof NuclearPipeNode) &&
        inRange(tile, other.getBuilding().tile(), linkRange * tilesize) && other.getBuilding().team == team &&
        ! nets.contains(other.energy().energyNet) &&
        !(other instanceof NuclearPipeNodeBuild && other.energy().linked.size >= other.getBlock(NuclearPipeNode.class).maxLinks) &&
        ! Structs.contains(Edges.getEdges(size), p -> { //do not link to adjacent buildings
          Tile t = world.tile(tile.x + p.x, tile.y + p.y);
          return t != null && t.build == other;
        });
  
    tempNuclearEntity.clear();
    nets.clear();
  
    //add conducting graphs to prevent double link
    for(Point2 p : Edges.getEdges(size)){
      Tile other = tile.nearby(p);
      if(other != null && other.team() == team && other.build instanceof NuclearEnergyBuildComp && ((NuclearEnergyBuildComp)other.build).energy() != null){
        nets.add(((NuclearEnergyBuildComp) other.build).getEnergyNetwork());
      }
    }
  
    if(tile.build instanceof NuclearEnergyBuildComp && ((NuclearEnergyBuildComp) tile.build).energy() != null){
      nets.add(((NuclearEnergyBuildComp) tile.build).getEnergyNetwork());
    }
  
    Geometry.circle(tile.x, tile.y, (int)(linkRange + 2), (x, y) -> {
      Building other = world.build(x, y);
      if(!(other instanceof NuclearEnergyBuildComp)) return;
      if(valid.get((NuclearEnergyBuildComp) other) && ! tempNuclearEntity.contains((NuclearEnergyBuildComp) other)){
        tempNuclearEntity.add((NuclearEnergyBuildComp) other);
      }
    });
  
    tempNuclearEntity.sort((a, b) -> {
      int type = -Boolean.compare(a.getBlock() instanceof NuclearPipeNode, b.getBlock() instanceof NuclearPipeNode);
      if(type != 0) return type;
      return Float.compare(a.getBuilding().dst2(tile), b.getBuilding().dst2(tile));
    });
  
    returnInt = 0;
  
    tempNuclearEntity.each(valid, e -> {
      if(returnInt++ < maxLinks){
        nets.add(e.getEnergyNetwork());
        temp.add(e);
      }
    });
    
    return temp;
  }
  
  public static Seq<NuclearEnergyBuildComp> getNodeLinks(Tile tile, NuclearEnergyBlockComp block, Team team){
    Boolf<NuclearEnergyBuildComp> valid = other -> other != null && other.getBuilding().tile() != tile && other.getBlock() instanceof NuclearPipeNode &&
        other.energy().linked.size < other.getBlock(NuclearPipeNode.class).maxLinks &&
        other.getBlock(NuclearPipeNode.class).inRange(other.getBuilding().tile, tile, other.getBlock(NuclearPipeNode.class).linkRange * tilesize) && other.getBuilding().team == team
        && !nets.contains(other.getEnergyNetwork()) &&
        !Structs.contains(Edges.getEdges(((Block) block).size), p -> { //do not link to adjacent buildings
          Tile t = world.tile(tile.x + p.x, tile.y + p.y);
          return t != null && t.build == other;
        });
  
    tempNuclearEntity.clear();
    nets.clear();
    
    //add conducting graphs to prevent double link
    for(Point2 p : Edges.getEdges(((Block)block).size)){
      Tile other = tile.nearby(p);
      if(other != null && other.team() == team && other.build instanceof NuclearEnergyBuildComp && ((NuclearEnergyBuildComp)other.build).energy() != null
          && !(block.consumeEnergy() && ((NuclearEnergyBuildComp) other.build).getNuclearBlock().consumeEnergy() && !block.outputEnergy() &&
          !(other.block() instanceof NuclearEnergyBlockComp) || ((NuclearEnergyBlockComp)other.block()).outputEnergy())){
        nets.add(((NuclearEnergyBuildComp) other.build).getEnergyNetwork());
      }
    }
    
    if(tile.build instanceof NuclearEnergyBuildComp && ((NuclearEnergyBuildComp) tile.build).energy() != null){
      nets.add(((NuclearEnergyBuildComp) tile.build).getEnergyNetwork());
    }
    
    Geometry.circle(tile.x, tile.y, 13, (x, y) -> {
      Building other = world.build(x, y);
      if(other instanceof NuclearEnergyBuildComp){
        if(valid.get((NuclearEnergyBuildComp) other) && ! tempNuclearEntity.contains((NuclearEnergyBuildComp) other)){
          tempNuclearEntity.add((NuclearEnergyBuildComp) other);
        }
      }
    });
  
    tempNuclearEntity.sort((a, b) -> {
      int type = -Boolean.compare(a.getBlock() instanceof NuclearPipeNode, b.getBlock() instanceof NuclearPipeNode);
      if(type != 0) return type;
      return Float.compare(a.getBuilding().dst2(tile), b.getBuilding().dst2(tile));
    });
   
    Seq<NuclearEnergyBuildComp> seq = new Seq<>();
    tempNuclearEntity.each(valid, t -> {
      nets.add(t.getEnergyNetwork());
      seq.add(t);
    });
    
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
    if(from.getBuilding().team != to.getBuilding().team || from == to || !from.getNuclearBlock().hasEnergy() || !to.getNuclearBlock().hasEnergy()) return false;
    return inRange(from, to, linkRange*tilesize) || (to instanceof NuclearPipeNodeBuild && inRange(to, from, ((NuclearPipeNodeBuild) to).block().linkRange*tilesize));
  }
  
  public boolean linkCountValid(NuclearEnergyBuildComp from, NuclearEnergyBuildComp to){
    boolean linksValid = ((NuclearPipeNodeBuild)from).block().maxLinks > from.energy().linked.size;
    if(to instanceof NuclearPipeNodeBuild){
      return ((NuclearPipeNodeBuild)to).block().maxLinks > to.energy().linked.size && linksValid;
    }
    return linksValid;
  }
  
  public class NuclearPipeNodeBuild extends SglBuilding{
    public float flowing;
    public float smoothAlpha;
    private float chanceFlow;
    private final Interval flowTimer = new Interval();
    private final WindowedMean flowMean = new WindowedMean(5);
    
    @Override
    public NuclearPipeNode block(){
      return (NuclearPipeNode)super.block();
    }
  
    @Override
    public void placed(){
      if(net.client()) return;
  
      for(NuclearEnergyBuildComp target: getPotentialLink(tile, team)){
        if(!energy.linked.contains(target.getBuilding().pos())) configure(target.getBuilding().pos());
      }
  
      super.placed();
    }
  
    @Override
    public void displayEnergy(Table table){}

    @Override
    public boolean onConfigureBuildTapped(Building other){
      if(!(other instanceof NuclearEnergyBuildComp)) return true;
      
      if(canLink(this, (NuclearEnergyBuildComp)other)){
        configure(Point2.unpack(other.pos()));
        return false;
      }
      
      if(other == this){
        if(energy.linked.size > 0){
          while(energy.linked.size>0){
            configure(Point2.unpack(energy.linked.get(0)));
          }
        }
        else{
          for(NuclearEnergyBuildComp target: getPotentialLink(tile, team)){
            configure(Point2.unpack(target.getBuilding().pos()));
          }
        }
        return false;
      }
      
      return true;
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      if(flowTimer.get(10)){
        flowMean.add(chanceFlow*Time.delta);
        chanceFlow = 0;
        flowing = flowMean.mean()/10;
      }
    }
  
    @Override
    public void onMovePathChild(float flow){
      super.onMovePathChild(flow);
      chanceFlow += flow;
    }
  
    @Override
    public void draw(){
      super.draw();
      Draw.z(Layer.power);
      if(energy.linked.size == 0) return;
      for(int i = 0; i < energy.linked.size; i++){
        Building entity = world.build(energy.linked.get(i));
        if(entity == null || entity.block instanceof NuclearPipeNode && entity.id() >= id) continue;
        drawLink(this, (NuclearEnergyBuildComp) entity);
      }
  
      float alpha = Mathf.clamp(flowing/energyCapacity);
      
      Lines.stroke(4.5f);
      for(int i = 0; i < energy.linked.size; i++){
        Building entity = world.build(energy.linked.get(i));
        if(entity == null) continue;
        if(entity.block instanceof NuclearPipeNode && entity.id() >= id) continue;
        if(entity instanceof NuclearPipeNodeBuild) alpha = (alpha + Mathf.clamp(((NuclearPipeNodeBuild)entity).flowing/((NuclearPipeNodeBuild)entity).block().energyCapacity))/2;
        smoothAlpha = Mathf.lerpDelta(smoothAlpha, alpha, 0.02f);
        Draw.alpha(smoothAlpha * laserOpacity);
        Tmp.v1.set(x, y).sub(entity.tile.worldx(), entity.tile.worldy()).setLength(size*tilesize/2f - 1.5f).scl(-1);
        Tmp.v2.set(entity.x, entity.y).sub(tile.worldx(), tile.worldy()).setLength(entity.block.size*tilesize/2f - 1.5f).scl(-1);
        Drawf.laser(((PowerNode)Blocks.powerNode).laser, linkLeaser, linkLeaser,
            x + Tmp.v1.x, y + Tmp.v1.y,
            entity.x() + Tmp.v2.x, entity.y() + Tmp.v2.y,
            0.45f);
      }
      Draw.reset();
    }
  
    @Override
    public void dropped(){
      energy.linked.clear();
      onEnergyNetworkUpdated();
    }
  
    @Override
    public void drawConfigure(){
      Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
      Drawf.circles(x, y, linkRange * tilesize);
    
      for(int x = (int)(tile.x - linkRange - 2); x <= tile.x + linkRange + 2; x++){
        for(int y = (int)(tile.y - linkRange - 2); y <= tile.y + linkRange + 2; y++){
          Building link = world.build(x, y);
          if(!(link instanceof NuclearEnergyBuildComp)) continue;
          if(link != this && canLink(this, (NuclearEnergyBuildComp) link)){
            if(energy.linked.contains(link.pos())){
              Drawf.square(link.x, link.y, link.block.size * tilesize / 2f + 1f, Pal.place);
            }
          }
        }
      }
    
      Draw.reset();
    }
  
    @Override
    public void drawSelect(){
      super.drawSelect();
    
      Lines.stroke(1f);
    
      Draw.color(Pal.accent);
      Drawf.circles(x, y, linkRange * tilesize);
      Draw.reset();
    }
  }
}
