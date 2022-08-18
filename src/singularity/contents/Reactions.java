package singularity.contents;

import mindustry.content.Items;
import mindustry.type.Item;
import mindustry.type.Liquid;
import singularity.type.Gas;
import singularity.type.Reaction;

@SuppressWarnings("SpellCheckingInspection")
public class Reactions implements ContentList{
  public static Reaction<Gas, Gas> H2_O2_H2O, CH4_O2_CO2_H2O;
  public static Reaction<Item, Item> sand_coke_si, si_glass_aerogel;
  public static Reaction<Item, Liquid> uranCrush_chemGel_uranCake, iridSalt_chemGel_iridium;
  public static Reaction<Item, Gas> coal_O2_CO2_SO2, pyra_O2_SO2;
  
  @Override
  public void load(){
    H2_O2_H2O = new Reaction<>(
        Gases.H2, 2,
        Gases.O2, 1,
        Gases.steam, 4
    ){{
      reactTime = 30;
      deltaHeat = -105268f;
      requireTemperature = 695f;
      requirePressure = 0.6f;
      chainsReact = true;
    }};
    
    CH4_O2_CO2_H2O = new Reaction<>(
        Gases.CH4, 1,
        Gases.O2, 2,
        Gases.steam, 6,
        Gases.CO2, 1
    ){{
      reactTime = 45;
      deltaHeat = -95628f;
      requireTemperature = 722f;
      requirePressure = 0.65f;
      chainsReact = true;
    }};
    
    coal_O2_CO2_SO2 = new Reaction<>(
        Items.coal, 1,
        Gases.O2, 8,
        Gases.SO2, 0.2f,
        Gases.CO2, 6
    ){{
      reactTime = 180;
      deltaHeat = -126584f;
      requireTemperature = 679f;
      requirePressure = 3.2f;
      chainsReact = true;
    }};
    
    pyra_O2_SO2 = new Reaction<>(
        Items.pyratite, 1,
        Gases.O2, 6,
        Gases.SO2, 6f
    ){{
      reactTime = 120;
      deltaHeat = -76584f;
      requireTemperature = 507f;
      requirePressure = 2.8f;
      chainsReact = true;
    }};
    
    sand_coke_si = new Reaction<>(
        Items.sand, 3,
        SglItems.coke, 1,
        Items.silicon, 2
    ){{
      reactTime = 90;
      deltaHeat = 55215f;
      requireTemperature = 703f;
      requirePressure = 10.27f;
    }};
    
    si_glass_aerogel = new Reaction<>(
        Items.silicon, 1,
        Items.metaglass, 2,
        SglItems.aerogel, 2
    ){{
      reactTime = 90;
      deltaHeat = 62809f;
      requireTemperature = 575f;
      requirePressure = 3.05f;
    }};
    
    uranCrush_chemGel_uranCake = new Reaction<>(
        SglItems.crush_uranium_ore, 1,
        SglLiquids.mixed_chemical_gel, 12,
        SglItems.uranium_cake, 2
    ){{
      reactTime = 90;
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
      requireTemperature = 1450f;
      requirePressure = 12.28f;
    }};
  }
}
