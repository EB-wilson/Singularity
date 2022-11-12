package singularity.world.blocks.liquid;

import mindustry.gen.Building;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.Takeable;

public class ClusterRouter extends MultLiquidBlock{
  public ClusterRouter(String name) {
    super(name);
    conduitAmount = 1;

    config(Integer.class, (ClusterRouterBuild e, Integer c) -> {
      e.liquidsBuffer = new ClusterLiquidModule[c];
      for (int i = 0; i < e.liquidsBuffer.length; i++) {
        e.liquidsBuffer[i] = new ClusterLiquidModule();
      }
    });
  }

  @Annotations.ImplEntries
  public class ClusterRouterBuild extends MultLiquidBuild implements Takeable {
    @Override
    public void updateTile() {
      super.updateTile();

      dumpClusterLiquid();
    }

    public void dumpClusterLiquid() {
      for (int i = 0; i < liquidsBuffer.length; i++) {
        ClusterLiquidModule liquids = liquidsBuffer[i];
        int index = i;
        Building dump = getNext("liquid", e -> {
          if (e instanceof MultLiquidBuild mu && mu.shouldClusterMove(this)){
            return mu.conduitAccept(this, index, liquids.current());
          }
          return e.acceptLiquid(this, liquids.current());
        });

        if (dump instanceof MultLiquidBuild mu){
          moveLiquid(mu, i, liquids.current());
        }
        else moveLiquid(dump, liquids.current());
      }
    }

    @Override
    public Object config() {
      return liquidsBuffer.length;
    }
  }
}
