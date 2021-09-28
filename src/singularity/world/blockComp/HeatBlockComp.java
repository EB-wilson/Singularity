package singularity.world.blockComp;

import universeCore.entityComps.blockComps.FieldGetter;

public interface HeatBlockComp extends FieldGetter{
  default float maxTemperature(){
    return getField(float.class, "maxTemperature");
  }
}
