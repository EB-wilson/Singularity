package singularity.contents;

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
      deltaHeat = 2.35f;
      requireTemperature = 2.03f;
      requirePressure = 10.27f;
    }};
    
    uranCrush_chemGel_uranCake = new Reaction<>(
        SglItems.crush_uranium_ore, 1,
        SglLiquids.mixed_chemical_gel, 12,
        SglItems.uranium_cake, 2
    ){{
      reactTime = 30;
      deltaHeat = 1.52f;
      requireTemperature = 2.18f;
      requirePressure = 7.52f;
    }};
    
    iridSalt_chemGel_iridium = new Reaction<>(
        SglItems.salt_iridium, 1,
        SglLiquids.mixed_chemical_gel, 12,
        SglLiquids.iridium_gel, 12
    ){{
      reactTime = 90;
      deltaHeat = 3.01f;
      requireTemperature = 2.8f;
      requirePressure = 12.28f;
    }};
  }
}
