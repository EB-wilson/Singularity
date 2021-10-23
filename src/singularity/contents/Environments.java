package singularity.contents;

import mindustry.ctype.ContentList;
import mindustry.world.Block;
import singularity.world.blocks.environment.SglOverlay;

public class Environments implements ContentList{
  public static Block methaneSpring;
  
  @Override
  public void load(){
    methaneSpring = new SglOverlay("methaneSpring", Gases.CH4, 1.72f){{
      variants = 3;
      pumpable = true;
    }};
  }
}
