package singularity.world.components.distnet;

import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import singularity.world.blocks.distribute.TargetConfigure;
import singularity.world.distribution.GridChildType;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;

public interface IOPointComp extends BuildCompBase{
  @Annotations.BindField("parentMat")
  default DistMatrixUnitBuildComp parent(){
    return null;
  }

  @Annotations.BindField("parentMat")
  default void parent(DistMatrixUnitBuildComp valur){}

  @Annotations.BindField("config")
  default TargetConfigure gridConfig(){
    return null;
  }

  @Annotations.BindField("config")
  default void gridConfig(TargetConfigure value){}

  default IOPointBlockComp getIOBlock(){
    return getBlock(IOPointBlockComp.class);
  }

  default GridChildType[] configTypes(){
    return getIOBlock().configTypes().toSeq().toArray(GridChildType.class);
  }

  default ContentType[] configContentTypes(){
    return getIOBlock().supportContentType().toSeq().toArray(ContentType.class);
  }

  boolean valid(DistMatrixUnitBuildComp unit, GridChildType type, Content content);
}
