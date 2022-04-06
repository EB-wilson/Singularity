package singularity.world.blocks.drills;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.graphic.SglDraw;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.components.EdgeLinkerBuildComp;
import singularity.world.components.EdgeLinkerComp;
import singularity.world.distribution.buffers.ItemsBuffer;
import universecore.annotations.Annotations;

import java.util.TreeSet;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class MatrixMiner extends DistNetBlock implements EdgeLinkerComp{
  public float drillTime = 60;
  public float rotatorSpeed = 1.5f;
  public int drillBufferCapacity = 20;
  public float armSpeed = 0.0375f;

  public int linkLength = 20;
  public float hardMultiple = 50;

  public Color drillColor = Color.valueOf("C6EFEC");

  public TextureRegion drillRegion, drillTop;
  public TextureRegion armRegion, armBitRegion;

  public MatrixMiner(String name){
    super(name);
    configurable = true;
  }

  @Override
  public void appliedConfig(){
    super.appliedConfig();
    config(IntSeq.class, (MatrixMinerBuild e, IntSeq seq) -> {
      e.armTasks.add(seq.get(0));
      e.armTasks.add(seq.get(1));
      e.armTasks.add(seq.get(2));
    });
  }

  @Override
  public boolean linkable(EdgeLinkerComp other){
    return other instanceof MatrixMinerEdge;
  }

  @Override
  public void init(){
    super.init();
    clipSize = Math.max(clipSize, linkLength * tilesize * 2);
  }

  public class MatrixMinerBuild extends DistNetBuild implements EdgeLinkerBuildComp{
    public IntMap<MatrixDrillBit> mining = new IntMap<>();
    public IntSeq armTasks = new IntSeq();

    public TreeSet<Integer> tempMine = new TreeSet<>();

    public boolean configuring;
    public ItemsBuffer itemBuffer = new ItemsBuffer();
    public Rect rect;
    public Vec2 armPos = new Vec2();

    @Override
    public void linked(EdgeLinkerBuildComp next){
      if(getEdges().isClosure()){
        float[] vertices = getEdges().getPoly().getTransformedVertices();
        float x = 0, y = 0, mx = 0, my = 0;
        for(int i = 0; i < vertices.length; i += 2){
          x = Math.min(x, vertices[i]);
          y = Math.min(y, vertices[i+1]);
          mx = Math.max(mx, vertices[i]);
          my = Math.max(my, vertices[i+1]);
        }
        rect = new Rect(x, y, mx - x, my - y);
        armPos.set(x, y);

        if(!tempMine.isEmpty()){
          for(Integer integer: tempMine){
            assignTask(Point2.x(integer), Point2.y(integer), 0);
          }
          tempMine.clear();
        }
      }
    }

    public void assignTaskClient(int x, int y, int cmdCode){
      armTasks.add(x);
      armTasks.add(y);
      armTasks.add(cmdCode);
    }

    public void assignTask(int x, int y, int cmdCode){
      configure(new IntSeq(new int[]{x, y, cmdCode}));
    }

    @Override
    public void delinked(EdgeLinkerBuildComp next){
      rect = null;
      armPos.set(x, y);
      for(IntMap.Entry<MatrixDrillBit> bitEntry: mining){
        tempMine.add(bitEntry.key);
        bitEntry.value.remove();
      }
      armTasks.clear();
    }

    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      items = itemBuffer.generateBindModule();
      return this;
    }

    @Override
    public void updateTile(){
      for(MatrixDrillBit bit: mining.values()){
        bit.update();
      }
      if(!updateValid()) return;

      if(armTasks.size >= 3 && handleTask(
          armTasks.get(0),
          armTasks.get(1),
          armTasks.get(2)
      )){
        for(int i = 0; i < 3; i++){
          armTasks.removeIndex(i);
        }
      }
    }

    public boolean handleTask(int x, int y, int cmdCode){
      float worldX = x*tilesize, worldY = y*tilesize;
      armPos.lerpDelta(worldX, worldY, armSpeed);
      switch(cmdCode){
        case 0:{
          if(Math.abs(armPos.x - worldX) < 0.1f && Math.abs(armPos.y - worldY) < 0.1f){
            mining.remove(Point2.pack(x, y));
            MatrixDrillBit bit = new MatrixDrillBit();
            bit.x = x;
            bit.y = y;
            int pos = Point2.pack(x, y);
            bit.ore = getMine(pos);
            mining.put(pos, bit);
            return true;
          }
        }
        case 1:{
          MatrixDrillBit bit = mining.get(Point2.pack(x, y));
          if(bit != null){
            bit.remove();
          }
          return true;
        }
        case 2:{
          if(Math.abs(armPos.x - worldX) < 0.1f && Math.abs(armPos.y - worldY) < 0.1f){
            MatrixDrillBit bit = mining.get(Point2.pack(x, y));
            if(bit == null) return true;
            int move = Math.min(bit.buffered, itemCapacity - items.get(bit.ore));
            bit.buffered -= move;
            items.add(bit.ore, move);
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public boolean updateValid(){
      return distributor.network.netValid() && rect != null;
    }

    @Override
    public boolean onConfigureTileTapped(Building other){
      if(other == this){
        configuring = !configuring;
        return false;
      }

      return true;
    }

    @Override
    public void buildConfiguration(Table table){
      super.buildConfiguration(table);

      Vars.ui.hudGroup.addChild(new Element(){
        float originX, originY;
        float currX, currY;

        {
          touchablility = () -> configuring? Touchable.enabled: Touchable.disabled;

          addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
              originX = currX = x;
              originY = currY = y;
              return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
              currX += x;
              currY += y;
              super.touchDragged(event, x, y, pointer);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
              assignConfigTasks(originX, originY, currX, currY);
              super.touchUp(event, x, y, pointer, button);
            }
          });

          addCaptureListener(new ElementGestureListener(){
            @Override
            public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
              originX = currX = x;
              originY = currY = y;
              super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
              currX += deltaX;
              currY += deltaY;
              super.pan(event, x, y, deltaX, deltaY);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
              assignConfigTasks(originX, originY, currX, currY);
              super.touchUp(event, x, y, pointer, button);
            }
          });
        }

        @Override
        public void draw(){
          super.draw();
          Vec2 originPos = Core.camera.project((float) (Math.floor(Math.min(originX, currX)/tilesize)*tilesize), (float) (Math.floor(Math.min(originY, currY)/tilesize)*tilesize));
          float x = originPos.x, y = originPos.y;
          Vec2 otherPos = Core.camera.project((float) (Math.ceil(Math.max(originX, currX)/tilesize)*tilesize), (float) (Math.ceil(Math.max(originY, currY)/tilesize)*tilesize));
          float w = otherPos.x - x, h = otherPos.y - y;

          Lines.stroke(1.5f, Pal.accent);
          Lines.rect(x, y, w, h);
        }

        @Override
        public void act(float delta){
          super.act(delta);
          Vec2 pos = Core.input.mouseScreen(rect.x, rect.y);
          setPosition(pos.x, pos.y);
          setBounds(pos.x, pos.y, rect.width, rect.height);
        }
      });
    }

    public Item getMine(int pos){
      Tile tile = Vars.world.tile(pos);
      if(tile != null && tile.floor().itemDrop != null && tile.block() == null){
        return tile.floor().itemDrop;
      }
      return null;
    }

    public void assignConfigTasks(float x, float y, float ox, float oy){
      int originX = (int) Math.floor(Math.min(x, ox)/tilesize);
      int originY = (int) Math.floor(Math.min(y, oy)/tilesize);
      int otherX = (int) Math.ceil(Math.max(x, ox)/tilesize);
      int otherY = (int) Math.ceil(Math.max(y, oy)/tilesize);

      MatrixDrillBit b;
      int dx = otherX - originX, dy = otherY - originY;
      for(int posY = 0; posY < dy; posY++){
        for(int posX = 0; posX < dx; posX++){
          assignTask(originX + posX, originY + posY, (b = mining.get(Point2.pack(originX + posX, originY + posY))) != null && b.working()? 1: 0);
        }
      }
    }

    @Override
    public void drawConfigure(){
      super.drawConfigure();
      int x, y;
      int cmdCode;
      for(int i = 0; i < armTasks.size; i+=3){
        x = armTasks.get(i);
        y = armTasks.get(i + 1);
        cmdCode = armTasks.get(i + 2);


      }
    }

    @Override
    public void draw(){
      super.draw();
      drawArm();
    }

    public void drawArm(){
      if(rect == null) return;
      Lines.line(
          armRegion,
          armPos.x, rect.y,
          armPos.x, rect.y + rect.height,
          true
      );
      Lines.line(
          armRegion,
          armPos.x, rect.y,
          armPos.x, rect.y + rect.height,
          true
      );
      Draw.rect(armBitRegion, armPos.x, armPos.y);
    }

    public class MatrixDrillBit{
      public int x, y;
      public float alpha;
      public float drillSpeed;
      public float progress;
      public float warmup;
      public float totalProgress;
      public Item ore;
      public int buffered;

      public boolean removing;

      public void update(){
        alpha = Mathf.lerpDelta(alpha, removing? 0: 1, 0.02f);
        if(alpha < 0.02f && removing) mining.remove(Point2.pack(x, y));

        ore = getMine(Point2.pack(x, y));
        if(ore == null){
          remove();
          return;
        }

        warmup = Mathf.lerpDelta(warmup, updateValid()? 1: 0, 0.02f);
        drillSpeed = warmup / (drillTime + ore.hardness*hardMultiple);

        float delta = edelta()*warmup;
        totalProgress += delta;

        if(buffered < drillBufferCapacity){
          progress += delta;

          float delay = drillTime + ore.hardness*hardMultiple;
          if(progress > delay){
            buffered += 1;
            progress = 0;
          }
        }
      }

      public void draw(){
        Draw.z(Layer.bullet);
        Draw.color(drillColor);
        Draw.alpha(alpha);
        Draw.rect(drillRegion, x*tilesize, y*tilesize, drillRegion.width*alpha, drillRegion.height*alpha, totalProgress*rotatorSpeed);
        Draw.color(ore.color);
        Draw.alpha(alpha);
        Draw.rect(drillTop, x*tilesize, y*tilesize);

        Lines.stroke(0.65f, Pal.lightishGray);
        SglDraw.dashCircle(x*tilesize, y*tilesize, tilesize*0.75f, Time.time/1.6f);
      }

      public void remove(){
        removing = true;
      }

      public boolean working(){
        return !removing;
      }
    }
  }
}
