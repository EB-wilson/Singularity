package singularity.game.planet;

import arc.func.Cons2;
import arc.func.Prov;
import arc.struct.Seq;

import java.util.Iterator;

public interface ChunkContextIncubator extends Iterator<ChunkContext> {
  void begin();
  Class<? extends ChunkContext> peekType();

  default void forEach(Cons2<Class<? extends ChunkContext>, ChunkContext> cons) {
    begin();

    while (hasNext()) {
      cons.get(peekType(), next());
    }
  }

  class Single<T extends ChunkContext> implements ChunkContextIncubator {
    private final Prov<T> context;
    private final Class<T> type;
    private boolean finished = true;

    @SuppressWarnings("unchecked")
    public Single(Class<T> type, T context) {
      this.context = () -> (T) context.clone();
      this.type = type;
    }

    @SuppressWarnings("unchecked")
    public Single(T context) {
      this.context = () -> (T) context.clone();
      this.type = (Class<T>) context.getClass();
    }

    public Single(Class<T> type, Prov<T> context) {
      this.context = context;
      this.type = type;
    }

    public Single(Prov<T> context) {
      this.context = context;
      this.type = null;
    }

    @Override public void begin() {finished = false;}
    @Override public Class<T> peekType() {return type;}
    @Override public boolean hasNext() {return !finished;}
    @Override public ChunkContext next() {finished = true;return context.get();}
  }

  class List implements ChunkContextIncubator {
    private static class Pair{
      Class<? extends ChunkContext> type;
      Prov<ChunkContext> prov;

      private Pair(Class<? extends ChunkContext> type, Prov<ChunkContext> prov) {
        this.type = type;
        this.prov = prov;
      }
    }

    private final Seq<Pair> list = new Seq<>();
    private int index;

    public List(ChunkContext... contexts) {
      for (ChunkContext prov : contexts) {
        list.add(new Pair(null, prov::clone));
      }
    }

    @SafeVarargs
    public List(Prov<ChunkContext>... contexts) {
      for (Prov<ChunkContext> prov : contexts) {
        list.add(new Pair(null, prov));
      }
    }

    @SuppressWarnings("unchecked")
    public List(Object... contexts) {
      for (int i = 0; i < contexts.length; i++) {
        list.add(new Pair(
            (Class<? extends ChunkContext>) contexts[i],
            contexts[i + 1] instanceof ChunkContext c? c::clone: (Prov<ChunkContext>) contexts[i + 1]
        ));
      }
    }

    @Override
    public void begin() {
      index = 0;
    }

    @Override
    public Class<? extends ChunkContext> peekType() {
      return list.get(index).type;
    }

    @Override
    public boolean hasNext() {
      return index < list.size;
    }

    @Override
    public ChunkContext next() {
      ChunkContext next = list.get(index).prov.get();
      index++;
      return next;
    }
  }
}
