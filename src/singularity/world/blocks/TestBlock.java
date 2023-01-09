package singularity.world.blocks;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import singularity.graphic.Distortion;
import singularity.graphic.SglDraw;

public class TestBlock extends SglBlock{
  public TestBlock(String name){
    super(name);
    update = true;
    solid = true;
  }

  static Distortion dist = new Distortion(Layer.min, Layer.flyingUnit - 0.02f);

  public class TestBlockBuild extends SglBuilding{


    @Override
    public void draw() {
      Draw.z(Layer.flyingUnit);
      SglDraw.drawDistortion("testDis", dist, d -> {
        Distortion.drawVoidDistortion(x, y, 16, 30);
      });

      Tmp.v1.set(5, 0).setAngle(Time.time);

      Draw.z(Layer.flyingUnit + 0.5f);
      Draw.color(Color.black);
      Fill.circle(x, y, 14);

      SglDraw.startBloom(Layer.flyingUnit + 1);
      Lines.stroke(4, Pal.lighterOrange);
      Lines.circle(x, y, 16);
      SglDraw.endBloom();
    }
  }
}
