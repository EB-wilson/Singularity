package singularity.contents.override;

import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawLiquidTile;
import mindustry.world.draw.DrawMulti;
import mindustry.world.draw.DrawRegion;
import singularity.contents.SglItems;
import singularity.world.blocks.product.NormalCrafter;
import universecore.util.OverrideContentList;

import static mindustry.type.ItemStack.with;

public class OverrideBlocks implements OverrideContentList{
  public static Block oldMelter, oldPulverizer;

  @Override
  public void load(){
    doOverrideContent(oldMelter = Blocks.melter,
        Blocks.melter = new NormalCrafter("melter_override"){{
          requirements(Category.crafting, ItemStack.with(Items.copper, 30, Items.lead, 35, Items.graphite, 45));
          autoSelect = true;
          canSelect = false;
          health = 200;
          hasLiquids = hasPower = true;
        
          newConsume();
          consume.items(ItemStack.with(Items.scrap, 1));
          consume.power(1f);
          consume.time(10);
          newProduce();
          produce.liquid(Liquids.slag, 0.2f);
    
          newConsume();
          consume.items(ItemStack.with(SglItems.black_crystone, 1));
          consume.power(1f);
          consume.time(10);
          newProduce();
          produce.liquid(Liquids.slag, 0.2f);

          draw = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(), new DrawDefault());
        }}
    );
    
    doOverrideContent(oldPulverizer = Blocks.pulverizer,
        Blocks.pulverizer = new NormalCrafter("pulverizer_override"){{
          requirements(Category.crafting, with(Items.copper, 30, Items.lead, 25));
          craftEffect = Fx.pulverize;
          updateEffect = Fx.pulverizeSmall;
          hasItems = hasPower = true;
          ambientSound = Sounds.grinding;
          ambientSoundVolume = 0.025f;
          autoSelect = true;
          canSelect = false;
          
          newConsume();
          consume.time(40);
          consume.item(Items.scrap, 1);
          consume.power(0.50f);
          newProduce();
          produce.item(Items.sand, 1);
          
          newConsume();
          consume.time(40);
          consume.item(SglItems.black_crystone, 1);
          consume.power(0.50f);
          newProduce();
          produce.item(Items.sand, 1);

          draw = new DrawMulti(
              new DrawDefault(),
              new DrawRegion("-rotator"){{
                spinSprite = true;
                rotateSpeed = 2f;
              }},
              new DrawRegion("-top")
          );
        }}
    );
  }
}
