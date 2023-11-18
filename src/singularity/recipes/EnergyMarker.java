package singularity.recipes;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import singularity.graphic.SglDrawConst;
import tmi.TooManyItems;
import tmi.recipe.types.RecipeItem;

public class EnergyMarker extends RecipeItem<String> {
  public static final EnergyMarker INSTANCE = TooManyItems.itemsManager.addItemWrap("energy_marker", new EnergyMarker());

  private EnergyMarker(){
    super("energy_marker");
  }

  @Override
  public int ordinal() {
    return -1;
  }

  @Override
  public int typeID() {
    return -1;
  }

  @Override
  public String name() {
    return item;
  }

  @Override
  public String localizedName() {
    return Core.bundle.get("category.neutron");
  }

  @Override
  public TextureRegion icon() {
    return ((TextureRegionDrawable) SglDrawConst.nuclearIcon).getRegion();
  }

  @Override
  public boolean hidden() {
    return false;
  }
}
