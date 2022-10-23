package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.scene.Element;
import arc.struct.ObjectMap;
import arc.util.Eachable;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import mindustry.world.meta.StatUnit;
import singularity.graphic.SglDrawConst;
import singularity.util.NumberStrify;
import singularity.world.DirEdges;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.components.distnet.DistComponent;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.draw.DrawDirSpliceBlock;
import singularity.world.meta.SglStat;

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

  protected ObjectMap<DistBufferType<?>, Integer> bufferSize = new ObjectMap<>();

  public TextureRegion interfaceLinker;

  public NetPluginComp(String name){
    super(name);
    rotate = true;
    rotateDraw = false;
    update = true;

    draw = new DrawMulti(
        new DrawDefault(),
        new DrawDirSpliceBlock<NetPluginCompBuild>(){{
          spliceBits = e -> {
            int res = 0;
            for(int i = 0; i < e.linked.length; i++){
              if((0b0001 << Mathf.mod(i - e.rotation, 4) & connectReq) == 0) continue;
              if(e.linked[i] != null) res |= 0b0001 << i;
            }
            return res;
          };
        }}
    );
  }

  @Override
  public void load(){
    super.load();
    interfaceLinker = Core.atlas.find(name + "_interface");
  }

  @Override
  public void setStats(){
    super.setStats();
    if(computingPower > 0) stats.add(SglStat.computingPower, computingPower*60, StatUnit.perSecond);
    if(bufferSize.size > 0){
      stats.add(SglStat.bufferSize, t -> {
        t.defaults().left().fillX().padLeft(10);
        t.row();
        for(ObjectMap.Entry<DistBufferType<?>, Integer> entry: bufferSize){
          if(entry.value <= 0) continue;
          t.add(Core.bundle.get("content." + entry.key.targetType().name() + ".name") + ": " + NumberStrify.toByteFix(entry.value, 2));
          t.row();
        }
      });
    }
    stats.add(SglStat.linkDirections, t -> {
      t.add(new Element(){
        @Override
        public void draw(){
          validate();

          Draw.scl(scaleX, scaleY);
          Lines.stroke(6, Color.lightGray);
          float rx = x + width/2, ry = y + height/2;
          Lines.quad(x, y, x + width, y, x + width, y + height, x, y + height);
          Tmp.v1.set(width/2 - 22, 0);
          Draw.color();
          for(int i = 0; i < 4; i++){
            if((connectReq & 0b0001 << i) == 0) continue;

            Tmp.v1.setAngle(i*90);
            Draw.rect(SglDrawConst.matrixArrow, rx + Tmp.v1.x, ry + Tmp.v1.y, 40, 40, (i - 1)*90);
          }
        }
      }).size(90);
    });
  }

  @Override
  public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list){
    super.drawPlanConfig(plan, list);
  }

  public void setBufferSize(DistBufferType<?> buffer, int size){
    bufferSize.put(buffer, size);
  }

  @Override
  public void drawOverlay(float x, float y, int rotation){
    drawReqBits(rotation, x, y);
  }

  private void drawReqBits(int rotation, float x, float y){
    Draw.color(SglDrawConst.matrixNet);
    for(int dir = 0; dir<4; dir++){
      if((0b0001 << Mathf.mod(dir - rotation, 4) & connectReq) == 0) continue;

      Tmp.v1.set(size*Vars.tilesize/2f + 4, 0).rotate(dir*90);
      Tmp.v2.set(Tmp.v1).rotate90(1).setLength(10 + Mathf.absin(size > 3? Vars.tilesize: size == 3? Vars.tilesize*0.75f: Vars.tilesize/2f, 0.35f));

      float dx = x + Tmp.v1.x, dy = y + Tmp.v1.y;
      Draw.rect(SglDrawConst.squareMarker, dx, dy);

      //贴图是朝上的，在地图绘制时需要向逆时针旋转90度
      Draw.rect(SglDrawConst.matrixArrow, dx + Tmp.v2.x, dy + Tmp.v2.y, dir*90);
      Draw.rect(SglDrawConst.matrixArrow, dx - Tmp.v2.x, dy - Tmp.v2.y, (dir - 2)*90);

      if(size >= 6){
        Tmp.v3.set(Tmp.v2).setLength(1);
        Lines.stroke(3.5f);
        Lines.line(
            dx + 5*Tmp.v1.x, dy + 5*Tmp.v1.y,
            dx + Tmp.v2.x - 2*Tmp.v1.x, dy + Tmp.v2.y - 2*Tmp.v1.y
        );
        Lines.line(
            dx - 5*Tmp.v1.x, dy - 5*Tmp.v1.y,
            dx - Tmp.v2.x + 2*Tmp.v1.x, dy - Tmp.v2.y + 2*Tmp.v1.y
        );
        Drawf.line(SglDrawConst.matrixNet, dx, dy, dx - Tmp.v2.x, dy - Tmp.v2.y);
      }
    }
  }

  public class NetPluginCompBuild extends DistNetBuild implements DistComponent{
    DistributeNetwork[] linked = new DistributeNetwork[4];

    @Override
    public boolean linkable(DistElementBuildComp other){
      return false;
    }

    @Override
    public NetPluginComp block(){
      return (NetPluginComp) block;
    }

    @Override
    public int computingPower(){
      return computingPower;
    }

    @Override
    public ObjectMap<DistBufferType<?>, Integer> bufferSize(){
      return bufferSize;
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

      if(!componentValid()){
        Arrays.fill(linked, null);
      }
      else if(distributor.network.netStructValid()) onPluginValided();
      else onPluginInvalided();
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
    public void onProximityAdded(){
      if (this.power != null) this.updatePowerGraph();

      for(Building building: proximity){
        if(building instanceof ComponentBus.ComponentBusBuild bus){
          bus.distributor.network.add((DistElementBuildComp) this);
        }
      }
    }

    @Override
    public void onProximityRemoved(){
      super.onProximityRemoved();
      onPluginRemoved();
    }

    @Override
    public void updateNetLinked(){
      netLinked().clear();
    }

    public void onPluginValided(){}

    public void onPluginInvalided(){}

    public void onPluginRemoved(){}
  }
}
