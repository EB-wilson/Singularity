package singularity.ui.fragments.override;

import arc.util.Log;
import mindustry.gen.Building;
import mindustry.ui.fragments.BlockInventoryFragment;

public class SglBlockInventoryFragment extends BlockInventoryFragment{
  private boolean shown = false;
  
  @Override
  public void showFor(Building tile){
    super.showFor(tile);
    if(!(tile == null || !tile.block.isAccessible() || tile.items.total() == 0)) shown = true;
  }
  
  @Override
  public void hide(){
    super.hide();
    shown = false;
  }
  
  public boolean statusSwitch(Building tile){
    if(shown){
      hide();
    }
    else showFor(tile);
    Log.info(shown);
    return shown;
  }
  
  public boolean isShown(){
    return shown;
  }
}
