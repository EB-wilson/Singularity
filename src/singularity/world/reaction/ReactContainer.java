package singularity.world.reaction;

import mindustry.core.World;
import mindustry.gen.Posc;
import mindustry.world.Tile;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.world.modules.GasesModule;
import universeCore.entityComps.blockComps.FieldGetter;

public interface ReactContainer extends Posc, FieldGetter{
  ItemModule inItems();
  LiquidModule inLiquids();
  GasesModule inGases();
  
  ItemModule outItems();
  LiquidModule outLiquids();
  GasesModule outGases();
  
  default float heat(){
    return getField(float.class, "heat");
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
  default int tileY() {
    return World.toTile(y());
  }
  
  @Override
  default float getX(){
    return getField(int.class, "x");
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
