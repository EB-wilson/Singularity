package singularity.world.draw;

import arc.Core;
import arc.func.Boolf2;
import arc.func.Intf;
import arc.graphics.Pixmap;
import arc.graphics.Pixmaps;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.PixmapRegion;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Point2;
import arc.util.Eachable;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import singularity.graphic.GraphicUtils;
import universecore.world.DirEdges;

public class DrawDirSpliceBlock<E> extends DrawBlock{
  public TextureRegion[] regions = new TextureRegion[16];
  public Intf<E> spliceBits = e -> 0;
  public Boolf2<BuildPlan, BuildPlan> planSplicer = (plan, other) -> false;

  public boolean simpleSpliceRegion = false;
  public String suffix = "_splice";

  @Override
  public void load(Block block){
    Pixmap[] splicers = new Pixmap[4];

    if(simpleSpliceRegion){
      PixmapRegion region = Core.atlas.getPixmap(block.name + suffix);
      Pixmap pixmap = region.crop();
      for(int i = 0; i < 4; i++){
        Pixmap m = i == 1 || i == 2? GraphicUtils.rotatePixmap90(pixmap.flipY(), i): GraphicUtils.rotatePixmap90(pixmap, i);
        splicers[i] = m;
      }
    }
    else{
      for(int i = 0; i < splicers.length; i++){
        splicers[i] = Core.atlas.getPixmap(block.name + suffix + "_" + i).crop();
      }
    }

    for(int i = 0; i < regions.length; i++){
      regions[i] = getSpliceRegion(splicers, i);
    }

    for(Pixmap pixmap: splicers){
      pixmap.dispose();
    }
  }

  protected TextureRegion getSpliceRegion(Pixmap[] splicers, int i){
    int move = 0;

    Pixmap map = new Pixmap(splicers[move].width, splicers[move].height);
    while(1 << move <= i){
      int bit = 1 << move;
      if((i & bit) != 0){
        map.draw(splicers[move], true);
      }
      move++;
    }

    Pixmaps.bleed(map, 2);
    Texture tex = new Texture(map, true);
    tex.setFilter(Texture.TextureFilter.nearest);
    tex.setWrap(Texture.TextureWrap.clampToEdge);
    return new TextureRegion(tex);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void draw(Building build){
    Draw.rect(regions[spliceBits.get((E) build)], build.x, build.y);
  }

  @Override
  public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
    int bits = 0;
    Block planBlock = plan.block;

    t: for(int i=0; i<4; i++){
      Block other = null;
      for(Point2 p: DirEdges.get(plan.block.size, i)){
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
            bits &= ~(0b0001 << i);
            continue t;
          }
        }
        else if(other != planBlock || !planSplicer.get(plan, target[0])){
          bits &= ~(0b0001 << i);
          continue t;
        }
      }
      bits |= 0b0001 << i;
    }

    Draw.rect(regions[bits], plan.drawx(), plan.drawy());
  }
}
