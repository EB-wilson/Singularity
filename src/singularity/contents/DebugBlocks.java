package singularity.contents;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import singularity.graphic.SglDraw;
import singularity.world.blocks.TestBlock;

public class DebugBlocks implements ContentList{
  public static Block drawTest;

  @Override
  public void load(){
    drawTest = new TestBlock("test_block"){{
      requirements(Category.effect, ItemStack.with());

      draw = new DrawBlock(){
        @Override
        public void draw(Building build){
          super.draw(build);

          for(int i = 0; i < 12; i++){
            SglDraw.drawRectAsCylindrical(
                build.x, build.y + i*4 + Mathf.randomSeed(build.id + i, -6, 6),
                Mathf.randomSeed(build.id + 1 + i, 18), Mathf.randomSeed(build.id + 2 + i, 8),
                10 + i + Mathf.randomSeed(build.id + 3 + i, -5, 5),
                Time.time + Mathf.randomSeed(build.id + 4 + i, 360),
                0, Pal.reactorPurple, Pal.reactorPurple2, Draw.z(), Layer.effect
            );
          }
        }
      };
    }};
  }
}
