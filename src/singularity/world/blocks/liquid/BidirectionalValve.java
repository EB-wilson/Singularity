package singularity.world.blocks.liquid;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.ui.Styles;
import mindustry.world.Tile;
import mindustry.world.modules.LiquidModule;
import universecore.components.blockcomp.ReplaceBuildComp;

public class BidirectionalValve extends ClusterConduit{
  public BidirectionalValve(String name) {
    super(name);
    conduitAmount = 0;
    configurable = true;

    config(Integer.class, (ThrottleValveBuild e, Integer c) -> e.reverses[c] = !e.reverses[c]);
    config(IntSeq.class, (ThrottleValveBuild e, IntSeq c) -> {
      e.liquidsBuffer = new ClusterLiquidModule[c.get(0)];
      for (int i = 0; i < e.liquidsBuffer.length; i++) {
        e.liquidsBuffer[i] = new ClusterLiquidModule();
        e.reverses[i] = c.get(i + 1) == 1;
      }
    });
  }

  public class ThrottleValveBuild extends ClusterConduitBuild{
    public boolean[] reverses;

    @Override
    public void onReplaced(ReplaceBuildComp old) {
      super.onReplaced(old);
      liquidsBuffer = old.<ClusterConduitBuild>getBuild().liquidsBuffer;
      reverses = new boolean[liquidsBuffer.length];
    }

    @Override
    public void buildConfiguration(Table table) {
      table.table(Styles.black6, conduits -> {
        for(int i=0; i<liquidsBuffer.length; i++){
          int index = i;
          conduits.button(t -> t.add(Core.bundle.get("misc.conduit") + "#" + index).left().grow(), Styles.underlineb, () -> configure(index))
              .update(b -> b.setChecked(reverses[index])).size(250, 35).pad(0);
          conduits.row();
        }
      });
    }

    @Override
    public float moveLiquidForward(boolean leaks, Liquid liquid) {
      Tile next = tile.nearby(rotation), pre = tile.nearby((rotation + 2)%4);

      float flow = 0;
      for(int i=0; i<liquidsBuffer.length; i++){
        LiquidModule liquids = liquidsBuffer[i];
        Tile tar = reverses[i]? pre: next;

        if (tar == null) continue;

        if(tar.build instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
          flow += moveLiquid(mu, i, liquids.current());
        }
        else if(tar.build != null){
          this.liquids = liquids;
          flow += moveLiquid(tar.build, liquids.current());
          this.liquids = cacheLiquids;
        }
      }

      return flow;
    }

    @Override
    public Object config() {
      IntSeq res = IntSeq.with(liquidsBuffer.length);
      for (boolean b : reverses) {
        res.add(b? 1: 0);
      }
      return res;
    }

    @Override
    public boolean conduitAccept(MultLiquidBuild source, int index, Liquid liquid) {
      noSleep();
      LiquidModule liquids = liquidsBuffer[index];
      return (source.interactable(team) && liquids.currentAmount() < 0.01f || liquids.current() == liquid && liquids.currentAmount() < liquidCapacity)
          && (reverses[index]? tile.absoluteRelativeTo(source.tile.x, source.tile.y) == rotation: source.tile.absoluteRelativeTo(tile.x, tile.y) == rotation);
    }

    @Override
    public LiquidModule getModuleAccept(Building source, Liquid liquid) {
      for (int i = 0; i < liquidsBuffer.length; i++) {
        if (!(reverses[i]? tile.absoluteRelativeTo(source.tile.x, source.tile.y) == rotation: source.tile.absoluteRelativeTo(tile.x, tile.y) == rotation)) continue;

        if (liquidsBuffer[i].currentAmount() <= 0.001f || (liquidsBuffer[i].current == liquid && liquidsBuffer[i].currentAmount() < liquidCapacity)) return liquidsBuffer[i];
      }
      return null;
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.interactable(team) && (getModuleAccept(source, liquid) != null || !isFull());
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.i(reverses.length);
      for (boolean b : reverses) {
        write.bool(b);
      }
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      int len = read.i();
      reverses = new boolean[len];
      for (int i = 0; i < reverses.length; i++) {
        reverses[i] = read.bool();
      }
    }
  }
}
