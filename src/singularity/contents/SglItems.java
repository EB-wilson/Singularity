package singularity.contents;

import arc.graphics.Color;
import mindustry.ctype.ContentList;
import mindustry.type.Item;

@SuppressWarnings("SpellCheckingInspection")
public class SglItems implements ContentList{
  /**FEX水晶*/
  public static Item crystal_FEX,
  /**充能FEX水晶*/
  crystal_FEX_power,
  /**矩阵合金*/
  matrix_alloy,
  /**强化合金*/
  strengthening_alloy,
  /**气凝胶*/
  aerogel,
  /**简并态中子聚合物*/
  degenerate_neutron_polymer,
  /**处理器*/
  cpu,
  /**铀238*/
  uranium_238,
  /**铀235*/
  uranium_235,
  /**钚239*/
  plutonium_239,
  /**浓缩铀235核燃料*/
  concentration_uranium_235,
  /**浓缩钚239核燃料*/
  concentration_plutonium_239,
  /**铀饼*/
  uranium_cake,
  /**干冰*/
  dry_ice,
  /**焦炭*/
  coke,
  /**铱*/
  iridium,
  /**核废料*/
  nuclear_waste,
  /**破碎铀矿石*/
  crush_uranium_ore,
  /**混合铀原料*/
  salt_uranium,
  /**混合铱盐*/
  salt_iridium;
  
  public void load(){
    crystal_FEX = new Item("crystal_FEX", Color.valueOf("#D2393E")){{
      hardness = 3;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.5f;
    }};
    
    crystal_FEX_power = new Item("crystal_FEX_power", Color.valueOf("#E34248")){{
      hardness = 3;
      explosiveness = 0.6f;
      flammability = 0f;
      radioactivity = 0.7f;
    }};
    
    matrix_alloy = new Item("matrix_alloy", Color.valueOf("#929090")){{
      hardness = 4;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    strengthening_alloy = new Item("strengthening_alloy", Color.valueOf("#B1B1B0")){{
      hardness = 4;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    aerogel = new Item("aerogel", Color.valueOf("#D5EBEE")){{
      hardness = 3;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
  
    degenerate_neutron_polymer = new Item("degenerate_neutron_polymer", Color.valueOf("#FF7FE0")){{
      hardness = 4;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    cpu = new Item("cpu", Color.valueOf("#FFEB00")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    
    uranium_238 = new Item("uranium_238", Color.valueOf("#348908")){{
      hardness = 2;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.3f;
    }};
    
    uranium_235 = new Item("uranium_235", Color.valueOf("#45A806")){{
      hardness = 2;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.8f;
    }};
    
    plutonium_239 = new Item("plutonium_239", Color.valueOf("#E3E37A")){{
      hardness = 2;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.8f;
    }};
    
    concentration_uranium_235 = new Item("concentration_uranium_235", Color.valueOf("#44A705")){{
      hardness = 4;
      explosiveness = 1f;
      flammability = 0f;
      radioactivity = 1f;
    }};
    
    concentration_plutonium_239 = new Item("concentration_plutonium_239", Color.valueOf("#E4E279")){{
      hardness = 4;
      explosiveness = 1f;
      flammability = 0f;
      radioactivity = 1f;
    }};
    
    uranium_cake = new Item("uranium_cake", Color.valueOf("#E67D53")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.1f;
    }};
    
    dry_ice = new Item("dry_ice", Color.valueOf("#EDF0ED")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    coke = new Item("coke", Color.valueOf("#6A6A69")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0.8f;
      radioactivity = 0f;
    }};
    
    iridium = new Item("iridium", Color.valueOf("#E4EFEF")){{
      hardness = 4;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    nuclear_waste = new Item("nuclear_waste", Color.valueOf("#918245")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.4f;
    }};
    
    crush_uranium_ore = new Item("crush_uranium_ore", Color.valueOf("#4D6D15FF")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.3f;
    }};
    
    salt_uranium = new Item("salt_uranium", Color.valueOf("#4D940C")){{
      hardness = 0;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.6f;
    }};
    
    salt_iridium = new Item("salt_iridium", Color.valueOf("#D8E1E1")){{
      hardness = 0;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
  }
}
