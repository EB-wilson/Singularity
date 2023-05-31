package singularity.world.blocks.product;

import arc.math.geom.Vec2;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;

import static mindustry.Vars.tilesize;

public class HoveringUnitFactory extends SglUnitFactory{
  public float outputRange = 0;

  public HoveringUnitFactory(String name) {
    super(name);
  }

  @Override
  public void init() {
    super.init();
    configurable = outputRange > size*tilesize;
  }

  public class HoveringUnitFactoryBuild extends SglUnitFactoryBuild{
    public final Vec2 payloadReleasePos = new Vec2();

    @Override
    public boolean onConfigureTapped(float x, float y) {
      float dst = dst(x, y);
      if (dst > outputRange) return false;
      else if (dst < size*tilesize){
        payloadReleasePos.setZero();
        return true;
      }
      else {
        payloadReleasePos.set(x - this.x, y - this.y);
        return true;
      }
    }

    @Override
    public void drawConfigure() {
      super.drawConfigure();
      if (outputRange > size*tilesize) Drawf.circles(x, y, outputRange, Pal.accent);
    }

    @Override
    public Vec2 outputtingOffset() {
      return payloadReleasePos;
    }

    @Override
    public void drawConstructingPayload() {

    }
  }
}
