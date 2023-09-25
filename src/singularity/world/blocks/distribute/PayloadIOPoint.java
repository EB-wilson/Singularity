package singularity.world.blocks.distribute;

import mindustry.ctype.Content;
import singularity.world.components.PayloadBuildComp;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.distribution.GridChildType;
import universecore.annotations.Annotations;

@Annotations.ImplEntries
public class PayloadIOPoint extends IOPoint {
  public PayloadIOPoint(String name){
    super(name);

    outputsPayload = acceptsPayload = true;
  }

  @Override
  public void setupRequestFact(){

  }

  @Annotations.ImplEntries
  public class PayloadIOPointBuild extends IOPointBuild implements PayloadBuildComp{

    @Override
    protected void transBack(){

    }

    @Override
    protected void resourcesSiphon(){

    }

    @Override
    protected void resourcesDump(){

    }

    @Override
    public boolean valid(DistMatrixUnitBuildComp unit, GridChildType type, Content content){
      return false;
    }

  }
}
