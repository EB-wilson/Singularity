package singularity.content;

import mindustry.content.Items;
import mindustry.ctype.ContentList;
import mindustry.type.Item;
import mindustry.type.Liquid;
import singularity.type.Reaction;

public class Reactions implements ContentList{
  public Reaction<Item, Item, Item> sand_coke_si;
  public Reaction<Item, Liquid, Item> uranCrush_chemGel_uranCake;
  public Reaction<Item, Liquid, Liquid> iridSalt_chemGel_iridium;
  
  @Override
  public void load(){
    sand_coke_si = new Reaction<>(
        Items.sand, 5,
        SglItems.coke, 1,
        Items.silicon, 3
    ){{
      requireTemperature = 2.03f;
      requirePressure = 10.27f;
      deltaHeat = -0.71f;
    }};
    
    uranCrush_chemGel_uranCake = new Reaction<>(
        SglItems.coke, 1,
        SglLiquids.mixed_chemical_gel, 12,
        SglItems.uranium_cake, 2
    ){{
      requireTemperature = 2.18f;
      requirePressure = 7.52f;
      deltaHeat = -0.96f;
    }};
    
    iridSalt_chemGel_iridium = new Reaction<>(
        SglItems.salt_iridium, 1,
        SglLiquids.mixed_chemical_gel, 12,
        SglLiquids.iridium_gel, 12
    ){{
      requireTemperature = 2.8f;
      requirePressure = 12.28f;
      deltaHeat = -0.85f;
    }};
  }
}
