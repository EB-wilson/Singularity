package singularity.world.components;

import arc.math.geom.Point2;
import singularity.world.DirEdges;
import universecore.annotations.Annotations;

import java.util.Arrays;

public interface SpliceBuildComp extends ChainsBuildComp{
  @Annotations.BindField("splice")
  default int[] splice(){
    return null;
  }

  @Annotations.BindField("splice")
  default void splice(int[] arr){}

  default boolean[] getSplice(){
    boolean[] result = new boolean[8];

    t: for(int i=0; i<8; i++){
      SpliceBuildComp other = null;
      for(Point2 p: DirEdges.get8(getBlock().size, i)){
        if(other == null){
          if(getBuilding().nearby(p.x, p.y) instanceof SpliceBuildComp oth && oth.chains().container == chains().container){
            other = oth;
          }
          else{
            result[i] = false;
            continue t;
          }
        }
        else if(other != getBuilding().nearby(p.x, p.y)){
          result[i] = false;
          continue t;
        }
      }
      result[i] = true;
    }

    return result;
  }

  default int[] getRegionBits(boolean interCorner){
    int[] result = new int[8];
    Arrays.fill(result, -1);

    boolean[] data = getSplice();

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

  @Annotations.MethodEntry(entryMethod = "onProximityUpdate")
  default void updateRegionBit(){
    splice(getRegionBits(getBlock(SpliceBlockComp.class).interCorner()));
  }
}
