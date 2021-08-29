package singularity.content;

import singularity.type.Gas;
import arc.graphics.Color;
import mindustry.ctype.ContentList;

@SuppressWarnings("SpellCheckingInspection")
public class Gases implements ContentList{
  public static Gas
  CH4,
  CO2,
  CO,
  Cl2,
  H2,
  N2,
  O2;
  
  @Override
  public void load(){
    CH4 = new Gas("CH4", Color.valueOf("#865A7A")){{
      heatCapacity = 0.3f;
      explosiveness = 1f;
      flammability = 1f;
      temperature = 0.4f;
  
      creatTank(24);
    }};
  
    CO2 = new Gas("CO2", Color.valueOf("#C4C4C4")){{
      heatCapacity = 0.8f;
      explosiveness = 0f;
      flammability = 0f;
      temperature = 0.4f;
      
      setCompressItem(SglItems.dry_ice, 1.8f,24);
    }};
  
    H2 = new Gas("H2", Color.valueOf("#71D3E2")){{
      heatCapacity = 0.1f;
      explosiveness = 1f;
      flammability = 1f;
      temperature = 0.4f;
  
      creatTank(24);
    }};
  
    O2 = new Gas("O2", Color.valueOf("#CCFCFF")){{
      heatCapacity = 0.5f;
      explosiveness = 1f;
      flammability = 1f;
      temperature = 0.4f;
      
      creatTank(24);
    }};
  
    Cl2 = new Gas("Cl2", Color.valueOf("#D7FFAD")){{
      heatCapacity = 0.2f;
      explosiveness = 0.5f;
      flammability = 0.6f;
      temperature = 0.5f;
  
      creatTank(24);
    }};
  
    N2 = new Gas("N2", Color.valueOf("#FFFFFF")){{
      heatCapacity = 1f;
      explosiveness = 0f;
      flammability = 0f;
      temperature = 0.4f;
  
      creatTank(24);
    }};
  
    CO = new Gas("CO", Color.valueOf("#F1D8FF")){{
      heatCapacity = 0.3f;
      explosiveness = 0.9f;
      flammability = 0.9f;
      temperature = 0.5f;
  
      creatTank(24);
    }};
  }
}
