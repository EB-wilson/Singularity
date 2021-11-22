package singularity.world.blockComp;

public interface ChainsBlockComp{
  default boolean chainable(ChainsBlockComp other){
    return getClass().isAssignableFrom(other.getClass());
  }
}
