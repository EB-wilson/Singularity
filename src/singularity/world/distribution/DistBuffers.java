package singularity.world.distribution;

import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.buffers.ItemsBuffer;

public class DistBuffers<T extends BaseBuffer<?, ?>>{
  public static final ObjectMap<DistBuffers<?>, Integer> defBufferCapacity = new ObjectMap<>();
  
  public static DistBuffers<ItemsBuffer> itemBuffer = new DistBuffers<>(ItemsBuffer::new);
  //public static DistBuffers<LiquidsBuffer> liquidBuffer = new DistBuffers<>(LiquidsBuffer::new);
  //public static DistBuffers<GasesBuffer> gasBuffer = new DistBuffers<>(GasesBuffer::new);
  
  public static Seq<DistBuffers<?>> all = new Seq<>();
  
  protected final Prov<T> initializer;
  protected T buffer;
  
  public DistBuffers(Prov<T> initializer){
    this(initializer, 1024);
  }
  
  public DistBuffers(Prov<T> initializer, int defaultCapacity){
    this.initializer = initializer;
    all.add(this);
    defBufferCapacity.put(this, defaultCapacity);
  }
  
  public T get(){
    return buffer == null? buffer = initializer.get(): buffer;
  }
}
