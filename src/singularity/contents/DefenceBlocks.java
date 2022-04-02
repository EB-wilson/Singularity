package singularity.contents;

import mindustry.ctype.ContentList;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Wall;
import singularity.world.blocks.defence.SglWall;

public class DefenceBlocks implements ContentList{
  public static Block strengthening_alloy_wall,
  
  strengthening_alloy_wall_large,
  
  neutron_polymer_wall,
  
  neutron_polymer_wall_large;
  
  @Override
  public void load(){
    strengthening_alloy_wall = new Wall("strengthening_alloy_wall"){{
      requirements(Category.defense, ItemStack.with(SglItems.strengthening_alloy, 8));
      health = 900;
    }};
    
    strengthening_alloy_wall_large = new Wall("strengthening_alloy_wall_large"){{
      requirements(Category.defense, ItemStack.with(SglItems.strengthening_alloy, 32));
      size = 2;
      health = 900*4;
    }};
    
    neutron_polymer_wall = new SglWall("neutron_polymer_wall"){{
      requirements(Category.defense, ItemStack.with(SglItems.degenerate_neutron_polymer, 8, SglItems.strengthening_alloy, 4));
      health = 2400;
      damageFilter = 62;
    }};
    
    neutron_polymer_wall_large = new SglWall("neutron_polymer_wall_large"){{
      requirements(Category.defense, ItemStack.with(SglItems.degenerate_neutron_polymer, 32, SglItems.strengthening_alloy, 16, SglItems.aerogel, 8));
      size = 2;
      health = 2400*4;
      damageFilter = 85;
    }};
  }
}
