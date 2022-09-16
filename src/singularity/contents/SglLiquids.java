package singularity.contents;

import arc.graphics.Color;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;

@SuppressWarnings("SpellCheckingInspection")
public class SglLiquids implements ContentList{
  /**FEX流体*/
  public static Liquid FEX_liquid,
  /**相位态FEX流体*/
  phase_FEX_liquid,
  /**混合化工凝胶*/
  mixed_chemical_gel,
  /**富铱凝胶*/
  iridium_gel,
  /**润滑剂*/
  lubricant,
  /**藻泥*/
  algae_mud,
  /**岩层沥青*/
  rock_bitumen,
  /**混合焦油*/
  mixed_tar,
  /**燃油*/
  fuel_oil,

  //气体
  /**氧气*/
  oxygen,
  /**氯气*/
  chlorine,
  /**氦气*/
  helium,
  /**氧化硫*/
  sulfur_oxide,
  /**甲烷*/
  methane,
  /**孢子云*/
  spore_cloud;

  public void load(){
    FEX_liquid = new Liquid("FEX_liquid", Color.valueOf("#E34248")){{
      heatCapacity = 0.3f;
      explosiveness = 0f;
      flammability = 0f;
      temperature = 0.5f;
      viscosity = 0f;
    }};
    
    phase_FEX_liquid = new Liquid("phase_FEX_liquid", Color.valueOf("#E34248")){{
      heatCapacity = 1.25f;
      explosiveness = 0f;
      flammability = 0f;
      temperature = 0f;
      viscosity = 0f;
    }};
    
    mixed_chemical_gel = new Liquid("mixed_chemical_gel", Color.valueOf("#FEAEA5")){{
      heatCapacity = 0.7f;
      explosiveness = 0f;
      flammability = 1f;
      temperature = 0.5f;
      viscosity = 0.9f;
    }};
    
    iridium_gel = new Liquid("iridium_gel", Color.valueOf("#DAF3F3")){{
      heatCapacity = 0.75f;
      explosiveness = 0;
      flammability = 0.7f;
      temperature = 0.55f;
      viscosity = 0.9f;
    }};
    
    lubricant = new Liquid("lubricant", Color.valueOf("#FFD662")){{
      heatCapacity = 1f;
      explosiveness = 0.4f;
      flammability = 0f;
      temperature = 0.4f;
      viscosity = 0.2f;
    }};
  
    algae_mud = new Liquid("algae_mud", Color.valueOf("#6EA145")){{
      heatCapacity = 0.4f;
      explosiveness = 0f;
      flammability = 0f;
      temperature = 0.5f;
      viscosity = 0.5f;
    }};
    
    rock_bitumen = new Liquid("rock_bitumen", Color.valueOf("#808A73")){{
      heatCapacity = 0.6f;
      explosiveness = 0f;
      flammability = 0f;
      temperature = 0.6f;
      viscosity = 0.95f;
    }};
    
    mixed_tar = new Liquid("mixed_tar", Color.valueOf("#F0E13D")){{
      heatCapacity = 0.2f;
      explosiveness = 0.5f;
      flammability = 0.7f;
      temperature = 0.6f;
      viscosity = 0.625f;
    }};
    
    fuel_oil = new Liquid("fuel_oil", Color.valueOf("#F8FFBE")){{
      heatCapacity = 0.1f;
      explosiveness = 0.75f;
      flammability = 1f;
      temperature = 0.4f;
      viscosity = 0.4f;
    }};

    //气体
    oxygen = new Liquid("oxygen", Color.white){{
      gas = true;

      heatCapacity = 0.2f;
      explosiveness = 0.8f;
      flammability = 0.5f;
      temperature = 0.4f;
      viscosity = 0f;
    }};

    chlorine = new Liquid("chlorine", Color.white){{
      gas = true;

      heatCapacity = 0.35f;
      explosiveness = 0.3f;
      flammability = 0.2f;
      temperature = 0.4f;
      viscosity = 0f;
    }};

    helium = new Liquid("helium", Color.white){{
      gas = true;

      heatCapacity = 0.6f;
      explosiveness = 0;
      flammability = 0;
      temperature = 0.4f;
      viscosity = 0;
    }};

    sulfur_oxide = new Liquid("sulfur_oxide", Color.white){{
      gas = true;

      heatCapacity = 0.65f;
      explosiveness = 0;
      flammability = 0;
      temperature = 0.4f;
      viscosity = 0;
    }};

    methane = new Liquid("methane", Color.white){{
      gas = true;

      heatCapacity = 0.8f;
      explosiveness = 1f;
      flammability = 0.85f;
      temperature = 0.4f;
      viscosity = 0;
    }};

    spore_cloud = new Liquid("spore_cloud", Pal.spore){{
      gas = true;

      heatCapacity = 0.3f;
      explosiveness = 0.8f;
      flammability = 0.75f;
      temperature = 0.4f;
      viscosity = 0;
    }};
  }
}
