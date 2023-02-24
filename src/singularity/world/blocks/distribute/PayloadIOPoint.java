package singularity.world.blocks.distribute;

import mindustry.ctype.Content;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.distribution.GridChildType;

public class PayloadIOPoint extends IOPoint{
  public PayloadIOPoint(String name){
    super(name);

    outputsPayload = acceptsPayload = true;
  }

  @Override
  public void setupRequestFact(){

  }

  public class PayloadIOPointBuild extends IOPointBuild{

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
