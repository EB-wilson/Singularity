package singularity.type;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.graphics.MultiPacker;
import mindustry.graphics.MultiPacker.PageType;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.meta.Stat;
import singularity.Sgl;
import singularity.Singularity;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatValues;

public class Gas extends UnlockableContent{
  public final Color color;
  
  protected boolean compressible;
  protected CompressItem compressItem;
  protected CompressLiquid compressLiquid;
  
  public Item tank;
  public boolean hasTank = false;
  public float tankContains;
  
  public float heatCapacity = 0.3f;
  public float explosiveness = 1f;
  public float flammability = 1f;
  public float temperature = 0.4f;
  
  public Gas(String name, Color color){
    super(name);
    this.color = color;
  }
  
  public Item creatTank(float consume){
    tank = new Item(name.replace(Sgl.modName + "-", "") + "_tank", color);
    hasTank = true;
    tankContains = consume;
    return tank;
  }
  
  public CompressItem creatItem(float requireComp, float consume){
    compressible = true;
    return compressItem = new CompressItem(name + "_item", color, requireComp, consume);
  }
  
  public CompressItem setCompressItem(Item item, float requireComp, float consume){
    compressible = true;
    return compressItem = new CompressItem(item, requireComp, consume);
  }
  
  public CompressLiquid creatLiquid(float requireComp, float consume){
    compressible = true;
    return compressLiquid = new CompressLiquid(name + "_liquid", color, requireComp, consume);
  }
  
  public CompressLiquid setCompressLiquid(Liquid liquid, float requireComp, float consume){
    compressible = true;
    return compressLiquid = new CompressLiquid(liquid, requireComp, consume);
  }
  
  public CompressItem getCompressItem(){
    return compressItem;
  }
  
  public CompressLiquid getCompressLiquid(){
    return compressLiquid;
  }
  
  public boolean compressible(){
    return compressible;
  }
  
  public boolean compItem(){
    return compressItem != null;
  }
  
  public boolean compLiquid(){
    return compressLiquid != null;
  }
  
  public boolean multiComp(){
    return compItem() && compLiquid();
  }
  
  @Override
  public void init(){
    super.init();
    if(compressLiquid != null && compressItem != null){
      compressItem.liquid = compressLiquid.liquid;
      compressItem.consumeLiquid = compressItem.consumeGas/compressLiquid.consumeGas;
    }
  }
  
  @Override
  public void setStats(){
    super.setStats();
    stats.addPercent(Stat.explosiveness, explosiveness);
    stats.addPercent(Stat.flammability, flammability);
    stats.addPercent(Stat.temperature, temperature);
    stats.addPercent(Stat.heatCapacity, heatCapacity);
    
    stats.add(SglStat.compressible, Core.bundle.get("gas.compressible." + compressible));
  
    if(compItem() && compLiquid()){
      stats.add(SglStat.compressor, table -> {
        table.defaults().left();
        table.table(t -> {
          t.add(Core.bundle.get("misc.compFirst") + ":").color(Pal.accent);
          t.row();
          SglStatValues.gasCompValue(compressLiquid).display(t);
  
          t.row();
          t.add(Core.bundle.get("misc.compSecond") + ":").color(Pal.accent);
          t.row();
          SglStatValues.gasCompValue(compressItem).display(t);
        });
      });
    }
    else if(compLiquid()){
      stats.add(SglStat.compressor, SglStatValues.gasCompValue(compressLiquid));
    }
    else if(compItem()){
      stats.add(SglStat.compressor, SglStatValues.gasCompValue(compressItem));
    }
    
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
    return SglContents.gas;
  }
  
  public static class CompressLiquid{
    public Liquid liquid;
    
    public final float requirePressure;
    public final float consumeGas;
  
    public CompressLiquid(String name, Color color, float require, float consume){
      this(new Liquid(name, color), require, consume);
    }
    
    public CompressLiquid(Liquid liquid, float require, float consume){
      this.liquid = liquid;
      requirePressure = require;
      consumeGas = consume;
    }
  }
  
  public static class CompressItem{
    public Item item;
    
    public float compTime = 60;
    public final float requirePressure;
    public final float consumeGas;
    
    public Liquid liquid;
    public float consumeLiquid;
  
    public CompressItem(String name, Color color, float require, float consume){
      this(new Item(name, color), require, consume);
    }
    
    public CompressItem(Item item, float require, float consume){
      this.item = item;
      requirePressure = require;
      consumeGas = consume;
    }
  }
}
