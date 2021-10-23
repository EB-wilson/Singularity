package singularity.world.meta;

import arc.Core;
import arc.util.Strings;
import mindustry.ui.ItemDisplay;
import mindustry.ui.LiquidDisplay;
import mindustry.world.meta.StatValue;
import singularity.type.Gas;

public class SglStatValues{
  public static StatValue gasCompValue(Gas.CompressLiquid prod){
    return table -> {
      table.table(in -> {
        in.defaults().left();
        in.add(new LiquidDisplay(prod.liquid, 0, false));
        in.row();
        in.add(Core.bundle.get("misc.compressRequire") + ": " + Strings.autoFixed(prod.requirePressure*100, 2) + " kPa");
        in.row();
        in.add(Core.bundle.get("misc.compressRate") + ": " + Strings.autoFixed(prod.consumeGas, 2) + "/1");
      });
    };
  }
  
  public static StatValue gasCompValue(Gas.CompressItem prod){
    return table -> {
      table.table(in -> {
        in.defaults().left();
        in.add(new ItemDisplay(prod.item, 0, true));
        in.row();
        in.add(Core.bundle.get("misc.compressRequire") + ": " + Strings.autoFixed(prod.requirePressure*100, 2) + " kPa");
        in.row();
        in.add(Core.bundle.get("misc.compressRate") + ": " + Strings.autoFixed(prod.consumeGas, 2) + "/1");
      });
    };
  }
}
