package singularity.world.blocks.liquid;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.util.Eachable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.Puddles;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
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
      for (int ind = 0; ind < e.liquidsBuffer.length; ind++) {
        e.liquidsBuffer[ind] = new ClusterLiquidModule();
      }
      e.input = new boolean[i.get(0)];
      e.output = new boolean[i.get(0)];
      e.blocking = new boolean[i.get(0)];
      for (int l = 0; l < e.liquidsBuffer.length; l++) {
        e.input[l] = i.get(l*3 + 1) == 1;
        e.output[l] = i.get(l*3 + 2) == 1;
        e.blocking[l] = i.get(l*3 + 3) == 1;
      }
    });
    config(Integer.class, (ConduitRivetingBuild e, Integer i) -> {
      e.currConf = i;
    });
    config(byte[].class, (ConduitRivetingBuild e, byte[] b) -> {
      switch (b[0]){
        case 0 -> e.input[e.currConf] = !e.input[e.currConf];
        case 1 -> e.output[e.currConf] = !e.output[e.currConf];
        case 2 -> e.blocking[e.currConf] = !e.blocking[e.currConf];
      }
    });
    
    configClear((ConduitRivetingBuild e) -> {
      e.currConf = 0;
      e.input = new boolean[e.liquidsBuffer.length];
      e.output = new boolean[e.liquidsBuffer.length];
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
    public boolean[] input;
    public boolean[] output;
    public boolean[] blocking;
    public int currConf;

    @Override
    public void draw(){
      Draw.rect(region, x, y);
      Draw.rect(arrow, x, y, rotation*90);
    }

    @Override
    public void onReplaced(ReplaceBuildComp old) {
      liquidsBuffer = old.<ClusterConduitBuild>getBuild().liquidsBuffer;
      input = new boolean[liquidsBuffer.length];
      output = new boolean[liquidsBuffer.length];
      blocking = new boolean[liquidsBuffer.length];
    }

    @Override
    public void buildConfiguration(Table table){
      table.table(Styles.black6, t -> {
        for(int i=0; i<liquidsBuffer.length; i++){
          int index = i;
          t.button(ta -> ta.add(Core.bundle.get("misc.conduit") + "#" + index).left().grow(), Styles.underlineb, () -> configure(index))
              .update(b -> b.setChecked(index == currConf)).size(250, 35).pad(0);
          t.row();
        }
      }).fill();
      table.table(Styles.black6, t -> {
        t.defaults().left();
        t.check(Core.bundle.get("infos.inputMode"), b -> configure(new byte[]{0})).size(180, 45)
            .update(c -> c.setChecked(input[currConf])).get().left();
        t.row();
        t.check(Core.bundle.get("infos.outputMode"), b -> configure(new byte[]{1})).size(180, 45)
            .update(c -> c.setChecked(output[currConf])).get().left();
        t.row();
        t.check(Core.bundle.get("infos.blocking"), b -> configure(new byte[]{2})).size(180, 45)
            .update(c -> c.setChecked(blocking[currConf]))
            .disabled(c -> input[currConf] || !output[currConf]).get().left();
      }).top().fill().padLeft(0);
    }
  
    @SuppressWarnings("DuplicatedCode")
    @Override
    public float moveLiquidForward(boolean leaks, Liquid liquid){
      Tile next = tile.nearby(rotation);

      float flow = 0;
      for(int i = 0; i < liquidsBuffer.length; i++){
        ClusterLiquidModule liquids = liquidsBuffer[i];

        if(output[i]){
          int i1 = i;
          Building other = getNext("liquids#" + i, e -> {
            if (!e.interactable(team)) return false;
            int rot = relativeTo(e);
            if(rot == (rotation + 1)%4 || rot == (rotation + 3)%4){
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

          if (blocking[i] && !input[i]) continue;
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

      for (int i = 0; i < input.length; i++) {
        req.add(input[i]? 1: 0);
        req.add(output[i]? 1: 0);
        req.add(blocking[i]? 1: 0);
      }
      return req;
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      noSleep();

      if (!source.interactable(team)) return false;

      for (int i = 0; i < input.length; i++) {
        if (input[i]){
          ClusterLiquidModule liq = liquidsBuffer[i];
          if(liq.currentAmount() < 0.01f || liquid == liq.current() && liq.currentAmount() < liquidCapacity) return true;
        }
      }

      return false;
    }

    @Override
    public boolean conduitAccept(MultLiquidBuild source, int index, Liquid liquid) {
      noSleep();
      if (source.tile.absoluteRelativeTo(tile.x, tile.y) != rotation && (input[index])) return false;
      LiquidModule liquids = liquidsBuffer[index];
      return source.interactable(team) && liquids.currentAmount() < 0.01f || liquids.current() == liquid && liquids.currentAmount() < liquidCapacity;
    }

    @Override
    public LiquidModule getModuleAccept(Building source, Liquid liquid) {
      for (int i = 0; i < liquidsBuffer.length; i++) {
        if (!input[i]) continue;

        LiquidModule liquids = liquidsBuffer[i];
        if(liquids.current() == liquid && liquids.currentAmount() < liquidCapacity) return liquids;
      }
      return null;
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.i(input.length);
      for (int i = 0; i < input.length; i++) {
        write.bool(input[i]);
        write.bool(output[i]);
        write.bool(blocking[i]);
      }
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      int len = read.i();
      input = new boolean[len];
      output = new boolean[len];
      blocking = new boolean[len];
      for (int i = 0; i < len; i++) {
        input[i] = read.bool();
        output[i] = read.bool();
        blocking[i] = read.bool();
      }
    }
  }
}
