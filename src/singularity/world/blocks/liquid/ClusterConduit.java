package singularity.world.blocks.liquid;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.input.Placement;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.liquid.LiquidJunction;
import mindustry.world.meta.BlockGroup;
import mindustry.world.modules.LiquidModule;

public class ClusterConduit extends MultLiquidBlock{
  public final int timerFlow = timers++;
  public Color botColor = Color.valueOf("565656");
  
  public TextureRegion cornerRegion;
  public TextureRegion[] botRegions = new TextureRegion[conduitAmount];
  public TextureRegion capRegion, arrow;
  
  public @Nullable Block junctionReplacement, bridgeReplacement;
  
  public ClusterConduit(String name){
    super(name);
    rotate = true;
    solid = false;
    floating = true;
    conveyorPlacement = true;
    noUpdateDisabled = true;
    canOverdrive = false;
    group = BlockGroup.liquids;
  }
  
  @Override
  public void load(){
    super.load();
  
    for(int i=0; i<conduitAmount; i++){
      botRegions[i] = Core.atlas.find(name + "_bottom_" + i);
    }
    capRegion = Core.atlas.find(name + "_cap");
    arrow = Core.atlas.find(name + "_arrow");
    cornerRegion = Core.atlas.find(name + "_corner");
  }
  
  @Override
  public void init(){
    super.init();
    
    if(junctionReplacement == null) junctionReplacement = Blocks.liquidJunction;
    if(bridgeReplacement == null || !(bridgeReplacement instanceof ItemBridge)) bridgeReplacement = Blocks.bridgeConduit;
  }
  
  @Override
  public void drawPlanConfigTop(BuildPlan req, Eachable<BuildPlan> list){
    boolean corner = cornerIng(req, list);
    
    if(corner){
      Draw.rect(cornerRegion, req.drawx(), req.drawy());
      Draw.rect(arrow, req.drawx(), req.drawy(), req.rotation * 90);
    }
    else{
      Draw.color(botColor);
      Draw.alpha(0.5f);
      for(int i = 0; i < conduitAmount; i++){
        Draw.rect(botRegions[i], req.drawx(), req.drawy(), req.rotation*90);
      }
      Draw.color();
      Draw.rect(region, req.drawx(), req.drawy(), req.rotation*90);
    }
  }
  
  protected boolean cornerIng(BuildPlan req, Eachable<BuildPlan> list){
    if(req.tile() == null) return false;
    
    boolean[] result = {false};
    list.each(other -> {
      if(other.breaking || other == req) return;
      
      for(Point2 point : Geometry.d4){
        int x = req.x + point.x, y = req.y + point.y;
        if(x >= other.x -(other.block.size - 1) / 2 && x <= other.x + (other.block.size / 2) && y >= other.y -(other.block.size - 1) / 2 && y <= other.y + (other.block.size / 2)){
          if(Vars.world.tile(other.x + Geometry.d4(other.rotation).x, other.y + Geometry.d4(other.rotation).y) == req.tile()){
            result[0] |= other.rotation != req.rotation && Vars.world.tile(req.x + Geometry.d4(req.rotation).x, req.y + Geometry.d4(req.rotation).y) != other.tile();
          }
        }
      }
    });
  
    return result[0];
  }
  
  @Override
  public Block getReplacement(BuildPlan req, Seq<BuildPlan> requests){
    if(junctionReplacement == null) return this;
    
    Boolf<Point2> cont = p -> requests.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && o.rotation == req.rotation && (req.block instanceof ClusterConduit || req.block instanceof LiquidJunction));
    return cont.get(Geometry.d4(req.rotation)) &&
        cont.get(Geometry.d4(req.rotation - 2)) &&
        req.tile() != null &&
        req.tile().block() instanceof ClusterConduit &&
        Mathf.mod(req.build().rotation - req.rotation, 2) == 1 ? junctionReplacement : this;
  }
  
  @Override
  public void handlePlacementLine(Seq<BuildPlan> plans){
    if(bridgeReplacement == null) return;
    
    Placement.calculateBridges(plans, (ItemBridge)bridgeReplacement);
  }
  
  @Override
  public TextureRegion[] icons(){
    return new TextureRegion[]{Core.atlas.find("conduit-bottom"), region};
  }
  
  public class ClusterConduitBuild extends MultLiquidBlock.MultLiquidBuild{
    public boolean capped, isCorner;
  
    @Override
    public void draw(){
      if(isCorner){
        Draw.rect(cornerRegion, x, y);
        Draw.rect(arrow, x, y, rotation*90);
      }
      else{
        for(int i=0; i<conduitAmount; i++){
          Draw.color(botColor);
          Draw.rect(botRegions[i], x, y, rotation*90);
          Drawf.liquid(botRegions[i], x, y, liquidsBuffer[i].currentAmount()/liquidCapacity, liquidsBuffer[i].current().color, rotation*90);
        }
  
        Draw.rect(region, x, y, rotation*90);
        if(capped && capRegion.found()) Draw.rect(capRegion, x, y, rotdeg());
      }
    }
  
    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();
      
      Building next = front();
      capped = next == null || next.team != team || !next.block.hasLiquids;
      for(Building other: proximity){
        isCorner |= other.block instanceof ClusterConduit && other.nearby(other.rotation) == this && other.rotation != rotation && nearby(rotation) != other;
      }
    }
    
    @Override
    public void updateTile(){
      super.updateTile();
      
      if(anyLiquid() && timer(timerFlow, 1)){
        moveLiquidForward(false, null);
        
        noSleep();
      }else{
        sleep();
      }
    }
  
    @Override
    public void display(Table table){
      super.display(table);
    }

    @Override
    public boolean conduitAccept(MultLiquidBuild source, int index, Liquid liquid){
      return super.conduitAccept(source, index, liquid) && (source.block instanceof ClusterConduit || source.tile.absoluteRelativeTo(tile.x, tile.y) == rotation);
    }

    @Override
    public float moveLiquidForward(boolean leaks, Liquid liquid){
      Tile next = tile.nearby(rotation);
      if(next == null) return 0;
      
      float flow = 0;
      for(int i=0; i<liquidsBuffer.length; i++){
        LiquidModule liquids = liquidsBuffer[i];
        if(next.build instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
          flow += moveLiquid(mu, i, liquids.current());
        }
        else if(next.build != null){
          this.liquids = liquids;
          flow += moveLiquid(next.build, liquids.current());
          this.liquids = cacheLiquids;
        }
      }
      
      return flow;
    }
  
    @Override
    public float moveLiquid(Building next, Liquid liquid){
      next = next.getLiquidDestination(this, liquid);
      if(next instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
        float f = 0;
        for(int index=0; index<liquidsBuffer.length; index++){
          if(liquidsBuffer[index] == liquids && mu.conduitAccept(this, index, liquidsBuffer[index].current())){
            f += moveLiquid(mu, index, liquidsBuffer[index].current());
          }
        }
        if (f > 0) return f;
      }
      return super.moveLiquid(next, liquid);
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      noSleep();
      return super.acceptLiquid(source, liquid) && (tile == null || source.tile.absoluteRelativeTo(tile.x, tile.y) == rotation);
    }
  }
}
