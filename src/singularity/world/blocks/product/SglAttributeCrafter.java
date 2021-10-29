package singularity.world.blocks.product;

import arc.Core;
import arc.func.Cons2;
import arc.func.Floatf;
import arc.math.Mathf;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.maps.Map;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import universeCore.util.Functions;

import static mindustry.Vars.indexer;
import static mindustry.Vars.state;

/**一w个由其下方地板具有的属性计算效率的工厂*/
public class SglAttributeCrafter extends NormalCrafter{
  protected ObjectMap<Attribute, Floatf<Float>> attributeBoosters = new ObjectMap<>();
  protected ObjectMap<Attribute, Cons2<Table, Seq<? extends Block>>> boostDisplay = new ObjectMap<>();
  /**基础效率级别，地板效率会基于此效率增加或减少*/
  public float basicEfficiency = 1f;
  /**最大加速效率，效率最高不会超过此值*/
  public float maxBoost = 4f;
  public boolean checkFloors = true;
  
  Table boost = new Table();
  Map lastMap;
  
  public SglAttributeCrafter(String name){
    super(name);
  }
  
  /**设置此工厂可用的属性地板效率增量，多种属性重复时会将效率累加
  * @param attrs 格式：[Attribute], [increase], .... 设置某个属性的效率倍率缩放值*/
  @SuppressWarnings("unchecked")
  public void setAttrBooster(Object... attrs){
    for(int i=0; i<attrs.length;){
      Attribute attr = (Attribute)attrs[i];
      if(attrs.length > i + 1 && attrs[i+1] instanceof Number){
        setAttrBooster(attr, ((Number)attrs[i+1]).floatValue());
        i += 2;
      }
      else if(attrs.length > i + 2 && attrs[i+1] instanceof Floatf && attrs[i+2] instanceof Cons2){
        setAttrBooster(attr, (Floatf<Float>)attrs[i+1], (Cons2<Table, Seq<? extends Block>>)attrs[i+2]);
        i += 3;
      }
    }
  }
  
  public void setAttrBooster(Attribute attr, float maxBoost, float optimal, float diffLeft, float diffRight){
    setAttrBooster(attr, f -> (float)Functions.lerpIncrease(diffLeft, diffRight, maxBoost, optimal, f), (t, bs) -> {
      t.add(Core.bundle.get("misc.max") + ": " + (int)(maxBoost*100) + "% " + Core.bundle.get("misc.optimal") + ": " + optimal);
      for(Block b: bs){
        float attrValue = b.attributes.get(attr);
        t.stack(
            new Image(b.uiIcon).setScaling(Scaling.fit),
            new Table(table -> {
              table.add((attrValue < 0 ? "[accent]": "[accent]+") + attrValue);
              table.top().left().add("/" + StatUnit.blocks.localized()).color(Pal.gray);
            })
        ).padLeft(5);
      }
    });
  }
  
  public void setAttrBooster(Attribute attr, float maxBoost, float optimal, float diffRate){
    setAttrBooster(attr, maxBoost, optimal, diffRate, diffRate);
  }
  
  public void setAttrBooster(Attribute attr, float maxBoost, float optimal){
    setAttrBooster(attr, maxBoost, optimal, 0.2f);
  }
  
  public void setAttrBooster(Attribute attr, float scl){
    attributeBoosters.put(attr, f -> f*scl);
    boostDisplay.put(attr, (t, bs) -> {
      for(Block b: bs){
        float multiplier = attributeBoosters.get(attr).get(b.attributes.get(attr));
        t.stack(
            new Image(b.uiIcon).setScaling(Scaling.fit),
            new Table(table -> {
              table.top().right().add((multiplier < 0 ? "[scarlet]" : basicEfficiency == 0 ? "[accent]" : "[accent]+") + (int)(multiplier*100) + "%").style(Styles.outlineLabel);
              table.top().left().add("/" + StatUnit.blocks.localized()).color(Pal.gray);
            })
        );
      }
    });
  }
  
  public void setAttrBooster(Attribute attr, Floatf<Float> scl, Cons2<Table, Seq<? extends Block>> display){
    attributeBoosters.put(attr, scl);
    boostDisplay.put(attr, display);
  }
  
  public void removeBooster(Attribute attr){
    attributeBoosters.remove(attr);
    boostDisplay.remove(attr);
  }
  
  @Override
  public void setBars(){
    super.setBars();
    bars.add("efficiency", (SglAttributeCrafterBuild e) -> new Bar(
        () -> Core.bundle.get("misc.efficiency") + ": " + Strings.autoFixed(e.efficiency()*100, 0) + "%",
        () -> Pal.accent,
        () -> Mathf.clamp(e.efficiency())
    ));
  }
  
  @Override
  public void setStats(){
    super.setStats();
    //因为地板类型可能种类繁多，所以这里采用可展开和关闭的布局方式以保证显示简洁
    Runnable build = () -> {
      boost.clearChildren();
      boost.defaults().left();
      if(state.isGame()){
        for(Attribute attr: Attribute.all){
          if(attributeBoosters.get(attr) == null) continue;
          boost.add(attr.name + ":");
          Seq<Floor> blocks = Vars.content.blocks()
              .select(block -> (! checkFloors || block instanceof Floor) && indexer.isBlockPresent(block) && block.attributes.get(attr) != 0 && ! ((block instanceof Floor && ((Floor) block).isDeep()) && ! floating))
              .<Floor>as().with(s -> s.sort(f -> f.attributes.get(attr)));
        
          if(blocks.any()){
            boostDisplay.get(attr).get(boost.table().get(), blocks);
          }else boost.add(Core.bundle.get("none.inmap"));
          boost.row();
        }
      }else boost.add(Core.bundle.get("stat.showinmap"));
    };
    build.run();
  
    boost.update(() -> {
      Map current = state.isGame() ? state.map : null;
      
      if(current != lastMap){
        build.run();
        lastMap = current;
      }
    });
    
    stats.add(Stat.affinities, table -> {
      table.row();
      table.table(Tex.pane, t -> t.add(boost).grow());
    });
  }
  
  @Override
  public boolean canPlaceOn(Tile tile, Team team){
    return efficiencyIncrease(tile.x, tile.y) > 0;
  }
  
  public float efficiencyIncrease(int x, int y){
    float result = 0f;
    for(Attribute attr: Attribute.all){
      if(!attributeBoosters.containsKey(attr)) continue;
      result += attributeBoosters.get(attr).get(sumAttribute(attr, x, y) + attr.env());
    }
    return Math.min(maxBoost, result + basicEfficiency);
  }
  
  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    drawPlaceText(Core.bundle.format("bar.efficiency",
    (int)(efficiencyIncrease(x, y) * 100f)), x, y, valid);
  }

  public class SglAttributeCrafterBuild extends NormalCrafterBuild{
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
