package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.ObjectMap;
import mindustry.gen.Building;
import singularity.world.DirEdges;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.components.distnet.DistComponent;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;

import java.util.Arrays;

public class NetPluginComp extends DistNetBlock{
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
   *
   * 若为-1则表明可任意连接
   * */
  public byte connectReq = -1;

  public int computingPower = 0;

  protected ObjectMap<DistBuffers<?>, Integer> buffersSize = new ObjectMap<>();

  public TextureRegion interfaceLinker;

  public NetPluginComp(String name){
    super(name);
    rotate = true;
    rotateDraw = false;
    update = true;
  }

  @Override
  public void load(){
    super.load();
    interfaceLinker = Core.atlas.find(name + "_interface");
  }

  public void setBufferSize(DistBuffers<?> buffer, int size){
    buffersSize.put(buffer, size);
  }

  public class NetPluginCompBuild extends DistNetBuild implements DistComponent{
    DistributeNetwork[] linked = new DistributeNetwork[4];

    @Override
    public NetPluginComp block(){
      return (NetPluginComp) block;
    }

    @Override
    public int computingPower(){
      return computingPower;
    }

    @Override
    public ObjectMap<DistBuffers<?>, Integer> bufferSize(){
      return buffersSize;
    }

    @Override
    public void networkUpdated(){
      tag: for(int i = 0; i < 4; i++){
        DistributeNetwork otherNet = null;
        linked[i] = null;

        for(Point2 p: DirEdges.get(block.size, i)){
          if(nearby(p.x, p.y) instanceof ComponentBus.ComponentBusBuild bus && bus.linkable(tile)){
            if(otherNet == null){
              otherNet = bus.distributor.network;
            }
            else if(otherNet != bus.distributor.network) continue tag;
          }
          else continue tag;
        }

        linked[i] = otherNet;
      }

      if(!componentValid()) Arrays.fill(linked, null);
    }

    @Override
    public boolean componentValid(){
      DistributeNetwork group = null;
      for(int i = 0; i < linked.length; i++){
        if((0b0001 << Mathf.mod(i - rotation, 4) & connectReq) == 0) continue;

        if(linked[i] == null) return false;
        if(group == null){
          group = linked[i];
        }
        else if(group != linked[i]) return false;
      }

      return true;
    }

    @Override
    public void draw(){
      Draw.rect(region, x, y);
      if(!componentValid()) return;

      for(int i = 0; i < linked.length; i++){
        if((0b0001 << Mathf.mod(i - rotation, 4) & connectReq) == 0) continue;
        int r = (i + rotation)%4;
        Draw.scl(1, r == 1 || r == 2? -1: 1);
        if(linked[i] != null) Draw.rect(interfaceLinker, x, y, 90*(i + rotation));
      }
    }

    @Override
    public void onProximityAdded(){
      if (this.power != null) this.updatePowerGraph();

      for(Building building: proximity){
        if(building instanceof ComponentBus.ComponentBusBuild bus){
          bus.distributor.network.add((DistElementBuildComp) this);
        }
      }
    }

    @Override
    public void updateNetLinked(){
      netLinked.clear();
    }
  }
}
