package singularity.contents;

import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.content.Planets;
import mindustry.game.Team;
import mindustry.graphics.Pal;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.graphics.g3d.SunMesh;
import mindustry.maps.planet.SerpuloPlanetGenerator;
import mindustry.type.Planet;
import singularity.world.gen.ForyustGenerator;

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
      generator = new SerpuloPlanetGenerator();
      meshLoader = () -> new HexMesh(this, 6);
      cloudMeshLoader = () -> new MultiMesh(
          new HexSkyMesh(this, 11, 0.15f, 0.13f, 5, new Color().set(Pal.spore).mul(0.9f).a(0.75f), 2, 0.45f, 0.9f, 0.38f),
          new HexSkyMesh(this, 1, 0.6f, 0.16f, 5, Color.white.cpy().lerp(Pal.spore, 0.55f).a(0.75f), 2, 0.45f, 1f, 0.41f)
      );

      launchCapacityMultiplier = 0.5f;
      sectorSeed = 2;
      allowWaves = true;
      allowWaveSimulation = true;
      allowSectorInvasion = true;
      allowLaunchSchematics = true;
      enemyCoreSpawnReplace = true;
      allowLaunchLoadout = true;
      //doesn't play well with configs
      prebuildBase = false;
      ruleSetter = r -> {
        r.waveTeam = Team.crux;
        r.placeRangeCheck = false;
        r.showSpawns = false;
      };
      iconColor = Color.valueOf("7d4dff");
      atmosphereColor = Color.valueOf("3c1b8f");
      atmosphereRadIn = 0.02f;
      atmosphereRadOut = 0.3f;
      startSector = 15;
      alwaysUnlocked = true;
      landCloudColor = Pal.spore.cpy().a(0.5f);
      hiddenItems.addAll(Items.erekirItems).removeAll(Items.serpuloItems);
    }};
  }
}
