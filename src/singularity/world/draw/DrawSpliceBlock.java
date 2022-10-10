package singularity.world.draw;

import arc.Core;
import arc.func.Boolf2;
import arc.func.Func;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Point2;
import arc.util.Eachable;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import singularity.world.DirEdges;

import java.util.Arrays;

public class DrawSpliceBlock<E> extends DrawBlock{
  private final static String[] splices = {"right_top", "left_top", "left_bottom", "right_bottom"};
  
  public TextureRegion[] regions = new TextureRegion[12];
  public Boolf2<BuildPlan, BuildPlan> planSplicer = (plan, other) -> false;
  public Func<E, int[]> splicer = e -> new int[]{-1, -1, -1, -1, -1, -1, -1, -1};

  public boolean interCorner;

  @Override
  public void load(Block block){
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
  public TextureRegion[] icons(Block block){
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

  @Override
  public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
    boolean[] data = new boolean[8];
    Block planBlock = plan.block;

    t: for(int i=0; i<8; i++){
      Block other = null;
      for(Point2 p: DirEdges.get8(plan.block.size, i)){
        int x = plan.x + p.x;
        int y = plan.y + p.y;
        BuildPlan[] target = {null};

        list.each(pl -> {
          if(target[0] != null) return;
          if(pl.x == x && pl.y == y){
            target[0] = pl;
          }
        });

        if(target[0] == null) continue t;

        if(other == null){
          if(planSplicer.get(plan, target[0])){
            other = target[0].block;
          }
          else{
            data[i] = false;
            continue t;
          }
        }
        else if(other != planBlock || !planSplicer.get(plan, target[0])){
          data[i] = false;
          continue t;
        }
      }
      data[i] = true;
    }
  
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
    
    for(int i=0; i<data.length; i++){
      if(i < 4){
        if(bits[i] != -1) Draw.rect(regions[i], plan.drawx(), plan.drawy());
      }
      else{
        if(bits[i] == 0){
          Draw.rect(regions[2*i - 4], plan.drawx(), plan.drawy());
        }else if(bits[i] == 1) Draw.rect(regions[2*i - 3], plan.drawx(), plan.drawy());
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void draw(Building entity){
    int[] data = splicer.get((E)entity);
    for(int i=0; i<data.length; i++){
      if(i < 4){
        if(data[i] != -1) Draw.rect(regions[i], entity.x, entity.y);
      }
      else{
        if(data[i] == 0){
          Draw.rect(regions[2*i - 4], entity.x, entity.y);
        }
        else if(data[i] == 1) Draw.rect(regions[2*i - 3], entity.x, entity.y);
      }
    }
  }
}
