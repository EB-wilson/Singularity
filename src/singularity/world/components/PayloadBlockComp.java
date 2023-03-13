package singularity.world.components;

import universecore.annotations.Annotations;

public interface PayloadBlockComp{
  @Annotations.BindField("payloadCapacity")
  default int payloadCapacity(){
    return 0;
  }

  @Annotations.BindField("payloadSpeed")
  default float payloadSpeed(){
    return 0;
  }

  @Annotations.BindField("payloadRotateSpeed")
  default float payloadRotateSpeed(){
    return 0;
  }
}
