package singularity.contents;

import arc.graphics.Color;
import mindustry.type.Item;
import singularity.Sgl;

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
  /**反物质*/
  anti_metter,
  /**铀饼*/
  uranium_cake,
  /**干冰*/
  dry_ice,
  /**绿藻块*/
  chlorella_block,
  /**绿藻素*/
  chlorella,
  /**焦炭*/
  coke,
  /**铱*/
  iridium,
  /**核废料*/
  nuclear_waste,
  /***/
  crush_ore,
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
      radioactivity = 0.4f;
      cost = 1.25f;
    }};
    
    crystal_FEX_power = new Item("crystal_FEX_power", Color.valueOf("#E34248")){{
      hardness = 3;
      explosiveness = 1.6f;
      flammability = 0f;
      radioactivity = 3f;
      cost = 1.35f;
    }};
    
    matrix_alloy = new Item("matrix_alloy", Color.valueOf("#929090")){{
      hardness = 4;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
      cost = 1.4f;
    }};
    
    strengthening_alloy = new Item("strengthening_alloy", Color.valueOf("#B1B1B0")){{
      hardness = 5;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
      cost = 1.25f;
    }};
    
    aerogel = new Item("aerogel", Color.valueOf("#D5EBEE")){{
      hardness = 3;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
      cost = 1.1f;
    }};
  
    degenerate_neutron_polymer = new Item("degenerate_neutron_polymer", Color.valueOf("#FF7FE0")){{
      hardness = 10;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
      cost = 3;
    }};
    
    uranium_238 = new Item("uranium_238", Color.valueOf("#7CA73D")){{
      hardness = 2;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.4f;
    }};
    
    uranium_235 = new Item("uranium_235", Color.valueOf("#B5D980")){{
      hardness = 2;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 1.6f;
    }};
    
    plutonium_239 = new Item("plutonium_239", Color.valueOf("#D1D19F")){{
      hardness = 2;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 1.6f;
    }};
    
    concentration_uranium_235 = new Item("concentration_uranium_235", Color.valueOf("#95B564")){{
      hardness = 4;
      explosiveness = 12f;
      flammability = 0f;
      radioactivity = 2.4f;
    }};
    
    concentration_plutonium_239 = new Item("concentration_plutonium_239", Color.valueOf("#B0B074")){{
      hardness = 4;
      explosiveness = 12f;
      flammability = 0f;
      radioactivity = 2.4f;
    }};

    anti_metter = new Item("anti_metter", Color.valueOf("734CD2")){{
      hardness = 12;
      explosiveness = 64;
      flammability = 0;
      radioactivity = 0;
    }};
    
    uranium_cake = new Item("uranium_cake", Color.valueOf("#E67D53")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    dry_ice = new SglItem("dry_ice", Color.valueOf("#EDF0ED")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
      
      temperature = 212;
      heatCapacity = 620;
    }};
  
    chlorella_block = new Item("chlorella_block", Color.valueOf("#6CB855")){{
      hardness = 1;
      explosiveness = 0.4f;
      flammability = 1.2f;
      radioactivity = 0f;
    }};
  
    chlorella = new Item("chlorella", Color.valueOf("#7BD261")){{
      hardness = 1;
      explosiveness = 1.2f;
      flammability = 1.6f;
      radioactivity = 0f;
    }};
    
    coke = new Item("coke", Color.valueOf("#6A6A69")){{
      hardness = 1;
      explosiveness = 1.5f;
      flammability = 1.8f;
      radioactivity = 0f;
    }};
    
    iridium = new Item("iridium", Color.valueOf("#E4EFEF")){{
      hardness = 6;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
      cost = 2.5f;
    }};
    
    nuclear_waste = new Item("nuclear_waste", Color.valueOf("#AAB3AE")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.25f;
    }};
  
    crush_ore = new Item("crush_ore", Color.gray){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    crush_uranium_ore = new Item("crush_uranium_ore", Color.valueOf("#4D6D15FF")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    salt_uranium = new Item("salt_uranium", Color.valueOf("#4D940C")){{
      hardness = 0;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.1f;
    }};
    
    salt_iridium = new Item("salt_iridium", Color.valueOf("#D8E1E1")){{
      hardness = 0;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
  }
  
  public static class SglItem extends Item{
    public float temperature = -1;
    public float heatCapacity = 550;
    
    public SglItem(String name, Color color){
      super(name, color);
    }
    
    public float getTemperature(){
      return temperature > 0? temperature: Sgl.atmospheres.current.getAbsTemperature();
    }
  }
}
