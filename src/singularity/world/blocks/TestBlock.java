package singularity.world.blocks;

public class TestBlock extends SglBlock{
  public TestBlock(String name){
    super(name);
    update = true;
    solid = true;
  }

  public class TestBlockBuild extends SglBuilding{}
}
