package singularity.game.planet;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.type.Planet;
import mindustry.type.Sector;

public class PlanetContext {
  public final Planet planet;

  public final ObjectMap<Sector, Chunk> chunks = new ObjectMap<>();
  public final Seq<ChunkContextIncubator> incubators = new Seq<>();

  @Nullable public Chunk currentChunk;

  private boolean initialized = false;

  public PlanetContext(Planet planet) {
    this.planet = planet;
  }

  public void addIncubator(ChunkContextIncubator incubator) {
    if (initialized) throw new IllegalStateException("Context is already initialized");

    this.incubators.add(incubator);
  }

  public void addIncubator(ChunkContextIncubator... incubators) {
    if (initialized) throw new IllegalStateException("Context is already initialized");

    this.incubators.add(incubators);
  }

  public Chunk getChunk(Sector sector) {
    return chunks.get(sector);
  }

  public void initial(){
    for (Sector sector : planet.sectors) {
      Chunk chunk = new Chunk(sector);
      chunks.put(sector, chunk);

      for (ChunkContextIncubator incubator : incubators) {
        incubator.begin();
        while (incubator.hasNext()) {
          Class<? extends ChunkContext> type = incubator.peekType();

          if (type == null) chunk.addContext(incubator.next());
          else chunk.addContext(type, incubator.next());
        }
      }

      chunk.initial();
    }

    initialized = true;
  }

  public void enterSector(Sector sector){
    if (sector.planet != planet) throw new IllegalArgumentException("Sector is not a part of this global");

    if (currentChunk != null) currentChunk.chunkUninstall();
    currentChunk = chunks.get(sector);
    currentChunk.chunkInstall();
  }

  public void exitSector(){
    if (currentChunk != null) currentChunk.chunkUninstall();
    currentChunk = null;
  }

  public void update(float delta) {
    for (Chunk chunk : chunks.values()) {
      chunk.update(delta);
    }
  }
}
