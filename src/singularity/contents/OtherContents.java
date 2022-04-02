package singularity.contents;

import mindustry.content.Items;
import mindustry.ctype.ContentList;
import singularity.type.AtomSchematic;

public class OtherContents implements ContentList{
  public static AtomSchematic copper_schematic,
      lead_schematic,
      silicon_schematic,
      titanium_schematic,
      thorium_schematic,
      uranium_schematic,
      iridium_schematic;

  @Override
  public void load(){
    copper_schematic = new AtomSchematic(Items.copper, 14000, CrafterBlocks.destructor){{
      request.medium(0.23f);
      request.time(30);
    }};

    lead_schematic = new AtomSchematic(Items.lead, 14000, copper_schematic){{
      request.medium(0.26f);
      request.time(30);
    }};

    silicon_schematic = new AtomSchematic(Items.silicon, 18000, lead_schematic){{
      request.medium(0.41f);
      request.item(Items.sand, 1);
      request.time(45);
    }};
  }
}
