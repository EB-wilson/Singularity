package singularity.world.blocks.gas;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.graphics.Colors;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.input.Placement;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.Autotiler;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.liquid.LiquidJunction;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.world.blockComp.GasBlockComp;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blocks.SglBlock;

import java.util.Arrays;

import static mindustry.Vars.tilesize;

public class GasConduit extends SglBlock implements Autotiler{
  public TextureRegion[] regions = new TextureRegion[5], tops = new TextureRegion[5];
  
  public boolean canLeak = true;
  
  public GasConduit(String name){
    super(name);
    hasGases = true;
    outputGases = true;
    rotate = true;
    solid = false;
    floating = true;
    conveyorPlacement = true;
    noUpdateDisabled = true;
    update = true;
  }
  
  @Override
  public void load(){
    super.load();
    for(int i=0; i<5; i++){
      regions[i] = Core.atlas.find(name + "_" + i);
      tops[i] = Core.atlas.find(name + "_top_" + i);
    }
  }
  
  @Override
  public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
    int[] bits = getTiling(req, list);
    
    if(bits == null) return;
    
    Draw.scl(bits[1], bits[2]);
    Draw.rect(regions[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
    Draw.color();
    Draw.rect(tops[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
    Draw.scl();
  }
  
  @Override
  public Block getReplacement(BuildPlan req, Seq<BuildPlan> requests){
    Boolf<Point2> cont = p -> requests.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && o.rotation == req.rotation && (req.block instanceof GasConduit || req.block instanceof LiquidJunction));
    return cont.get(Geometry.d4(req.rotation)) &&
      cont.get(Geometry.d4(req.rotation - 2)) &&
      req.tile() != null &&
      req.tile().block() instanceof GasConduit &&
      Mathf.mod(req.build().rotation - req.rotation, 2) == 1 ? Blocks.liquidJunction : this;
  }
  
  @Override
  public void handlePlacementLine(Seq<BuildPlan> plans){
    Placement.calculateBridges(plans, (ItemBridge)Blocks.bridgeConduit);
  }
  
  @Override
  public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
    if(!(otherblock instanceof GasBlockComp)) return false;
    GasBlockComp blockComp = (GasBlockComp)otherblock;
    return blockComp.hasGases() && (blockComp.outputGases() || (lookingAt(tile, rotation, otherx, othery, otherblock))) && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
  }
  
  @Override
  public TextureRegion[] icons(){
    return new TextureRegion[]{regions[0], tops[0]};
  }
  
  public class GasConduitBuild extends SglBuilding{
    int[] blendData;
    Color gasColor;
  
    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();
      
      int[] temp = buildBlending(tile, rotation, null, true);
      blendData = Arrays.copyOf(temp, temp.length);
    }
  
    @Override
    public void handleGas(GasBuildComp source, Gas gas, float amount){
      gases.add(gas, amount);
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      
      gasColor = Color.white.cpy();
      Tile next = this.tile.nearby(this.rotation);
      if(next.build instanceof GasBuildComp){
        gases.each(stack -> {
          gasColor.mul(stack.gas.color);
          moveGas((GasBuildComp) next.build, stack.gas);
        });
      }
      else if(next.build == null){
        float pressureDiff = pressure() - Sgl.atmospheres.current.getCurrPressure();
        if(pressureDiff <= 0.001) return;
        gases.each(stack -> {
          gasColor.mul(stack.gas.color);
          handleGas(this, stack.gas, -pressureDiff);
          Sgl.gasAreas.pour(next, stack.gas, pressureDiff);
        });
      }
      gasColor.a(pressure()/maxGasPressure*0.75f);
    }
  
    @Override
    public void draw(){
      float rotation = rotdeg();
      int r = this.rotation;
    
      Draw.z(Layer.blockUnder);
      for(int i = 0; i < 4; i++){
        if((blendData[4] & (1 << i)) != 0){
          int dir = r - i;
          float rot = i == 0 ? rotation : (dir)*90;
          drawConduit(x + Geometry.d4x(dir) * tilesize*0.75f, y + Geometry.d4y(dir) * tilesize*0.75f, 0, rot, i != 0 ? SliceMode.bottom : SliceMode.top);
        }
      }
    
      Draw.z(Layer.block);
    
      Draw.scl(blendData[1], blendData[2]);
      drawConduit(x, y, blendData[0], rotation, SliceMode.none);
      Draw.reset();
    }
  
    protected void drawConduit(float x, float y, int bits, float rotation, SliceMode slice){
      Draw.rect(sliced(regions[bits], slice), x, y, rotation);
      
      Draw.color(gasColor);
      Draw.rect(sliced(tops[bits], slice), x, y, rotation);
    }
  }
}
