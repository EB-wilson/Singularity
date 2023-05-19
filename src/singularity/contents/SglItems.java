package singularity.contents;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.util.Time;
import mindustry.type.Item;
import singularity.core.UpdatePool;

@SuppressWarnings("SpellCheckingInspection")
public class SglItems implements ContentList{
  /**铝*/
  public static Item aluminium,
  /**FEX水晶*/
  crystal_FEX,
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
  /**铀238*/
  uranium_238,
  /**铀235*/
  uranium_235,
  /**钚239*/
  plutonium_239,
  /**相位封装氢单元*/
  encapsulated_hydrogen_cell,
  /**相位封装氦单元*/
  encapsulated_helium_cell,
  /**浓缩铀235核燃料*/
  concentration_uranium_235,
  /**浓缩钚239核燃料*/
  concentration_plutonium_239,
  /**氢聚变燃料*/
  hydrogen_fusion_fuel,
  /**氦聚变燃料*/
  helium_fusion_fuel,
  /**反物质*/
  anti_metter,
  /**绿藻块*/
  chlorella_block,
  /**绿藻素*/
  chlorella,
  /**碱石*/
  alkali_stone,
  /**絮凝剂*/
  flocculant,
  /**焦炭*/
  coke,
  /**铱*/
  iridium,
  /**核废料*/
  nuclear_waste,
  /**黑晶石*/
  black_crystone,
  /**岩层沥青*/
  rock_bitumen,
  /**铀原矿*/
  uranium_rawore,
  /**铀原料*/
  uranium_rawmaterial,
  /**铱金混合物*/
  iridium_mixed_rawmaterial,
  /**氯铱酸盐*/
  iridium_chloride;

  public void load(){
    aluminium = new Item("aluminium", Color.valueOf("#C0ECFF")){{
      hardness = 3;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
      cost = 0.9f;
    }};

    crystal_FEX = new Item("crystal_FEX", Color.valueOf("#D2393E")){{
      hardness = 3;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.4f;
      cost = 1.25f;
    }};
    
    crystal_FEX_power = new Item("crystal_FEX_power", Color.valueOf("#E34248")){
      {
        hardness = 3;
        explosiveness = 3.6f;
        flammability = 0f;
        radioactivity = 3f;
        cost = 1.35f;

        frameTime = 9;
      }

      @Override
      public void loadIcon(){
        super.loadIcon();
        TextureRegion[] regions = new TextureRegion[18];

        for(int i = 0; i < 10; i++){
          regions[i] = Core.atlas.find(name + "_" + i);
          if(i != 0 && i != 9) regions[regions.length - i] = regions[i];
        }

        fullIcon = new TextureRegion(fullIcon);
        uiIcon = new TextureRegion(uiIcon);

        UpdatePool.receive("dynamicIcon-" + name, () -> {
          int frame = (int)(Time.globalTime / frameTime) % regions.length;

          fullIcon.set(regions[frame]);
          uiIcon.set(regions[frame]);
        });
      }
    };
    
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
      cost = 1.5f;
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

    encapsulated_hydrogen_cell = new Item("encapsulated_hydrogen_cell", Color.valueOf("#9EFFC6")){{
      hardness = 2;
      explosiveness = 2.4f;
      flammability = 1.8f;
      radioactivity = 0;
    }};

    encapsulated_helium_cell = new Item("encapsulated_helium_cell", Color.valueOf("#F9FFDE")){{
      hardness = 2;
      explosiveness = 0.3f;
      flammability = 0;
      radioactivity = 0;
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

    hydrogen_fusion_fuel = new Item("hydrogen_fusion_fuel", Color.valueOf("#83D6A0")){{
      hardness = 2;
      explosiveness = 2.4f;
      flammability = 1.8f;
      radioactivity = 0;
    }};

    helium_fusion_fuel = new Item("helium_fusion_fuel", Color.valueOf("#D0D6B7")){{
      hardness = 2;
      explosiveness = 0.3f;
      flammability = 0;
      radioactivity = 0;
    }};

    anti_metter = new Item("anti_metter", Color.valueOf("734CD2")){{
      hardness = 12;
      explosiveness = 64;
      flammability = 0;
      radioactivity = 0;
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

    alkali_stone = new Item("alkali_stone", Color.valueOf("#B0BAC0")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};

    coke = new Item("coke", Color.valueOf("#6A6A69")){{
      hardness = 1;
      explosiveness = 1.5f;
      flammability = 1.8f;
      radioactivity = 0f;
    }};

    flocculant = new Item("flocculant", Color.white){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
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
  
    black_crystone = new Item("black_crystone", Color.gray){{
      hardness = 3;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};

    rock_bitumen = new Item("rock_bitumen", Color.valueOf("#808A73")){{
      hardness = 1;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
    
    uranium_rawore = new Item("uranium_rawore", Color.valueOf("#95B564")){{
      hardness = 4;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.04f;
    }};
    
    uranium_rawmaterial = new Item("uranium_rawmaterial", Color.valueOf("#B5D980")){{
      hardness = 0;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0.1f;
    }};
    
    iridium_mixed_rawmaterial = new Item("iridium_mixed_rawmaterial", Color.valueOf("#AECBCB")){{
      hardness = 0;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};

    iridium_chloride = new Item("iridium_chloride", Color.valueOf("#CBE0E0")){{
      hardness = 0;
      explosiveness = 0f;
      flammability = 0f;
      radioactivity = 0f;
    }};
  }
}
