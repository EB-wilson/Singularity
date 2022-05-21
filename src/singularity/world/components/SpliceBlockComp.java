package singularity.world.components;

import arc.math.geom.Geometry;
import mindustry.Vars;
import mindustry.world.Tile;

import java.util.Arrays;

public interface SpliceBlockComp extends ChainsBlockComp{
  default boolean[] getSplice(Tile tile){
    boolean[] result = new boolean[8];
    
    for(int i=0; i<8; i++){
      Tile other = Vars.world.tile(tile.x + Geometry.d8(i).x, tile.y + Geometry.d8(i).y);
      if(other.block() instanceof ChainsBlockComp
          && (tile.build instanceof SpliceBuildComp && other.build instanceof SpliceBuildComp?
            (((SpliceBuildComp) tile.build).chains().container == ((SpliceBuildComp) other.build).chains().container)
          : chainable((ChainsBlockComp) other.block()))) result[i] = true;
    }
    
    return result;
  }
  
  default int[] getRegionBits(Tile tile, boolean interCorner){
    int[] result = new int[8];
    Arrays.fill(result, -1);
    
    boolean[] data = getSplice(tile);
  
    for(int part = 0; part < 8; part++){
      if(part < 4){
        result[part] = !data[(part*2) % 8]? 0: -1;
      }
      else{
        int i = (part - 4)*2, b = (i+2)%8;
        result[part] = !data[i] && !data[b]? 0: data[i] && (interCorner || !data[i+1]) && data[b]? 1: -1;
      }
    }
    
    return result;
  }
}
