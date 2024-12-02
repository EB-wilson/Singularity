package singularity.game.planet;

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.type.Sector;

import java.io.*;

public class Chunk {
  public final Sector sector;
  public final ObjectMap<Class<? extends ChunkContext>, ChunkContext> contexts = new ObjectMap<>();

  public boolean isForeground = false;

  public Chunk(Sector sector) {
    this.sector = sector;
  }

  public void initial(){
    for (ChunkContext context : contexts.values()) {
      context.init(this);
    }
  }

  public void addContext(ChunkContext context) {
    contexts.put(context.getClass(), context);
  }

  public void addContext(Class<? extends ChunkContext> chunkType, ChunkContext context) {
    if (!chunkType.isAssignableFrom(context.getClass())) throw new IllegalArgumentException("Context class mismatch");

    contexts.put(chunkType, context);
  }

  @SuppressWarnings("unchecked")
  public <T extends ChunkContext> T getContext(Class<T> chunkType) {
    T res = (T) contexts.get(chunkType);

    if (res == null) {
      Class<?> resC = null;
      for (ObjectMap.Entry<Class<? extends ChunkContext>, ChunkContext> entry : contexts) {
        if (chunkType.isAssignableFrom(entry.key)) {
          if (resC == null) {
            res = (T) entry.value;
            resC = entry.key;
          }
          else if (entry.key.isAssignableFrom(resC)){
            res = (T) entry.value;
            resC = entry.key;
          }
        }
      }
    }

    return res;
  }

  public <T extends ChunkContext> void apply(Class<T> chunkType, Cons<T> cons){
    T context = getContext(chunkType);
    if (context == null) throw new IllegalArgumentException("No context of type " + chunkType.getName() + " found");

    cons.get(context);
  }

  public void chunkInstall(){
    isForeground = true;

    for (ChunkContext context : contexts.values()) {
      context.install();
    }
  }

  public void chunkUninstall(){
    isForeground = false;

    for (ChunkContext context : contexts.values()) {
      context.uninstall();
    }
  }

  public void update(float delta){
    for (ChunkContext context : contexts.values()) {
      if (isForeground) context.updateFore(delta);
      else context.updateBack(delta);

      context.update(delta);
    }
  }

  public void saveChunk(Writes writes){
    ObjectMap<String, byte[]> data = new ObjectMap<>();
    for (ChunkContext context : contexts.values()) {
      String str = context.getContextName();

      try(ByteArrayOutputStream bu = new ByteArrayOutputStream()) {
        context.save(new Writes(new DataOutputStream(bu)));

        byte[] bytes = bu.toByteArray();
        data.put(str, bytes);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    writes.i(data.size);
    for (ObjectMap.Entry<String, byte[]> entry : data) {
      writes.str(entry.key);
      writes.b(entry.value.length);
      writes.b(entry.value);
    }
  }

  public void loadChunk(Reads reads){
    ObjectMap<String, byte[]> data = new ObjectMap<>();

    int size = reads.i();
    for (int i = 0; i < size; i++) {
      String str = reads.str();
      int len = reads.b();
      byte[] bytes = reads.b(len);

      data.put(str, bytes);
    }

    for (ChunkContext context : contexts.values()) {
      String str = context.getContextName();
      if (data.containsKey(str)) {
        try(ByteArrayInputStream in = new ByteArrayInputStream(data.get(str))) {
          Reads read = new Reads(new DataInputStream(in));
          context.load(read);
        } catch (RuntimeException | IOException e) {
          Log.err("[Singularity] Failed to load chunk context " + str + " for sector " + sector);
        }
      }
    }
  }
}
