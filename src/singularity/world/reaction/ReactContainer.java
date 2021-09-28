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
  default ReactionModule reacts(){
    return getField(ReactionModule.class, "reacts");
  }
  
  default ItemModule items(){
    return getField(ItemModule.class, "items");
  }
  
  default LiquidModule liquids(){
    return getField(LiquidModule.class, "liquids");
  }
  
  default GasesModule gases(){
    return getField(GasesModule.class, "gases");
  }
  
  default float heat(){
    return getField(float.class, "heat");
  }
  
  float pressure();
  
  @Override
  default int tileY() {
    return World.toTile(y());
  }
  
  @Override
  default float getX(){
    return getField(int.class, "x");
  }
  
  void heat(float heat);
  
  default Tile tile(){
    return getField(Tile.class, "tile");
  }
  
  @Override
  default int tileX() {
    return World.toTile(x());
  }
  @Override
  default float getY(){
    return getField(int.class, "y");
  }
  
  @Override
  default float x(){
    return getField(int.class, "x");
  }
  
  @Override
  default float y(){
    return getField(int.class, "y");
  }
  
  @Override
  default int id(){
    return getField(int.class, "id");
  }
}
