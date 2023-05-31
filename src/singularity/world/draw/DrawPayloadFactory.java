package singularity.world.draw;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.PixmapRegion;
import arc.graphics.g2d.TextureRegion;
import arc.util.Eachable;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.graphic.GraphicUtils;

public class DrawPayloadFactory<E> extends DrawDirSpliceBlock<E>{
  public TextureRegion topRegion, outRegion;

  public Cons<E> drawPayload = e -> {};
  public String suffix = "";

  @Override
  public void load(Block block) {
    String name = block.name;
    int size = block.size;

    topRegion = Core.atlas.find(name + "_top", "factory-top-" + size + suffix);
    outRegion = Core.atlas.find(name + "_out", "factory-out-" + size + suffix);

    Pixmap[] splicers = new Pixmap[4];

    PixmapRegion region = Core.atlas.getPixmap(Core.atlas.find(name + "_in", "factory-in-" + size + suffix));
    Pixmap pixmap = region.crop();
    for(int i = 0; i < 4; i++){
      Pixmap m = i == 1 || i == 2? GraphicUtils.rotatePixmap90(pixmap.flipY(), i): GraphicUtils.rotatePixmap90(pixmap, i);
      splicers[i] = m;
    }

    for(int i = 0; i < regions.length; i++){
      regions[i] = getSpliceRegion(splicers, i);
    }

    for(Pixmap p: splicers){
      p.dispose();
    }
  }

  @Override
  public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list) {
    Draw.rect(block.region, plan.drawx(), plan.drawy());
    super.drawPlan(block, plan, list);
    Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation*90);
    Draw.rect(topRegion, plan.drawx(), plan.drawy());
  }

  @SuppressWarnings("unchecked")
  @Override
  public void draw(Building build) {
    Draw.rect(build.block.region, build.x, build.y);
    Draw.rect(regions[spliceBits.get((E) build)], build.x, build.y);
    Draw.rect(outRegion, build.x, build.y, build.rotdeg());

    drawPayload.get((E) build);

    Draw.rect(topRegion, build.x, build.y);
  }

  @Override
  public TextureRegion[] icons(Block block) {
    return new TextureRegion[]{
        block.region,
        outRegion,
        topRegion
    };
  }
}
