package singularity.contents;

import arc.graphics.Color;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.SunMesh;
import mindustry.type.Planet;
import singularity.world.gen.ForyustPlanetGenerator;

public class SglPlanets implements ContentList{
  /**曦泽*/
  public static Planet seazer,
  /**森榆*/
  foryust,
  /**鸢羽*/
  firther;

  @Override
  public void load(){
    seazer = new Planet("seazer", null, 5f){{
      bloom = true;
      accessible = false;
      meshLoader = () -> new SunMesh(
          this, 5, 5, 0.35, 1.85, 1.2, 1.1, 1.1f,
          Color.valueOf("F4A120"),
          Color.valueOf("F4B83A"),
          Color.valueOf("F4CA5E"),
          Color.valueOf("F4D575"),
          Color.valueOf("F4E38D"),
          Color.valueOf("F4E7A3")
      );
    }};

    foryust = new Planet("foryust", seazer, 3.2f){{
      generator = new ForyustPlanetGenerator();
      meshLoader = () -> new HexMesh(this, 4);

    }};
  }
}
