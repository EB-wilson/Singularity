package singularity.world.blocks.liquid;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.util.Eachable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Puddles;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
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

    config(Integer.class, (ClusterValveBuild e, Integer i) -> {
      e.currConfig = i;
    });
    config(byte[].class, (ClusterValveBuild e, byte[] i) -> {
      switch (i[0]){
        case 0 -> e.input = !e.input;
        case 1 -> e.output = !e.output;
        case 2 -> e.blocking = !e.blocking;
      }
    });

    config(IntSeq.class, (ClusterValveBuild e, IntSeq c) -> {
      if (c.get(0) == 0){
        e.configured[c.get(1)] = Vars.content.liquid(c.get(2));
      }
      else if(c.get(0) == 1){
        e.liquidsBuffer = new ClusterLiquidModule[c.get(4)];
        for (int ind = 0; ind < e.liquidsBuffer.length; ind++) {
          e.liquidsBuffer[ind] = new ClusterLiquidModule();
        }

        e.input = c.get(1) == 1;
        e.output = c.get(2) == 1;
        e.blocking = c.get(3) == 1;

        e.configured = new Liquid[c.get(4)];
        for (int l = 0; l < e.liquidsBuffer.length; l++) {
          e.configured[l] = Vars.content.liquid(c.get(l + 5));
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
    private static final IntSeq tmp = new IntSeq();

    public Liquid[] configured;
    public boolean input;
    public boolean output;
    public boolean blocking;

    int currConfig;

    @Override
    public void onReplaced(ReplaceBuildComp old) {
      liquidsBuffer = old.<ClusterConduitBuild>getBuild().liquidsBuffer;
      configured = new Liquid[liquidsBuffer.length];
    }

    @Override
    public void buildConfiguration(Table table){
      table.table(ta -> {
        ta.table(Styles.black6, conduits -> {
          for(int i=0; i<liquidsBuffer.length; i++){
            int index = i;
            conduits.button(t -> t.add(Core.bundle.get("misc.conduit") + "#" + index).left().grow(), Styles.underlineb,
                () -> configure(index)).update(b -> b.setChecked(index == currConfig)).size(250, 35).pad(0);
            conduits.row();
          }
        });
      }).top();

      table.table(tb -> {
        tb.clearChildren();
        tb.defaults().left().fill();
        ItemSelection.buildTable(
            tb,
            Vars.content.liquids(),
            () -> configured[currConfig],
            li -> configure(IntSeq.with(0, currConfig, li == null? -1: li.id)),
            false
        );
      }).padLeft(0).top();

      table.table(Styles.black6, t -> {
        t.defaults().left().top().height(45).minWidth(170).padRight(12).left().growX();
        t.check(Core.bundle.get("infos.inputMode"), b -> configure(new byte[]{0}))
            .update(c -> c.setChecked(input)).get().left();
        t.row();
        t.check(Core.bundle.get("infos.outputMode"), b -> configure(new byte[]{1}))
            .update(c -> c.setChecked(output)).get().left();
        t.row();
        t.check(Core.bundle.get("infos.blocking"), b -> configure(new byte[]{2}))
            .update(c -> c.setChecked(blocking))
            .disabled(c -> input || !output).get().left();
      }).top().fill().padLeft(0);
    }

    @Override
    public Object config() {
      IntSeq req = IntSeq.with(1);

      req.add(input? 1: 0);
      req.add(output? 1: 0);
      req.add(blocking? 1: 0);

      req.add(liquidsBuffer.length);
      for(Liquid liquid: configured){
        req.add(liquid == null? -1: liquid.id);
      }
      return req;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public float moveLiquidForward(boolean leaks, Liquid liquid){
      Tile next = tile.nearby(rotation);

      float flow = 0;
      for(int i = 0; i < liquidsBuffer.length; i++){
        LiquidModule liquids = liquidsBuffer[i];
        if(liquid != null && liquids.current() != liquid) continue;

        Liquid li = liquids.current();
        IntSeq index = configuredIndex(li);
        if(output && !index.isEmpty()){
          for(int l = 0; l < index.size; l++){
            int in = index.get(l);
            Building other = getNext(groupName(i), e -> {
              if(!e.interactable(team)) return false;
              byte rot = relativeTo(e);
              if(rot == (rotation + 1)%4 || rot == (rotation + 3)%4){
                if(e instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
                  return mu.conduitAccept(this, in, liquids.current());
                }else{
                  return e.block.hasLiquids && e.acceptLiquid(this, liquids.current());
                }
              }
              return false;
            });

            if(other instanceof MultLiquidBuild mu){
              if(!mu.conduitAccept(this, in, li)) continue;

              flow += moveLiquid(mu, i, in, li);
            }else if(other != null){
              this.liquids = liquids;
              flow += moveLiquid(other, liquids.current());
              this.liquids = cacheLiquids;
            }
          }

          if(blocking && !input) continue;
        }

        if(next.build instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
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

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      noSleep();
      IntSeq index = configuredIndex(liquid);
      for(int i = 0; i < index.size; i++){
        int in = index.get(i);
        if(input && (source.interactable(team) && liquidsBuffer[in].currentAmount() < 0.01f
        || liquid == liquidsBuffer[in].current() && liquidsBuffer[in].currentAmount() < liquidCapacity)) return true;
      }

      return false;
    }

    @Override
    public boolean shouldClusterMove(MultLiquidBuild source) {
      return super.shouldClusterMove(source) && source.tile.absoluteRelativeTo(tile.x, tile.y) == rotation;
    }

    @Override
    public void handleLiquid(Building source, Liquid liquid, float amount) {
      IntSeq index = configuredIndex(liquid);
      if(index.isEmpty()){
        super.handleLiquid(source, liquid, amount);
      }
      else{
        float am = amount/index.size;
        for(int i = 0; i < index.size; i++){
          liquidsBuffer[index.get(i)].add(liquid, am);
        }
      }
    }

    public IntSeq configuredIndex(Liquid liquid){
      tmp.clear();
      for (int i = 0; i < configured.length; i++) {
        if (configured[i] == liquid) tmp.add(i);
      }
      return tmp;
    }

    @Override
    public void draw(){
      Draw.rect(region, x, y);
      Draw.rect(arrow, x, y, rotation*90);
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.bool(input);
      write.bool(output);
      write.bool(blocking);

      write.i(configured.length);
      for(Liquid liquid: configured){
        write.i(liquid == null? -1: liquid.id);
      }
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      input = read.bool();
      output = read.bool();
      blocking = read.bool();

      configured = new Liquid[read.i()];
      for (int i = 0; i < configured.length; i++) {
        configured[i] = Vars.content.liquid(read.i());
      }
    }
  }
}
