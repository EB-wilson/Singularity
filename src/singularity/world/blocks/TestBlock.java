package singularity.world.blocks;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.blocks.logic.MemoryBlock;

public class TestBlock extends SglBlock{
  public TestBlock(String name){
    super(name);
    update = true;
    solid = true;

  }

  public class TestBlockBuild extends SglBuilding{

  }
}
