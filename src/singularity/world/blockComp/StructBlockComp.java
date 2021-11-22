package singularity.world.blockComp;

public interface StructBlockComp extends ChainsBlockComp{
  @Override
  default boolean chainable(ChainsBlockComp other){
    return other instanceof StructBlockComp;
  }
}
