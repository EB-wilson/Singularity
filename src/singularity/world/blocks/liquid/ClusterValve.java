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
        case 0 -> e.input[e.currConfig] = !e.input[e.currConfig];
        case 1 -> e.output[e.currConfig] = !e.output[e.currConfig];
        case 2 -> e.blocking[e.currConfig] = !e.blocking[e.currConfig];
      }
    });

    config(IntSeq.class, (ClusterValveBuild e, IntSeq c) -> {
      if (c.get(0) == 0){
        e.configured[c.get(1)] = Vars.content.liquid(c.get(2));
      }
      else if(c.get(0) == 1){
        e.liquidsBuffer = new ClusterLiquidModule[c.get(1)];
        for (int ind = 0; ind < e.liquidsBuffer.length; ind++) {
          e.liquidsBuffer[ind] = new ClusterLiquidModule();
        }
        e.configured = new Liquid[c.get(1)];
        e.input = new boolean[c.get(1)];
        e.output = new boolean[c.get(1)];
        e.blocking = new boolean[c.get(1)];
        for (int l = 0; l < e.liquidsBuffer.length; l++) {
          e.configured[l] = Vars.content.liquid(c.get(l*4 + 2));
          e.input[l] = c.get(l*4 + 3) == 1;
          e.output[l] = c.get(l*4 + 4) == 1;
          e.blocking[l] = c.get(l*4 + 5) == 1;
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
    public boolean[] input;
    public boolean[] output;
    public boolean[] blocking;

    int currConfig;

    @Override
    public void onReplaced(ReplaceBuildComp old) {
      liquidsBuffer = old.<ClusterConduitBuild>getBuild().liquidsBuffer;
      configured = new Liquid[liquidsBuffer.length];
      input = new boolean[liquidsBuffer.length];
      output = new boolean[liquidsBuffer.length];
      blocking = new boolean[liquidsBuffer.length];
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
        t.defaults().left().top();
        t.check(Core.bundle.get("infos.inputMode"), b -> configure(new byte[]{0})).size(180, 45)
            .update(c -> c.setChecked(input[currConfig])).get().left();
        t.row();
        t.check(Core.bundle.get("infos.outputMode"), b -> configure(new byte[]{1})).size(180, 45)
            .update(c -> c.setChecked(output[currConfig])).get().left();
        t.row();
        t.check(Core.bundle.get("infos.blocking"), b -> configure(new byte[]{2})).size(180, 45)
            .update(c -> c.setChecked(blocking[currConfig]))
            .disabled(c -> input[currConfig] || !output[currConfig]).get().left();
      }).top().fill().padLeft(0);
    }

    @Override
    public Object config() {
      IntSeq req = IntSeq.with(1);

      req.add(liquidsBuffer.length);
      for (int i = 0; i < configured.length; i++) {
        req.add(configured[i] == null? -1: configured[i].id);
        req.add(input[i]? 1: 0);
        req.add(output[i]? 1: 0);
        req.add(blocking[i]? 1: 0);
      }
      return req;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public float moveLiquidForward(boolean leaks, Liquid liquid){
      Tile next = tile.nearby(rotation);
      int index = configuredIndex(liquid);

      float flow = 0;
      for(int i = 0; i < liquidsBuffer.length; i++){
        LiquidModule liquids = liquidsBuffer[i];

        if(output[i] && i == index){
          Building other = getNext(groupName(i), e -> {
            if (!e.interactable(team)) return false;
            byte rot = relativeTo(e);
            if(rot == (rotation + 1)%4 || rot == (rotation + 3)%4){
              if(e instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
                return mu.conduitAccept(this, index, liquidsBuffer[index].current());
              }
              else return e.block.hasLiquids && e.acceptLiquid(this, liquidsBuffer[index].current());
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

          if (blocking[index] && !input[index]) continue;
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
      int index = configuredIndex(liquid);
      if(index == -1 || !input[index]) return false;
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
      write.i(configured.length);
      for (int i = 0; i < input.length; i++) {
        write.i(configured[i] == null? -1: configured[i].id);
        write.bool(input[i]);
        write.bool(output[i]);
        write.bool(blocking[i]);
      }
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      int len = read.i();
      configured = new Liquid[len];
      input = new boolean[len];
      output = new boolean[len];
      blocking = new boolean[len];
      for (int i = 0; i < len; i++) {
        configured[i] = Vars.content.liquid(read.i());
        input[i] = read.bool();
        output[i] = read.bool();
        blocking[i] = read.bool();
      }
    }
  }
}
