package singularity.contents;

import arc.graphics.Color;
import mindustry.ctype.ContentList;
import mindustry.type.Liquid;

@SuppressWarnings("SpellCheckingInspection")
public class SglLiquids implements ContentList{
  /**FEX流体*/
  public static Liquid liquid_FEX,
  /**混合化工凝胶*/
  mixed_chemical_gel,
  /**富铱凝胶*/
  iridium_gel,
  /**润滑剂*/
  lubricant,
  /**岩层沥青*/
  rock_bitumen,
  /**混合焦油*/
  mixed_tar,
  /**燃油*/
  fuel_oil;

  public void load(){
    liquid_FEX = new Liquid("liquid_FEX", Color.valueOf("#E34248")){{
      heatCapacity = 0.7f;
      explosiveness = 0f;
      flammability = 0f;
      temperature = 1f;
      viscosity = 0f;
    }};
    
    mixed_chemical_gel = new Liquid("mixed_chemical_gel", Color.valueOf("#FEAEA5")){{
      heatCapacity = 0.2f;
      explosiveness = 1f;
      flammability = 1f;
      temperature = 0.8f;
      viscosity = 1f;
    }};
    
    iridium_gel = new Liquid("iridium_gel", Color.valueOf("#DAF3F3")){{
      heatCapacity = 0.42f;
      explosiveness = 0;
      flammability = 0.07f;
      temperature = 1.2f;
      viscosity = 1f;
    }};
    
    lubricant = new Liquid("lubricant", Color.valueOf("#FFD662")){{
      heatCapacity = 0.3f;
      explosiveness = 0.4f;
      flammability = 0f;
      temperature = 0.4f;
      viscosity = 0.2f;
    }};
    
    rock_bitumen = new Liquid("rock_bitumen", Color.valueOf("#808A73")){{
      heatCapacity = 1f;
      explosiveness = 0f;
      flammability = 0f;
      temperature = 1f;
      viscosity = 1f;
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
  }
}
