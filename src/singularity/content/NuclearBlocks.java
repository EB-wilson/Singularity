package singularity.content;

import singularity.type.SglCategory;
import singularity.world.blocks.nuclear.EnergySource;
import singularity.world.blocks.nuclear.NuclearPipeNode;
import mindustry.content.Items;
import mindustry.ctype.ContentList;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.BuildVisibility;

public class NuclearBlocks implements ContentList{
  /**核能管道*/
  public static Block nuclear_pipe,
  /**核能源*/
  nuclear_energy_source;
  
  @Override
  public void load(){
    nuclear_pipe = new NuclearPipeNode("nuclear_pipe_node"){{
      requirements(SglCategory.nuclear, ItemStack.with(Items.titanium, 75));
      group = BlockGroup.transportation;
    }};
  
    nuclear_energy_source = new EnergySource("nuclear_energy_source"){{
      requirements(SglCategory.nuclear, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
  }
}
