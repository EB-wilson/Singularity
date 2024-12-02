package singularity.game;

import arc.Events;
import arc.struct.ObjectMap;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.type.Planet;
import mindustry.type.Sector;
import singularity.game.planet.Chunk;
import singularity.game.planet.PlanetContext;

public class SingularityGameLogic {
  private final ObjectMap<Planet, PlanetContext> planets = new ObjectMap<>();

  public boolean inSingularity = false;
  public boolean paused = false;

  public Chunk currentChunk;

  public PlanetContext makePlanetContext(Planet planet){
    PlanetContext context = new PlanetContext(planet);
    planets.put(planet, context);
    return context;
  }

  public PlanetContext getPlanetContext(Planet planet){
    return planets.get(planet);
  }

  public void assignListener(){
    Events.on(EventType.WorldLoadEndEvent.class, e -> {
      Sector sector = Vars.state.rules.sector;

      if (inSingularity && planets.containsKey(sector.planet)) {
        planets.get(sector.planet).enterSector(sector);

        currentChunk = planets.get(sector.planet).getChunk(sector);
      }
    });

    Events.on(EventType.ResetEvent.class, e -> {
      if (!inSingularity || currentChunk == null) return;

      planets.get(currentChunk.sector.planet).exitSector();
      currentChunk = null;
    });
  }

  public void init(){
    Events.on(EventType.ContentInitEvent.class, e -> {
      planets.values().forEach(PlanetContext::initial);
    });
  }

  public void update(){
    if(!paused && inSingularity) {
      planets.values().forEach(e -> e.update(Time.delta));
    }
  }
}
