package singularity.world.blocks.distribute;

import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;
import arc.struct.ObjectSet;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import singularity.graphic.SglDraw;
import singularity.type.Gas;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blockComp.distributeNetwork.DistElementBuildComp;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class MatrixBridge extends DistNetBlock{
  private static final ObjectSet<MatrixBridgeBuild> temps = new ObjectSet<>();
  
  public TextureRegion linkRegion, capRegion;
  
  public float linkRange = 15;
  public float transportItemTime = 1;
  
  public MatrixBridge(String name){
    super(name);
    configurable = true;
    hasItems = true;
    hasLiquids = outputsLiquid = true;
    hasGases = outputGases = true;
  }
  
  @Override
  public void appliedConfig(){
    super.appliedConfig();
    
    config(Point2.class, (MatrixBridgeBuild e, Point2 p) -> {
      int pos = p.pack();
      Building target = Vars.world.build(pos);
      if(target instanceof MatrixBridgeBuild){
        if(target == e.linkNext){
          e.linkNext = null;
          e.deLink((DistElementBuildComp) target);
        }
        else{
          if(e.linkNext != null){
            e.deLink((DistElementBuildComp) e.linkNext);
          }
          if(((MatrixBridgeBuild) target).linkNext == e){
            ((MatrixBridgeBuild) target).linkNext = null;
          }
          else e.link((DistElementBuildComp) target);
          e.linkNext = (MatrixBridgeBuild) target;
        }
        e.linkNextLerp = 0;
      }
      else if(target instanceof DistElementBuildComp){
        if(target == e.linkElement){
          e.linkElement = null;
          e.deLink((DistElementBuildComp) target);
        }
        else{
          if(e.linkElement != null){
            e.deLink(e.linkElement);
          }
          e.linkElement = (DistElementBuildComp) target;
          e.link((DistElementBuildComp) target);
        }
        e.linkElementLerp = 0;
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
    linkRegion = Core.atlas.find(name + "_link");
    capRegion = Core.atlas.find(name + "_cap");
  }
  
  public boolean canLink(Tile origin, Tile other, float range){
    if(origin == null || other == null) return false;
    return Intersector.overlaps(Tmp.cr1.set(origin.drawx(), origin.drawy(), range), other.getHitbox(Tmp.r1));
  }
  
  public void doCanLink(Tile origin, float range, Cons<MatrixBridgeBuild> cons){
    Geometry.circle(origin.x, origin.y, (int) (range*Vars.tilesize), (x, y) -> {
      Building e = Vars.world.build(x, y);
      if(e instanceof MatrixBridgeBuild && temps.add((MatrixBridgeBuild) e)){
        cons.get((MatrixBridgeBuild) e);
      }
    });
  }
  
  public class MatrixBridgeBuild extends DistNetBuild{
    public float transportCounter;
    public MatrixBridgeBuild linkNext;
    public DistElementBuildComp linkElement;
    public float linkNextLerp, linkElementLerp;
  
    public boolean canLink(Building target){
      if(!MatrixBridge.this.canLink(tile, target.tile, linkRange*Vars.tilesize)) return false;
      if(target instanceof MatrixBridgeBuild && target.block == block) return true;
      return target instanceof DistElementBuildComp;
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      if(linkNext != null && ! linkNext.getBuilding().isAdded()) linkNext = null;
      if(linkElement != null && ! linkElement.getBuilding().isAdded()) linkElement = null;
      if(linkNextLerp < 0.99f) linkNextLerp = Mathf.lerpDelta(linkNextLerp, 1, 0.04f);
      if(linkElementLerp < 0.99f) linkElementLerp = Mathf.lerpDelta(linkElementLerp, 1, 0.04f);
      
      if(consValid()){
        if(linkNext == null){
          doDump();
        }
        else{
          doTransport(linkNext);
        }
      }
    }
    
    public void doDump(){
      dumpAccumulate();
      dumpLiquid();
      dumpGas();
    }
    
    public void doTransport(MatrixBridgeBuild next){
      transportCounter += edelta();
      while(transportCounter >= transportItemTime){
        Item item = items.take();
        if(item != null){
          if(next.acceptItem(this, item)){
            next.handleItem(this, item);
          }
          else{
            items.add(item, 1);
            items.undoFlow(item);
          }
        }
        transportCounter -= transportItemTime;
      }
      
      liquids.each((l, a) -> {
        moveLiquid(next, l);
      });
  
      moveGas(next);
    }
  
    @Override
    public boolean onConfigureTileTapped(Building other){
      if(other == null) return true;
      
      if(other == this){
        if(linkNext != null) configure(Point2.unpack(linkNext.pos()));
        if(linkElement != null) configure(Point2.unpack(linkElement.getBuilding().pos()));
        return true;
      }
      
      if(canLink(other)){
        if(other instanceof DistElementBuildComp){
          configure(Point2.unpack(other.pos()));
        }
        return false;
      }
      
      return true;
    }
  
    @Override
    public void drawConfigure(){
      Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
      Drawf.circles(x, y, linkRange * tilesize);
      float radius;
  
      for(int x = (int)(tile.x - linkRange - 2); x <= tile.x + linkRange + 2; x++){
        for(int y = (int)(tile.y - linkRange - 2); y <= tile.y + linkRange + 2; y++){
          Building link = world.build(x, y);
          if(link != null && link != this && canLink(link)){
            if(linkNext == link || linkElement == link){
              radius = link.block.size * tilesize / 2f + 1f;
              Drawf.square(link.x, link.y, radius, Pal.place);
              Tmp.v1.set(link.x, link.y).sub(this.x, this.y).setLength(radius);
              Drawf.dashLine(Pal.accent,
                  this.x + Tmp.v1.x, this.y + Tmp.v1.y,
                  link.x - Tmp.v1.x, link.y - Tmp.v1.y);
            }
            else{
              if(link.block == block){
                radius = tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f);
                Tmp.v1.set(0, 1).setLength(radius + 4).setAngle(Time.time);
                Drawf.circles(link.x, link.y, radius);
                for(int i=0; i<4; i++){
                  Draw.color(Pal.gray);
                  Fill.poly(link.x + Tmp.v1.x, link.y + Tmp.v1.y, 3, 3.5f, Time.time + i*90 + 60);
                  Draw.color(Pal.accent);
                  Fill.poly(link.x + Tmp.v1.x, link.y + Tmp.v1.y, 3, 1.5f, Time.time + i*90 + 60);
                  Tmp.v1.rotate(90);
                }
              }
            }
          }
        }
      }
  
      Draw.reset();
    }
  
    @Override
    public void draw(){
      Draw.rect(region, x, y);
      if(linkNext != null){
        SglDraw.drawLink(tile, linkNext.getBuilding().tile, linkRegion, capRegion, linkNextLerp);
      }
      if(linkElement != null){
        SglDraw.drawLink(tile, linkElement.getBuilding().tile, linkRegion, capRegion, linkElementLerp);
      }
    }
  
    @Override
    public boolean acceptItem(Building source, Item item){
      return source.team == team && items.get(item) < itemCapacity && (source.block == block || linkNext != null);
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.team == team && liquids.get(liquid) < liquidCapacity && (source.block == block || linkNext != null);
    }
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return source.getBuilding().team == team && pressure() < maxGasPressure && (source.getBlock() == block || linkNext != null);
    }
  }
}
