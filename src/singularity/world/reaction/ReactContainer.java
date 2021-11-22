package singularity.world.reaction;

import mindustry.core.World;
import mindustry.gen.Posc;
import mindustry.world.Tile;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blockComp.HeatBuildComp;
import singularity.world.modules.GasesModule;
import singularity.world.modules.ReactionModule;

public interface ReactContainer extends Posc, HeatBuildComp, GasBuildComp{
  ReactionModule reacts();
  
  ItemModule items();
  
  LiquidModule liquids();
  
  GasesModule gases();
  
  float heat();
  
  @Override
  default int tileY() {
    return World.toTile(y());
  }
  
  @Override
  float getX();
  
  void heat(float heat);
  
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
