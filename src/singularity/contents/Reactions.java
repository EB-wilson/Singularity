package singularity.contents;

import mindustry.content.Items;
import mindustry.ctype.ContentList;
import mindustry.type.Item;
import mindustry.type.Liquid;
import singularity.type.Reaction;

public class Reactions implements ContentList{
  public Reaction<Item, Item, Item> sand_coke_si, si_glass_aerogel;
  public Reaction<Item, Liquid, Item> uranCrush_chemGel_uranCake;
  public Reaction<Item, Liquid, Liquid> iridSalt_chemGel_iridium;
  
  @Override
  public void load(){
    sand_coke_si = new Reaction<>(
        Items.sand, 5,
        SglItems.coke, 1,
        Items.silicon, 3
    ){{
      deltaHeat = 35215f;
      requireTemperature = 703f;
      requirePressure = 10.27f;
    }};
    
    si_glass_aerogel = new Reaction<>(
        Items.silicon, 1,
        Items.metaglass, 2,
        SglItems.aerogel, 2
    ){{
      reactTime = 120;
      deltaHeat = 52809f;
      requireTemperature = 389f;
      requirePressure = 5.85f;
    }};
    
    uranCrush_chemGel_uranCake = new Reaction<>(
        SglItems.crush_uranium_ore, 1,
        SglLiquids.mixed_chemical_gel, 12,
        SglItems.uranium_cake, 2
    ){{
      reactTime = 30;
      deltaHeat = 52204f;
      requireTemperature = 568f;
      requirePressure = 7.52f;
    }};
    
    iridSalt_chemGel_iridium = new Reaction<>(
        SglItems.salt_iridium, 1,
        SglLiquids.mixed_chemical_gel, 12,
        SglLiquids.iridium_gel, 12
    ){{
      reactTime = 90;
      deltaHeat = 41247f;
      requireTemperature = 790f;
      requirePressure = 12.28f;
    }};
  }
}
