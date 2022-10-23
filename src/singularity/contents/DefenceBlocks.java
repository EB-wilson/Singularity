package singularity.contents;

import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import mindustry.world.draw.DrawRegion;
import singularity.world.blocks.defence.PhasedRadar;
import singularity.world.blocks.defence.SglWall;
import singularity.world.draw.DrawDirSpliceBlock;

public class DefenceBlocks implements ContentList{
  /**相控雷达*/
  public static Block phased_radar,
  /**强化合金墙*/
  strengthening_alloy_wall,
  /**大型强化合金墙*/
  strengthening_alloy_wall_large,
  /**简并态中子聚合物墙*/
  neutron_polymer_wall,
  /**大型简并态中子聚合物墙*/
  neutron_polymer_wall_large;
  
  @Override
  public void load(){
    phased_radar = new PhasedRadar("phased_radar"){{
      requirements(Category.effect, ItemStack.with());

      newConsume();
      consume.power(1);

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawDirSpliceBlock<PhasedRadarBuild>(){{
            simpleSpliceRegion = true;
            spliceBits = e -> {
              int res = 0;
              for(int i = 0; i < 4; i++){
                if(e.splice()[i] != -1) res |= 0b0001 << i;
              }
              return res;
            };
          }},
          new DrawRegion("_rotator"){{
            rotateSpeed = 0.4f;
          }}
      );
    }};

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
