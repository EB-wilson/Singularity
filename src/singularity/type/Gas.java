package singularity.type;

import mindustry.type.Liquid;
import mindustry.ui.ItemDisplay;
import mindustry.ui.LiquidDisplay;
import singularity.Singularity;
import singularity.ui.tables.GasValue;
import singularity.world.meta.SglStat;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.graphics.MultiPacker;
import mindustry.graphics.MultiPacker.PageType;
import mindustry.type.Item;

public class Gas extends UnlockableContent{
  public final Item compressItem;
  public final Liquid compressLiquid;
  public final boolean compressible;
  public final Color color;
  public final float compressRequire;
  
  public Item tank;
  public boolean hasTank = false;
  public float tankContains;
  
  public float criticalPressure = 1.5f;
  public float heatCapacity = 0.3f;
  public float explosiveness = 1f;
  public float flammability = 1f;
  public float temperature = 0.4f;
  
  private Gas(String name, Color color, Item cpItem, Liquid cpLiquid, float compressRequire){
    super(name);
    compressItem = cpItem;
    compressLiquid = cpLiquid;
    compressible = cpItem != null || cpLiquid != null;
    this.color = color;
    this.compressRequire = compressRequire;
  }
  
  public Gas(String name, Color color, Item cpItem, float compressRequire){
    this(name, color, cpItem, null, compressRequire);
  }
  
  public Gas(String name, Color color, Liquid cpLiquid, float compressRequire){
    this(name, color, null, cpLiquid, compressRequire);
  }
  
  public Gas(String name, Color color){
    this(name, color, null, null, 0);
  }
  
  public Item creatTank(float consume){
    tank = new Item(name + "_tank", color);
    hasTank = true;
    tankContains = consume;
    return tank;
  }
  
  public boolean compItem(){
    return compressible && compressItem != null;
  }
  
  public boolean compLiquid(){
    return compressible && compressLiquid != null;
  }
  
  @Override
  public void setStats(){
    super.setStats();
    stats.add(SglStat.compressible, Core.bundle.get("gas.compressible." + compressible));
    if(compressible) stats.add(SglStat.compressor, t -> {
      t.add(Core.bundle.get("stat.compressor") + ":");
      t.row();
      if(compressItem != null){
        t.add(new ItemDisplay(compressItem, 0));
      }
      else{
        t.add(new LiquidDisplay(compressLiquid, 0, false));
      }
      t.row();
      t.add(Core.bundle.get("stat.compressRequire") + ":");
      t.row();
      t.add(new GasValue(this, compressRequire, true, false));
    });
    stats.useCategories = true;
  }
  
  @Override
  public void createIcons(MultiPacker packer){
    Pixmap base = Core.atlas.getPixmap(Singularity.getModAtlas("gas_base")).crop();
    Pixmap gas = new Pixmap(base.getWidth(), base.getHeight());
    gas.fill(color.cpy().a(0.6f));
    gas.draw(base, 0, 0, true);
    packer.add(PageType.main, name, gas);
    packer.add(PageType.ui, name, gas);
  }
  
  @Override
  public ContentType getContentType(){
    return SglContentType.gas.value;
  }
}
