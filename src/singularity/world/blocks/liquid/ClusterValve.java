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
import mindustry.Vars;
import mindustry.entities.Puddles;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import mindustry.ui.Styles;
import mindustry.world.Tile;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.modules.LiquidModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ReplaceBuildComp;
import universecore.components.blockcomp.Takeable;

public class ClusterValve extends ClusterConduit{
  public ClusterValve(String name) {
    super(name);
    conduitAmount = 0;
    configurable = true;
    outputsLiquid = true;

    config(Boolean.class, (ClusterValveBuild e, Boolean c) -> e.outputMode = c);

    config(IntSeq.class, (ClusterValveBuild e, IntSeq c) -> {
      if (c.get(0) == 0){
        e.configured[c.get(1)] = Vars.content.liquid(c.get(2));
      }
      else if(c.get(0) == 1){
        e.outputMode = c.get(1) == 1;
        int len = c.size - 2;
        e.liquidsBuffer = new ClusterLiquidModule[len];
        for (int i = 0; i < e.liquidsBuffer.length; i++) {
          e.liquidsBuffer[i] = new ClusterLiquidModule();
        }
        e.configured = new Liquid[len];
        for (int i = 2; i < c.size; i++) {
          e.configured[i - 2] = Vars.content.liquid(c.get(i));
        }
      }
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
  public class ClusterValveBuild extends ClusterConduitBuild implements Takeable {
    public Liquid[] configured;
    boolean outputMode;

    int currConfig;
    Runnable buildItemSelect;

    @Override
    public void onReplaced(ReplaceBuildComp old) {
      liquidsBuffer = old.<ClusterConduitBuild>getBuild().liquidsBuffer;
      configured = new Liquid[liquidsBuffer.length];
    }

    @Override
    public void buildConfiguration(Table table){
      table.table(ta -> {
        ta.table(Styles.black6, t -> {
          t.defaults().pad(0).margin(0);
          t.table(Tex.buttonTrans, i -> i.image().size(35)).size(40);
          t.table(b -> {
            b.check("", outputMode, this::configure).left();
            b.table(text -> {
              text.defaults().grow().left();
              text.add(Core.bundle.get("misc.currentMode")).color(Pal.accent);
              text.row();
              text.add("").update(l -> {
                l.setText(outputMode ? Core.bundle.get("infos.outputMode"): Core.bundle.get("infos.inputMode"));
              });
            }).grow().right().padLeft(8);
          }).size(194, 40).padLeft(8);
        }).size(250, 40);
        ta.row();

        ta.table(Styles.black6, conduits -> {
          for(int i=0; i<liquidsBuffer.length; i++){
            int index = i;
            conduits.button(t -> t.add(Core.bundle.get("misc.conduit") + "#" + index).left().grow(), Styles.underlineb, () -> {
              currConfig = index;
              buildItemSelect.run();
            }).update(b -> b.setChecked(index == currConfig)).size(250, 35).pad(0);
            conduits.row();
          }
        });
      });
      table.table(Tex.pane2, tb -> {
        buildItemSelect = () -> {
          tb.clearChildren();
          tb.defaults().left().fill();
          ItemSelection.buildTable(
              tb,
              Vars.content.liquids(),
              () -> configured[currConfig],
              li -> configure(IntSeq.with(0, currConfig, li == null? -1: li.id))
          );
        };
        buildItemSelect.run();
      }).padLeft(0);
    }

    @Override
    public Object config() {
      IntSeq res = IntSeq.with(1, outputMode? 1: 0);
      for(Liquid liquid : configured) {
        res.add(liquid == null ? -1 : liquid.id);
      }
      return res;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public float moveLiquidForward(boolean leaks, Liquid liquid){
      if(!outputMode){
        return super.moveLiquidForward(leaks, liquid);
      }
      else{
        Tile next = tile.nearby(rotation);
        int index = configuredIndex(liquid);

        float flow = 0;
        for(int i = 0; i < liquidsBuffer.length; i++){
          LiquidModule liquids = liquidsBuffer[i];

          if(i == index){
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
              flow += moveLiquid((MultLiquidBuild)other, index, liquidsBuffer[index].current());
            }
            else if (other != null){
              this.liquids = liquidsBuffer[index];
              flow += moveLiquid(other, liquidsBuffer[index].current());
              this.liquids = cacheLiquids;
            }
          }
          else if(next.build instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
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
    public boolean acceptLiquid(Building source, Liquid liquid){
      noSleep();
      if(outputMode) return false;
      int index = configuredIndex(liquid);
      if(index == -1) return false;
      return source.interactable(team) && liquidsBuffer[index].currentAmount() < 0.01f || liquid == liquidsBuffer[index].current() && liquidsBuffer[index].currentAmount() < liquidCapacity;
    }

    @Override
    public boolean shouldClusterMove(MultLiquidBuild source) {
      return super.shouldClusterMove(source) && source.tile.absoluteRelativeTo(tile.x, tile.y) == rotation;
    }

    @Override
    public void handleLiquid(Building source, Liquid liquid, float amount) {
      int index = configuredIndex(liquid);
      if(index == -1){
        super.handleLiquid(source, liquid, amount);
      }
      else liquidsBuffer[index].add(liquid, amount);
    }

    public int configuredIndex(Liquid liquid){
      for (int i = 0; i < configured.length; i++) {
        if (configured[i] == liquid) return i;
      }
      return -1;
    }

    @Override
    public void draw(){
      Draw.rect(region, x, y);
      Draw.rect(arrow, x, y, rotation*90);
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.bool(outputMode);
      write.i(configured.length);
      for (Liquid liquid : configured) {
        write.i(liquid == null? -1: liquid.id);
      }
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      outputMode = read.bool();
      int len = read.i();
      configured = new Liquid[len];
      for (int i = 0; i < len; i++) {
        configured[i] = Vars.content.liquid(read.i());
      }
    }
  }
}
