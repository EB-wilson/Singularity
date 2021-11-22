package singularity.contents;

import mindustry.graphics.Pal;
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
  O2,
  spore_cloud,
  vapor,
  steam;
  
  @Override
  public void load(){
    CH4 = new Gas("CH4", Color.valueOf("#865A7A")){{
      heatCapacity = 612f;
      flammability = 1f;
    }};
  
    CO2 = new Gas("CO2", Color.valueOf("#C4C4C4")){{
      heatCapacity = 548f;
      flammability = 0f;
      
      setCompressItem(SglItems.dry_ice, 9.8f,24);
    }};
  
    H2 = new Gas("H2", Color.valueOf("#71D3E2")){{
      heatCapacity = 271f;
      flammability = 1f;
    }};
  
    O2 = new Gas("O2", Color.valueOf("#CCFCFF")){{
      heatCapacity = 339f;
      flammability = 1f;
    }};
  
    Cl2 = new Gas("Cl2", Color.valueOf("#D7FFAD")){{
      heatCapacity = 302f;
      flammability = 0.6f;
    }};
  
    N2 = new Gas("N2", Color.valueOf("#FFFFFF")){{
      heatCapacity = 369f;
      flammability = 0f;
    }};
  
    CO = new Gas("CO", Color.valueOf("#F1D8FF")){{
      heatCapacity = 465f;
      flammability = 0.9f;
    }};
    
    spore_cloud = new Gas("spore_cloud", Pal.spore){{
      heatCapacity = 387f;
      flammability = 1.2f;
    }};
    
    vapor = new Gas("vapor", Color.white){{
      heatCapacity = 421;
      temperature = 292f;
      flammability = 0f;
    }};
    
    steam = new Gas("steam", Color.white){{
      heatCapacity = 421;
      temperature = 525f;
      flammability = 0f;
    }};
  }
}
