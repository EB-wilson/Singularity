package singularity.world.blocks.distribute;

import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import singularity.type.Gas;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blockComp.distributeNetwork.DistElementBuildComp;
import singularity.world.distribution.MatrixGrid;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class MatrixBridge extends MatrixGridBlock{
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
      if(!(target instanceof MatrixBridgeBuild)) return;
      MatrixBridgeBuild other = (MatrixBridgeBuild) target;
      
      if(e.linkTarget != other.pos()){
        e.linkTarget = other.pos();
        if(other.targetBuild == e){
          other.linkTarget = -1;
        }
        else{
          if(e.targetBuild != null){
            e.deLink((DistElementBuildComp) e.targetBuild);
          }
          e.link((DistElementBuildComp) other);
        }
      }
      else{
        e.linkTarget = -1;
        e.deLink((DistElementBuildComp) other);
      }
    });
  }
  
  @Override
  public void init(){
    super.init();
    clipSize = linkRange;
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
  
  public class MatrixBridgeBuild extends MatrixGridBuild{
    public float transportCounter;
    public int linkTarget = -1, lastTarget;
    public MatrixBridgeBuild targetBuild;
    public Building linkConfiguring;
    public float linkLerp;
    
    public @Nullable MatrixGrid matrixGrid;
    public boolean useFrequency;
  
    public boolean canLink(Building target){
      if(!MatrixBridge.this.canLink(tile, target.tile, linkRange*Vars.tilesize)) return false;
      if(target instanceof MatrixBridgeBuild) return target.block == block;
      return distributor.network.netValid() && canDistribute(target);
    }
  
    @Override
    public int frequencyUse(){
      return useFrequency? super.frequencyUse(): 0;
    }
  
    @Override
    public void updateTile(){
      if(lastTarget != linkTarget){
        lastTarget = linkTarget;
        
        Building e = linkTarget ==-1? null:  Vars.world.build(linkTarget);
        targetBuild = e instanceof MatrixBridgeBuild ? (MatrixBridgeBuild) e : null;
        linkLerp = 0;
      }
      if(linkLerp < 0.99f) linkLerp = Mathf.lerpDelta(linkLerp, 1, 0.04f);
      
      if(consValid() && matrixGrid == null){
        if(linkTarget == -1 || targetBuild == null){
          doDump();
        }
        else{
          doTransport(targetBuild);
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
        if(linkTarget >= 0){
          configure(Point2.unpack(linkTarget));
        }
        return false;
      }
      
      if(canLink(other)){
        if(other instanceof MatrixBridgeBuild){
          configure(Point2.unpack(other.pos()));
        }
        else{
          if(distributor.network.netValid()) linkConfiguring = other;
        }
        return false;
      }
      
      return true;
    }
  
    @Override
    public void buildConfiguration(Table table){
      super.buildConfiguration(table);
    }
  
    @Override
    public void drawConfigure(){
      Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
      Drawf.circles(x, y, linkRange * tilesize);
      boolean netValid = distributor.network.netValid();
      float radius;
  
      for(int x = (int)(tile.x - linkRange - 2); x <= tile.x + linkRange + 2; x++){
        for(int y = (int)(tile.y - linkRange - 2); y <= tile.y + linkRange + 2; y++){
          Building link = world.build(x, y);
          if(link != null && link != this && canLink(link)){
            if(linkTarget == link.pos()){
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
              else{
                if(!netValid) continue;
                radius = link.block.size * tilesize / 2f + 1f;
                Drawf.square(link.x, link.y, radius, Pal.accent);
                
                Tmp.v1.set(-1, 1).setLength(radius + 1).scl((Time.time%60)/60*1.41421f);
                Tmp.v2.set(1, 0).setLength(radius + 1).add(Tmp.v1);
                for(int i=0; i<4; i++){
                  Draw.color(Pal.gray);
                  Fill.square(link.x + Tmp.v2.x, link.y + Tmp.v2.y, 2f, 45);
                  Draw.color(Pal.place);
                  Fill.square(link.x + Tmp.v2.x, link.y + Tmp.v2.y, 1.25f, 45);
                  Tmp.v2.rotate(90);
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
      if(targetBuild != null){
        drawLink(targetBuild);
      }
    }
  
    public void drawLink(Building other){
      Tmp.v1.set(other.x, other.y).sub(x, y);
      Tmp.v2.set(Tmp.v1).scl(linkLerp).sub(Tmp.v1.setLength(tilesize/2f));
      Lines.stroke(8);
      Lines.line(linkRegion, x + Tmp.v1.x,
          y + Tmp.v1.y,
          x + Tmp.v2.x,
          y + Tmp.v2.y,
          false);
    
      if(capRegion != null) Draw.rect(capRegion, x + Tmp.v2.x, y + Tmp.v2.y, Tmp.v2.angle());
    }
  
    @Override
    public boolean acceptItem(Building source, Item item){
      return source.team == team && items.get(item) < itemCapacity && (source.block == block || targetBuild != null);
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.team == team && liquids.get(liquid) < liquidCapacity && (source.block == block || targetBuild != null);
    }
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return source.getBuilding().team == team && pressure() < maxGasPressure && (source.getBlock() == block || targetBuild != null);
    }
  }
}
