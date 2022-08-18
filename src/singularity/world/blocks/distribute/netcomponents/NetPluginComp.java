package singularity.world.blocks.distribute.netcomponents;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.util.Tmp;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.world.components.distnet.DistComponent;
import singularity.world.distribution.DistBuffers;

public class NetPluginComp extends Block{
  public static final byte RIGHT = 0b0001;
  public static final byte UP = 0b0010;
  public static final byte LEFT = 0b0100;
  public static final byte DOWN = 0b1000;

  /**按位标记链接方向：
   * <pre>{@code
   *       0b0010
   *        ┌───┐
   * 0b0100 │   │ 0b0001
   *        └───┘
   *       0b1000
   * }</pre>
   * 按位作或运算取链接位置组合，比如上下，则为{@code 0b0010 | 0b1000 = 0b1010}
   * */
  public byte connectReq = 0;

  public int computingPower = 0;

  protected ObjectMap<DistBuffers<?>, Integer> buffersSize = new ObjectMap<>();

  public TextureRegion interfaceLight, interfaceDark;

  public NetPluginComp(String name){
    super(name);
    rotate = true;
    update = true;
  }

  @Override
  public void load(){
    super.load();

  }

  public void setBufferSize(DistBuffers<?> buffer, int size){
    buffersSize.put(buffer, size);
  }

  public class NetPluginCompBuild extends Building implements DistComponent{
    CompBusGroup[] linked = new CompBusGroup[4];
    CompBusGroup linkedGroup;

    @Override
    public int computingPower(){
      return computingPower;
    }

    @Override
    public ObjectMap<DistBuffers<?>, Integer> bufferSize(){
      return buffersSize;
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      linkedGroup = null;

      Tmp.v1.set(0, block.size/2f + 0.5f);
      float x = tile.x + block.offset, y = tile.y + block.offset;
      tag: for(int i = 0; i < linked.length; i++){
        CompBusGroup group = null;
        linked[i] = null;

        for(float off = -block.size/2f + 0.5f; off <= block.size/2f - 0.5f; off++){
          Tmp.v2.set(Tmp.v1);
          Tmp.v2.add(0, off);
          Tmp.v2.rotate90(i);

          if(nearby((int)(x + Tmp.v2.x), (int)(y + Tmp.v2.y)) instanceof ComponentBus.ComponentBusBuild bus && bus.linkable(tile)){
            if(group == null){
              group = bus.busGroup;
            }
            else if(group != bus.busGroup) continue tag;
          }
          else continue tag;
        }

        linked[i] = group;
        linkedGroup = group;
      }

      if(!connectValid()) linkedGroup = null;
    }

    public CompBusGroup connectBus(){
      return linkedGroup;
    }

    public boolean connectValid(){
      CompBusGroup group = null;
      for(int i = 0; i < linked.length; i++){
        if(group != null && group != linked[i]) return false;

        if((0b0001 << (i - rotation) & connectReq) == 0) continue;

        if(group == null){
          if(linked[i] == null) return false;
          group = linked[i];
        }
      }

      return true;
    }

    @Override
    public void draw(){
      Draw.rect(region, x, y);
      if(!connectValid()) return;

      for(int i = 0; i < linked.length; i++){
        if(linked[i] != null) Draw.rect((i + rotation)%4 <= 1? interfaceLight: interfaceDark, x, y, 90*(i + rotation));
      }
    }
  }
}
