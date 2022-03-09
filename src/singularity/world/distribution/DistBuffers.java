package singularity.world.distribution;

import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.buffers.GasesBuffer;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.buffers.LiquidsBuffer;

public class DistBuffers<T extends BaseBuffer<?, ?, ?>>{
  public static Seq<DistBuffers<?>> all = new Seq<>();
  public static final ObjectMap<DistBuffers<?>, Integer> defBufferCapacity = new ObjectMap<>();
  
  public static DistBuffers<ItemsBuffer> itemBuffer = new DistBuffers<>(ItemsBuffer::new);
  public static DistBuffers<LiquidsBuffer> liquidBuffer = new DistBuffers<>(LiquidsBuffer::new);
  public static DistBuffers<GasesBuffer> gasBuffer = new DistBuffers<>(GasesBuffer::new);
  
  protected final Prov<T> initializer;
  
  public DistBuffers(Prov<T> initializer){
    this(initializer, 1024);
  }
  
  public DistBuffers(Prov<T> initializer, int defaultCapacity){
    this.initializer = initializer;
    all.add(this);
    defBufferCapacity.put(this, defaultCapacity);
  }
  
  public T get(){
    T buffer = initializer.get();
    buffer.capacity = defBufferCapacity.get(this);
    return buffer;
  }
  
  public T get(int capacity){
    T buffer = initializer.get();
    buffer.capacity = capacity;
    return buffer;
  }
}
