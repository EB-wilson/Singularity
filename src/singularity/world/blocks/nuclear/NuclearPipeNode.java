package singularity.world.blocks.nuclear;

import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import singularity.Singularity;
import singularity.world.blockComp.NuclearEnergyBuildComp;
import singularity.world.blocks.SglBlock;
import singularity.world.nuclearEnergy.EnergyGroup;
import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.input.Placement;
import mindustry.world.Tile;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class NuclearPipeNode extends SglBlock{
  public TextureRegion linkDraw, lineTop;
  
  /**管道节点的最大连接范围*/
  public float linkRange = 10;
  /**最大连接数*/
  public int maxLinks = 4;
  
  public NuclearPipeNode(String name){
    super(name);
    hasEnergy = true;
    hasEnergyGroup = true;
    update = true;
    energyCapacity = 0;
    
    configurable = true;
    
    config(Integer.class, (e, value) -> {
      NuclearEnergyBuildComp entity = (NuclearEnergyBuildComp)e;
      Tile tile = Vars.world.tile(value);
      if(tile == null || !(tile.build instanceof NuclearEnergyBuildComp) || !((NuclearEnergyBuildComp)tile.build).getNuclearBlock().hasEnergy()) return;
      NuclearEnergyBuildComp other = (NuclearEnergyBuildComp)tile.build;
      
      if(entity.energy().linked.contains(value)){
        entity.energy().linked.removeValue(value);
        EnergyGroup group = new EnergyGroup();
        group.reflow(entity);
        
        if(other.getNuclearBlock().hasEnergyGroup() && other.energy().group != group){
          other.energy().linked.removeValue(entity.getBuilding().pos());
          EnergyGroup otherGroup = new EnergyGroup();
          otherGroup.reflow(other);
        }
      }
      else{
        if(!linkCountValid(entity, other)) return;
        
        entity.energy().linked.add(value);
        if(other.getNuclearBlock().hasEnergyGroup()){
          entity.energy().group.addGroup(other.energy().group);
          other.energy().linked.add(entity.getBuilding().pos());
        }
        else{
          entity.energy().group.add(other);
        }
        entity.energy().group.calculatePath();
      }
    });
  }
  
  @Override
  public void load(){
    super.load();
    linkDraw = Singularity.getModAtlas("nuclear_pipe");
    lineTop = Core.atlas.find(name + "_light");
  }
  
  public void drawLink(NuclearEnergyBuildComp entity, NuclearEnergyBuildComp other){
    Lines.stroke(7f);
    
    Tmp.v1.set(entity.getBuilding().x, entity.getBuilding().y).sub(other.getBuilding().tile.worldx(), other.getBuilding().tile.worldy()).setLength(tilesize/2f).scl(-1f);
    Lines.line(linkDraw,
      entity.getBuilding().x + Tmp.v1.x, entity.getBuilding().y + Tmp.v1.y,
      other.getBuilding().x - Tmp.v1.x, other.getBuilding().y - Tmp.v1.y,
      false);
  
    Draw.z(Layer.effect);
    
    Lines.stroke(3f);
    Lines.line(lineTop,
      entity.getBuilding().x + Tmp.v1.x, entity.getBuilding().y + Tmp.v1.y,
      other.getBuilding().x - Tmp.v1.x, other.getBuilding().y - Tmp.v1.y,
      false);
  }
  
  @Override
  public void changePlacementPath(Seq<Point2> points, int rotation){
    Placement.calculateNodes(points, this, rotation, (point, other) -> inRange(world.tile(point.x, point.y), world.tile(other.x, other.y), linkRange*tilesize));
  }
  
  public Seq<NuclearEnergyBuildComp> getPotentialLink(NuclearEnergyBuildComp origin){
    Seq<NuclearEnergyBuildComp> temp = new Seq<>();
  
    Geometry.circle(origin.getBuilding().tile.x, origin.getBuilding().tile.y, (int)(linkRange * tilesize), (x, y) -> {
      Building other = Vars.world.build(x, y);
      if(!(other instanceof NuclearEnergyBuildComp) || !((NuclearEnergyBuildComp)other).getNuclearBlock().hasEnergy()) return;
      if(canLink(origin, (NuclearEnergyBuildComp)other))temp.add((NuclearEnergyBuildComp)other);
    });
    
    return temp;
  }
  
  public boolean inRange(Tile origin, Tile other, float range){
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
    @Override
    public NuclearPipeNode block(){
      return (NuclearPipeNode)super.block();
    }
  
    @Override
    public void displayEnergy(Table table){}
  
    @Override
    public boolean onConfigureTileTapped(Building other){
      if(!(other instanceof NuclearEnergyBuildComp)) return true;
      
      if(canLink(this, (NuclearEnergyBuildComp)other)){
        configure(other.pos());
        return false;
      }
      
      if(other == this){
        if(energy.linked.size > 0){
          while(energy.linked.size>0){
            configure(energy.linked.get(0));
          }
        }
        else{
          for(NuclearEnergyBuildComp target: getPotentialLink(this)){
            configure(target.getBuilding().pos());
          }
        }
        return false;
      }
      
      return true;
    }
  
    @Override
    public void draw(){
      super.draw();
      Seq<NuclearEnergyBuildComp> linked = getEnergyLinked();
      if(linked.size == 0) return;
      for(NuclearEnergyBuildComp other: linked){
        drawLink(this, other);
      }
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
