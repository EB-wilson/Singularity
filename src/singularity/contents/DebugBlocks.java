package singularity.contents;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;
import singularity.Sgl;
import singularity.graphic.SglDraw;
import singularity.type.SglCategory;
import singularity.world.blocks.SglBlock;
import singularity.world.blocks.debug.BlockDataMonitor;
import singularity.world.blocks.debug.VarsContainer;
import singularity.world.draw.SglDrawBlock;

public class DebugBlocks implements ContentList{
  public static Block blockMonitor, varsContainer, testBlock;
  
  @Override
  public void load(){
    blockMonitor = new BlockDataMonitor("block_monitor"){{
      requirements(SglCategory.debugging, ItemStack.empty);
      buildVisibility = Sgl.config.debugMode? BuildVisibility.shown: BuildVisibility.hidden;
    }};
    
    varsContainer = new VarsContainer("vars_container"){{
      requirements(SglCategory.debugging, ItemStack.empty);
      buildVisibility = Sgl.config.debugMode? BuildVisibility.shown: BuildVisibility.hidden;
    }};

    testBlock = new SglBlock("debug"){{
      requirements(SglCategory.debugging, ItemStack.with());
      buildVisibility = Sgl.config.debugMode? BuildVisibility.shown: BuildVisibility.hidden;

      update = true;

      draw = new SglDrawBlock<>(this){{
        drawDef = e -> {
          Draw.color(Pal.accent);
          SglDraw.gradientCircle(e.x, e.y, 120, 60*Mathf.sin(Time.time/36f), Tmp.c1.set(Pal.accent).a(0));
        };
      }};
    }};
  }
}
