package singularity.world.components.distnet;

import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import singularity.world.blocks.distribute.TargetConfigure;
import singularity.world.distribution.GridChildType;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;

public interface IOPointComp extends BuildCompBase{
  @Annotations.BindField("parent")
  default DistMatrixUnitBuildComp parent(){
    return null;
  }

  @Annotations.BindField("parent")
  default void parent(DistMatrixUnitBuildComp valur){}

  @Annotations.BindField("config")
  default TargetConfigure gridConfig(){
    return null;
  }

  @Annotations.BindField("config")
  default void gridConfig(TargetConfigure valur){}

  default IOPointBlockComp getIOBlock(){
    return getBlock(IOPointBlockComp.class);
  }

  GridChildType[] configTypes();

  ContentType[] configContentTypes();

  void applyConfig(TargetConfigure value);

  boolean valid(DistMatrixUnitBuildComp unit, GridChildType type, Content content);
}
