package singularity.contents.override;

import arc.graphics.g2d.TextureRegion;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import singularity.contents.SglItems;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.draw.DrawFactory;
import universecore.util.OverrideContentList;

import static mindustry.type.ItemStack.with;

public class OverrideBlocks implements OverrideContentList{
  @Override
  public void load(){
    doOverrideContent(Blocks.melter,
        new NormalCrafter("melter_override"){{
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
          consume.items(ItemStack.with(SglItems.crush_ore, 1));
          consume.power(1f);
          consume.time(10);
          newProduce();
          produce.liquid(Liquids.slag, 0.2f);
          
          draw = new DrawFactory<>(this);
        }}
    );
    
    doOverrideContent(Blocks.pulverizer,
        new NormalCrafter("pulverizer_override"){{
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
          consume.item(SglItems.crush_ore, 1);
          consume.power(0.50f);
          newProduce();
          produce.item(Items.sand, 1);
          
          draw = new DrawFactory<NormalCrafterBuild>(this){
            {iconRotator = true;}
      
            @Override
            public TextureRegion[] icons(){
              return new TextureRegion[]{
                  region,
                  rotator,
                  top
              };
            }
          };
        }}
    );
  }
}
