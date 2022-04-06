package singularity.world.blocks.liquid;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.util.Eachable;
import mindustry.entities.Puddles;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import mindustry.ui.Styles;
import mindustry.world.Tile;
import mindustry.world.modules.LiquidModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.Takeable;

public class ConduitRiveting extends ClusterConduit{
  public ConduitRiveting(String name){
    super(name);
    configurable = true;
    outputsLiquid = true;
    
    config(Integer[].class, (ConduitRivetingBuild e, Integer[] i) -> {
      e.index = e.index == i[0]? -1: i[0];
      e.output = i[1] > 0;
    });
    config(Integer.class, (ConduitRivetingBuild e, Integer i) -> {
      e.index = e.index == i? -1: i;
    });
    config(Boolean.class, (ConduitRivetingBuild e, Boolean i) -> {
      e.output = i;
    });
    
    configClear((ConduitRivetingBuild e) -> {
      e.index = -1;
      e.output = false;
    });
  }
  
  @Override
  public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
    Draw.rect(region, req.drawx(), req.drawy());
    Draw.rect(arrow, req.drawx(), req.drawy(), req.rotation * 90);
  }
  
  @Override
  public TextureRegion[] icons(){
    return new TextureRegion[]{
        region,
        arrow
    };
  }
  
  @Annotations.ImplEntries
  public class ConduitRivetingBuild extends ClusterConduitBuild implements Takeable{
    public int index = -1;
    public boolean output;
  
    @Override
    public void draw(){
      Draw.rect(region, x, y);
      Draw.rect(arrow, x, y, rotation*90);
    }
  
    @Override
    public void buildConfiguration(Table table){
      table.table(Styles.black6, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image().size(35)).size(40);
        t.table(b -> {
          b.check("", output, this::configure).left();
          b.table(text -> {
            text.defaults().grow().left();
            text.add(Core.bundle.get("misc.currentMode")).color(Pal.accent);
            text.row();
            text.add("").update(l -> {
              l.setText(output ? Core.bundle.get("infos.outputMode"): Core.bundle.get("infos.inputMode"));
            });
          }).grow().right().padLeft(8);
        }).size(194, 40).padLeft(8);
      }).size(250, 40);
      table.row();
      
      table.table(Styles.black6, conduits -> {
        for(int i=0; i<conduitAmount; i++){
          int index = i;
          conduits.button(t -> t.add(Core.bundle.get("misc.conduit") + "#" + index).left().grow(), Styles.underlineb, () -> configure(index))
              .update(b -> b.setChecked(index == this.index)).size(250, 35).pad(0);
          conduits.row();
        }
      });
    }
  
    @Override
    public float moveLiquidForward(boolean leaks, Liquid liquid){
      if(!output){
        return super.moveLiquidForward(leaks, liquid);
      }
      else{
        Tile next = tile.nearby(rotation);
        if(next == null) return 0;
  
        float flow = 0;
        for(int i = 0; i < liquidsBuffer.length; i++){
          LiquidModule liquids = liquidsBuffer[i];
          
          if(i == index){
            int i1 = i;
            Building other = getNext("liquids", e -> {
              if(nearby(Mathf.mod(rotation - 1, 4)) == e || nearby(Mathf.mod(rotation + 1, 4)) == e){
                if(e instanceof MultLiquidBuild){
                  return ((MultLiquidBuild) e).conduitAccept(this, i1, liquidsBuffer[i1].current());
                }
                else return e.block.hasLiquids && e.acceptLiquid(this, liquidsBuffer[i1].current());
              }
              return false;
            });
            
            if(other == null){
              if(leaks && !next.block().solid && ! next.block().hasLiquids){
                float leakAmount = liquids.currentAmount()/1.5f;
                Puddles.deposit(next, tile, liquids.current(), leakAmount);
                liquids.remove(liquids.current(), leakAmount);
              }
            }
            else if(other instanceof MultLiquidBuild){
              flow += moveLiquid((MultLiquidBuild)other, index, liquidsBuffer[index].current());
            }
            else{
              this.liquids = liquidsBuffer[index];
              flow += moveLiquid(other, liquidsBuffer[index].current());
              this.liquids = cacheLiquids;
            }
          }
          else if(next.build instanceof MultLiquidBuild){
            flow += moveLiquid((MultLiquidBuild) next.build, i, liquids.current());
          }
          else if(next.build != null){
            this.liquids = liquids;
            flow += moveLiquid(next.build, liquids.current());
            this.liquids = cacheLiquids;
          }
          else if(leaks && !next.block().solid && ! next.block().hasLiquids){
            float leakAmount = liquids.currentAmount()/1.5f;
            Puddles.deposit(next, tile, liquids.current(), leakAmount);
            liquids.remove(liquids.current(), leakAmount);
          }
        }
  
        return flow;
      }
    }
  
    @Override
    public Object config(){
      return new Integer[]{index, output? 1: -1};
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      noSleep();
      if(output) return false;
      if(index == -1){
        return super.acceptLiquid(source, liquid);
      }
      return source.interactable(team) && liquidsBuffer[index].currentAmount() < 0.01f || liquid == liquidsBuffer[index].current() && liquidsBuffer[index].currentAmount() < liquidCapacity;
    }
  
    @Override
    public void handleLiquid(Building source, Liquid liquid, float amount){
      if(index == -1){
        super.handleLiquid(source, liquid, amount);
      }
      else liquidsBuffer[index].add(liquid, amount);
    }
  }
}
