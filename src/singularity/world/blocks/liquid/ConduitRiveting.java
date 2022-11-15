package singularity.world.blocks.liquid;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.util.Eachable;
import arc.util.io.Reads;
import arc.util.io.Writes;
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
import universecore.components.blockcomp.ReplaceBuildComp;
import universecore.components.blockcomp.Takeable;

public class ConduitRiveting extends ClusterConduit{
  public ConduitRiveting(String name){
    super(name);
    conduitAmount = 0;
    configurable = true;
    outputsLiquid = true;
    
    config(IntSeq.class, (ConduitRivetingBuild e, IntSeq i) -> {
      e.liquidsBuffer = new ClusterLiquidModule[i.get(0)];
      e.condConfig = new int[i.get(0)];
      for (int l = 0; l < e.liquidsBuffer.length; l++) {
        e.liquidsBuffer[l] = new ClusterLiquidModule();
        e.condConfig[l] = i.get(l + 1);
      }
    });
    config(Integer.class, (ConduitRivetingBuild e, Integer i) -> {
      e.condConfig[e.currConf] = (e.condConfig[e.currConf] + 1)%4;
    });
    
    configClear((ConduitRivetingBuild e) -> {
      e.currConf = 0;
      e.condConfig = new int[e.liquidsBuffer.length];
    });
  }
  
  @Override
  public void drawPlanConfigTop(BuildPlan req, Eachable<BuildPlan> list){
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
    public int[] condConfig;
    public int currConf;

    @Override
    public void draw(){
      Draw.rect(region, x, y);
      Draw.rect(arrow, x, y, rotation*90);
    }

    @Override
    public void onReplaced(ReplaceBuildComp old) {
      liquidsBuffer = old.<ClusterConduitBuild>getBuild().liquidsBuffer;
      condConfig = new int[liquidsBuffer.length];
    }

    @Override
    public void buildConfiguration(Table table){
      table.table(Styles.black6, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image().size(35)).size(40);
        t.button(b -> {
          b.table(text -> {
            text.defaults().grow().left();
            text.add(Core.bundle.get("misc.currentMode")).color(Pal.accent);
            text.row();
            text.add("").update(l -> {
              l.setText(condConfig[currConf] == 0? Core.bundle.get("infos.disabled"):
                  condConfig[currConf] == 1? Core.bundle.get("infos.branchMode"):
                  condConfig[currConf] == 2? Core.bundle.get("infos.inputMode"): Core.bundle.get("infos.outputMode"));
            });
          }).grow().right().padLeft(8);
        }, Styles.cleart, () -> configure(currConf)).size(194, 40).padLeft(8);
      }).size(250, 40);
      table.row();
      
      table.table(Styles.black6, conduits -> {
        for(int i=0; i<liquidsBuffer.length; i++){
          int index = i;
          conduits.button(t -> t.add(Core.bundle.get("misc.conduit") + "#" + index).left().grow(), Styles.underlineb, () -> currConf = index)
              .update(b -> b.setChecked(index == currConf)).size(250, 35).pad(0);
          conduits.row();
        }
      });
    }
  
    @SuppressWarnings("DuplicatedCode")
    @Override
    public float moveLiquidForward(boolean leaks, Liquid liquid){
      Tile next = tile.nearby(rotation);

      float flow = 0;
      for(int i = 0; i < liquidsBuffer.length; i++){
        LiquidModule liquids = liquidsBuffer[i];

        if(condConfig[i] != 2 && condConfig[i] != 0){
          int i1 = i;
          Building other = getNext("liquids", e -> {
            if(nearby(Mathf.mod(rotation - 1, 4)) == e || nearby(Mathf.mod(rotation + 1, 4)) == e){
              if(e instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
                return mu.conduitAccept(this, i1, liquidsBuffer[i1].current());
              }
              else return e.block.hasLiquids && e.acceptLiquid(this, liquidsBuffer[i1].current());
            }
            return false;
          });

          if(other instanceof MultLiquidBuild){
            flow += moveLiquid((MultLiquidBuild)other, i, liquidsBuffer[i].current());
          }
          else if(other != null){
            this.liquids = liquids;
            flow += moveLiquid(other, liquidsBuffer[i].current());
            this.liquids = cacheLiquids;
          }

          if (condConfig[i] == 3) continue;
        }

        if(next.build instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
          flow += moveLiquid((MultLiquidBuild) next.build, i, liquids.current());
        }
        else if(next.build != null){
          this.liquids = liquids;
          flow += moveLiquid(next.build, liquids.current());
          this.liquids = cacheLiquids;
        }
        else if(leaks && !next.block().solid && !next.block().hasLiquids){
          float leakAmount = liquids.currentAmount()/1.5f;
          Puddles.deposit(next, tile, liquids.current(), leakAmount);
          liquids.remove(liquids.current(), leakAmount);
        }
      }

      return flow;
    }

    @Override
    public Object config() {
      IntSeq req = IntSeq.with(liquidsBuffer.length);

      for (int i : condConfig) {
        req.add(i);
      }
      return req;
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      noSleep();

      if (!source.interactable(team)) return false;

      for (int i = 0; i < condConfig.length; i++) {
        if (condConfig[i] != 3 && condConfig[i] != 0){
          ClusterLiquidModule liq = liquidsBuffer[i];
          if(liq.currentAmount() < 0.01f || liquid == liq.current() && liq.currentAmount() < liquidCapacity) return true;
        }
      }

      return false;
    }

    @Override
    public boolean conduitAccept(MultLiquidBuild source, int index, Liquid liquid) {
      noSleep();
      if (source.tile.absoluteRelativeTo(tile.x, tile.y) != rotation && (condConfig[index] == 3 || condConfig[index] == 0)) return false;
      LiquidModule liquids = liquidsBuffer[index];
      return source.interactable(team) && liquids.currentAmount() < 0.01f || liquids.current() == liquid && liquids.currentAmount() < liquidCapacity;
    }

    @Override
    public LiquidModule getModuleAccept(Building source, Liquid liquid) {
      for (int i = 0; i < liquidsBuffer.length; i++) {
        if (condConfig[i] == 3 || condConfig[i] == 0) continue;

        LiquidModule liquids = liquidsBuffer[i];
        if(liquids.current() == liquid && liquids.currentAmount() < liquidCapacity) return liquids;
      }
      return null;
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.i(condConfig.length);
      for (int i : condConfig) {
        write.i(i);
      }
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      int len = read.i();
      condConfig = new int[len];
      for (int i = 0; i < len; i++) {
        condConfig[i] = read.i();
      }
    }
  }
}
