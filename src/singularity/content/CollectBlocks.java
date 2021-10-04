package singularity.content;

import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.ContentList;
import mindustry.type.Category;
import mindustry.world.Block;
import singularity.world.blocks.drills.BaseDrill;

import static mindustry.type.ItemStack.with;

public class CollectBlocks implements ContentList {
  /**高速钻头*/
  public static Block high_speed_drill;

  @Override
  public void load() {
    high_speed_drill = new BaseDrill("high_speed_drill"){{
      requirements(Category.production, with(SglItems.strengthening_alloy, 60, SglItems.aerogel, 70, Items.silicon, 90, Items.graphite, 60));
      
      bitHardness = 5;
      drillTime = 300;
      rotatorSpeed = 3.8f;
      size = 4;
  
      newConsume();
      consume.power(1.8f);
      newBooster(2.1f);
      consume.time(60f);
      consume.item(SglItems.dry_ice, 1);
      newBooster(1.9f);
      consume.liquid(Liquids.cryofluid, 0.08f);
      newBooster(1.8f);
      consume.liquid(Liquids.water, 0.1f);
    }};
  }
}
