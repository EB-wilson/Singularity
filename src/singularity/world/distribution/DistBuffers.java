package singularity.world.distribution;

import arc.func.Prov;
import arc.struct.Seq;
import mindustry.ctype.ContentType;
import mindustry.gen.Building;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.buffers.LiquidsBuffer;
import singularity.world.modules.SglLiquidModule;

public abstract class DistBuffers<T extends BaseBuffer<?, ?, ?>>{
  public static Seq<DistBuffers<?>> all = new Seq<>();
  
  public static DistBuffers<ItemsBuffer> itemBuffer = new DistBuffers<>(ContentType.item, 8, ItemsBuffer::new){
    @Override
    public Integer containerUsed(Building build){
      return build.items.total();
    }
  };
  public static DistBuffers<LiquidsBuffer> liquidBuffer = new DistBuffers<>(ContentType.liquid, 4, LiquidsBuffer::new){
    @Override
    public Float containerUsed(Building build){
      if(build.liquids instanceof SglLiquidModule li){
        return li.total();
      }

      float[] total = {0};
      build.liquids.each((l, a) -> total[0] += a);
      return total[0];
    }
  };

  private final ContentType contentType;
  private final Prov<T> initializer;
  private final int unit;
  
  public DistBuffers(ContentType contentType, int unit, Prov<T> initializer){
    this.contentType = contentType;
    this.unit = unit;
    this.initializer = initializer;
    all.add(this);
  }
  
  public T get(int capacity){
    T buffer = initializer.get();
    buffer.capacity = capacity;
    return buffer;
  }

  public ContentType targetType(){
    return contentType;
  }

  public abstract Number containerUsed(Building build);

  public int unit(){
    return unit;
  }
}
