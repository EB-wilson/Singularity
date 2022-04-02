package singularity.world.reaction;

import mindustry.core.World;
import mindustry.gen.Posc;
import mindustry.world.Tile;
import singularity.world.components.GasBuildComp;
import singularity.world.components.HeatBuildComp;
import singularity.world.modules.ReactionModule;

public interface ReactContainer extends Posc, HeatBuildComp, GasBuildComp{
  ReactionModule reacts();
  
  @Override
  default int tileY() {
    return World.toTile(y());
  }
  
  @Override
  float getX();
  
  Tile tile();
  
  @Override
  default int tileX() {
    return World.toTile(x());
  }
  
  @Override
  float getY();
  
  @Override
  float x();
  
  @Override
  float y();
  
  @Override
  int id();
}
