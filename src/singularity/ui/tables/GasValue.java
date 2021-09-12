package singularity.ui.tables;

import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.core.UI;
import mindustry.ui.ItemDisplay;
import mindustry.ui.Styles;
import mindustry.world.meta.StatUnit;
import singularity.type.Gas;

import static mindustry.Vars.iconMed;

public class GasValue extends Table{
  public Gas gas;
  public float amount;
  
  public GasValue(Gas gas, float amount){
    this(gas, amount, true, true);
  }
  
  public GasValue(Gas gas, float amount, boolean showName, boolean preSec){
    this.gas = gas;
    this.amount = amount;
    
    add(new Stack(){{
      add(new Image(gas.uiIcon));
    
      if(amount != 0){
        Table t = new Table().left().bottom();
        t.add(amount > 1000 ? UI.formatAmount(((Number)amount).longValue()) : Strings.autoFixed(amount, 2) + "").style(Styles.outlineLabel);
        add(t);
      }
    }}).size(iconMed).padRight(3  + (amount != 0 && Strings.autoFixed(amount, 2).length() > 2 ? 8 : 0));
    
    add().width(8);
    
    if(preSec){
      add(StatUnit.perSecond.localized()).padLeft(2).padRight(5).color(Color.lightGray).style(Styles.outlineLabel);
    }
  
    if(showName) add(gas.localizedName);
  }
}
