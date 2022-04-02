package singularity.world.draw;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.util.Eachable;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.world.components.ChainsBlockComp;
import singularity.world.components.SpliceBlockComp;
import singularity.world.components.SpliceBuildComp;

import java.util.Arrays;

public class DrawSpliceBlock<T extends Building & SpliceBuildComp> extends SglDrawBlock<T>{
  private final static String[] splices = {"right_top", "left_top", "left_bottom", "right_bottom"};
  
  public TextureRegion[] regions = new TextureRegion[12];
  
  public DrawSpliceBlock(Block block){
    super(block);
  }
  
  @Override
  public void load(){
    super.load();
    for(int i=0; i<4; i++){
      regions[i] = Core.atlas.find(block.name + "_" + i);
    }
    
    for(int a=0; a<splices.length; a++){
      String str = splices[a];
      regions[a*2 + 4] = Core.atlas.find(block.name + "_" + str + "_" + 0);
      regions[a*2 + 5] = Core.atlas.find(block.name + "_" + str + "_" + 1);
    }
  }
  
  @Override
  public TextureRegion[] icons(){
    return new TextureRegion[]{
        regions[0],
        regions[1],
        regions[2],
        regions[3],
        regions[4],
        regions[6],
        regions[8],
        regions[10],
    };
  }
  
  public void drawRequest(BuildPlan req, Eachable<BuildPlan> list, boolean interCorner){
    boolean[] data = new boolean[8];
    
    list.each(other -> {
      if(other.breaking || other == req) return;
      
      for(int i=0; i<8; i++){
        Point2 point = Geometry.d8(i);
        int x = req.x + point.x, y = req.y + point.y;
        if(x == other.x && y == other.y && other.block instanceof ChainsBlockComp && ((SpliceBlockComp)block).chainable((ChainsBlockComp) other.block)){
          data[i] = true;
          return;
        }
      }
    });
  
    int[] bits = new int[8];
    Arrays.fill(bits, -1);
  
    for(int part = 0; part < 8; part++){
      if(part < 4){
        bits[part] = !data[(part*2) % 8]? 0: -1;
      }
      else{
        int i = (part - 4)*2, b = (i+2)%8;
        bits[part] = !data[i] && !data[b]? 0: data[i] && (interCorner || !data[i+1]) && data[b]? 1: -1;
      }
    }
    
    Draw.rect(region, req.drawx(), req.drawy());
    
    for(int i=0; i<data.length; i++){
      if(i < 4){
        if(bits[i] != -1) Draw.rect(regions[i], req.drawx(), req.drawy());
      }
      else{
        if(bits[i] == 0){
          Draw.rect(regions[2*i - 4], req.drawx(), req.drawy());
        }else if(bits[i] == 1) Draw.rect(regions[2*i - 3], req.drawx(), req.drawy());
      }
    }
  }
  
  public class DrawSpliceBlockDrawer extends SglDrawBlockDrawer{
    public DrawSpliceBlockDrawer(T entity){
      super(entity);
    }
  
    @Override
    public void doDraw(){
      draw();
    }
  
    @Override
    public void draw(){
      Draw.rect(region, entity.x, entity.y);
      if(drawDef != null) drawDef.get(entity);
      int[] data = entity.spliceData();
      for(int i=0; i<data.length; i++){
        if(i < 4){
          if(data[i] != -1) Draw.rect(regions[i], entity.x, entity.y);
        }
        else{
          if(data[i] == 0){
            Draw.rect(regions[2*i - 4], entity.x, entity.y);
          }else if(data[i] == 1) Draw.rect(regions[2*i - 3], entity.x, entity.y);
        }
      }
    }
  }
}
