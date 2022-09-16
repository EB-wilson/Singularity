package singularity.world.distribution;

import arc.func.Prov;
import arc.struct.Seq;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.buffers.LiquidsBuffer;

public class DistBuffers<T extends BaseBuffer<?, ?, ?>>{
  public static Seq<DistBuffers<?>> all = new Seq<>();
  
  public static DistBuffers<ItemsBuffer> itemBuffer = new DistBuffers<>(ItemsBuffer::new);
  public static DistBuffers<LiquidsBuffer> liquidBuffer = new DistBuffers<>(LiquidsBuffer::new);
  
  protected final Prov<T> initializer;
  
  public DistBuffers(Prov<T> initializer){
    this.initializer = initializer;
    all.add(this);
  }
  
  public T get(int capacity){
    T buffer = initializer.get();
    buffer.capacity = capacity;
    return buffer;
  }
}
