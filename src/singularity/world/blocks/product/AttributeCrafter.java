package singularity.world.blocks.product;

import singularity.world.meta.SglStat;
import arc.Core;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.StatValues;

/**一个由其下方地板具有的属性计算效率的工厂*/
public class AttributeCrafter extends NormalCrafter{
  protected float[] attributeBoosters = new float[Attribute.all.length];
  /**基础效率级别，地板效率会基于此效率增加或减少*/
  public float basicEfficiency = 1f;
  /**最大加速效率，效率最高不会超过此值*/
  public float maxBoost = 4f;
  
  public AttributeCrafter(String name){
    super(name);
  }
  
  /**设置此工厂可用的属性地板效率增量，多种属性重复时会将效率累加
  * @param attrs 格式：[Attribute], [increase], .... 设置某个属性的效率倍率缩放值*/
  public void setAttributeBooster(Object... attrs){
    for(int i=0; i<attrs.length; i+=2){
      Attribute attr = (Attribute)attrs[i];
      attributeBoosters[attr.id] = ((Number)attrs[i+1]).floatValue();
    }
  }
  
  @Override
  public void setStats(){
    super.setStats();
    //因为地板类型可能种类繁多，所以这里采用可展开和关闭的布局方式以保证显示简洁
    Table boost = new Table();
    boost.defaults().left();
    for(Attribute attr: Attribute.all){
      if(attributeBoosters[attr.id] == 0) continue;
      boost.add(attr.name + ":");
      for(Floor block : Vars.content.blocks()
        .select(block -> block instanceof Floor && ((Floor)block).attributes.get(attr) != 0 && !((Floor)block).isLiquid)
        .<Floor>as().with(s -> s.sort(f -> f.attributes.get(attr)))){
        StatValues.floorEfficiency(block, block.attributes.get(attr)*attributeBoosters[attr.id], false).display(boost);
      }
      boost.row();
    }
    
    Table displayBoost = new Table();
    displayBoost.left();
    TextButton b = new TextButton(Core.bundle.get("unfold"));
    b.clicked(() -> {
      displayBoost.clear();
      displayBoost.add(boost);
      displayBoost.row();
      displayBoost.button(Core.bundle.get("misc.fold"), () -> {
        displayBoost.clear();
        displayBoost.add(b);
      });
    });
    
    displayBoost.add(b);
    stats.add(SglStat.floorBoosting, table -> {
      table.row();
      table.add(displayBoost);
    });
  }
  
  @Override
  public boolean canPlaceOn(Tile tile, Team team){
    return efficiencyIncrease(tile.x, tile.y) > 0;
  }
  
  public float efficiencyIncrease(int x, int y){
    float result = 0f;
    for(Attribute attr: Attribute.all){
      result += sumAttribute(attr, x, y)*attributeBoosters[attr.id];
    }
    return Math.min(maxBoost, result + basicEfficiency);
  }
  
  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    drawPlaceText(Core.bundle.format("bar.efficiency",
    (int)(efficiencyIncrease(x, y) * 100f)), x, y, valid);
  }

  public class AttributeCrafterBuild extends NormalCrafterBuild{
    float attributeEffect = 1;
    
    @Override
    public float efficiency(){
      return super.efficiency()*attributeEffect;
    }
    
    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();
      attributeEffect = efficiencyIncrease(tile.x, tile.y);
    }
  }
}
