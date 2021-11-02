package singularity.contents;

import mindustry.ctype.ContentList;
import mindustry.world.Block;
import singularity.world.blocks.environment.SglOverlay;

public class Environments implements ContentList{
  public static Block methane_spring;
  
  @Override
  public void load(){
    methane_spring = new SglOverlay("methane_spring", Gases.CH4, 1.72f){{
      variants = 3;
      pumpable = true;
    }};
  }
}
