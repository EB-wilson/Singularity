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
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.Singularity;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.components.EdgeLinkerBuildComp;
import singularity.world.components.EdgeLinkerComp;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.request.PutItemsRequest;
import universecore.annotations.Annotations;

import java.util.TreeSet;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class MatrixMiner extends DistNetBlock implements EdgeLinkerComp{
  public static final ObjectSet<Item> TMP_ITEMS = new ObjectSet<>();
  public float drillTime = 60;
  public float rotatorSpeed = 1.5f;
  public int drillBufferCapacity = 20;

  public float armSpeed = 0.0375f;
  public float armIdleWait = 120;

  public float bitPostTime = 45;
  public float bitPostDistance = 16;

  public int linkLength = 25;
  public float hardMultiple = 50;

  public float linkOffset = 0;

  public Color drillColor = Color.valueOf("C6EFEC");

  public TextureRegion drillRegion, drillTop;
  public TextureRegion armRegion, armBitRegion, armCapRegion;
  public TextureRegion linkRegion, linkCapRegion;

  public MatrixMiner(String name){
    super(name);
    configurable = true;
    itemCapacity = 64;
  }

  @Override
  public void appliedConfig(){
    config(Integer.class, this::link);
    config(Boolean.class, (MatrixMinerBuild e, Boolean b) -> e.selectMode = b);
    config(IntSeq.class, (MatrixMinerBuild e, IntSeq seq) -> {
      e.armTasks.add(seq.get(0));
      e.armTasks.add(seq.get(1));
      e.armTasks.add(seq.get(2));
      e.armTasks.add(seq.get(3));
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

  @Override
  public void load(){
    super.load();
    drillRegion = Core.atlas.find(name + "_drill");
    drillTop = Core.atlas.find(name + "_drill_top");
    armRegion = Core.atlas.find(name + "_arm");
    armBitRegion = Core.atlas.find(name + "_arm_bit");
    armCapRegion = Core.atlas.find(name + "_arm_cap");
    linkRegion = Core.atlas.find(name + "_linking");
    linkCapRegion = Core.atlas.find(name + "_link_cap");
  }

  @Annotations.ImplEntries
  public class MatrixMinerBuild extends DistNetBuild implements EdgeLinkerBuildComp{
    public IntMap<MatrixDrillBit> mineBits = new IntMap<>();
    public ObjectMap<Item, float[]> progressMap = new ObjectMap<>();
    public IntSeq armTasks = new IntSeq();
    public float armWaiting;

    public TreeSet<Integer> tempMine = new TreeSet<>();

    public boolean configuring;
    public boolean selectMode = true;

    public ItemsBuffer itemBuffer = new ItemsBuffer();
    public Rect rect;
    public Vec2 armPos = new Vec2();

    public Element lastController;
    public PutItemsRequest putReq;

    @Override
    public void edgeUpdated(){
      if(getEdges().isClosure()){
        float[] vertices = getEdges().getPoly().getTransformedVertices();
        float x = Float.MAX_VALUE, y = Float.MAX_VALUE, mx = 0, my = 0;
        for(int i = 0; i < vertices.length; i += 2){
          x = Math.min(x, vertices[i]);
          y = Math.min(y, vertices[i+1]);
          mx = Math.max(mx, vertices[i]);
          my = Math.max(my, vertices[i+1]);
        }
        rect = new Rect(x, y, mx - x, my - y);
        armPos.set(this.x, this.y);

        if(!tempMine.isEmpty()){
          for(Integer integer: tempMine){
            assignTask(Point2.x(integer), Point2.y(integer), 0, false);
          }
          tempMine.clear();
        }
      }
      else{
        rect = null;
        armPos.set(x, y);
        for(IntMap.Entry<MatrixDrillBit> bitEntry: mineBits){
          tempMine.add(bitEntry.key);
          bitEntry.value.remove();
        }
        armTasks.clear();
      }
    }

    public void oreUpdated(MatrixDrillBit matrixDrillBit){
      TMP_ITEMS.clear();
      for(MatrixDrillBit bit: mineBits.values()){
        if(bit.ore == null) continue;
        TMP_ITEMS.add(bit.ore);
      }

      for(Item key: progressMap.keys()){
        if(!TMP_ITEMS.contains(key)) progressMap.remove(key);
      }
    }

    public void assignTaskClient(int x, int y, int cmdCode, boolean concurrent){
      armTasks.add(x);
      armTasks.add(y);
      armTasks.add(cmdCode);
      armTasks.add(concurrent? 1: 0);
    }

    public void assignTask(int x, int y, int cmdCode, boolean concurrent){
      configure(new IntSeq(new int[]{x, y, cmdCode, concurrent? 1: 0}));
    }

    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      itemBuffer.capacity = itemCapacity*itemBuffer.bufferType().unit();
      items = itemBuffer.generateBindModule();
      return this;
    }

    @Override
    public void networkValided(){
      super.networkValided();
      if(putReq != null) putReq.kill();
      distributor.assign(putReq = new PutItemsRequest(this, itemBuffer));
      putReq.init(distributor.network);
    }

    @Override
    public void updateTile(){
      super.updateTile();

      for(IntMap.Entry<MatrixDrillBit> bit: mineBits){
        bit.value.update();
        float[] progRef = progressMap.get(bit.value.ore, () -> new float[1]);
        if(progRef[0] >= 1){
          bit.value.buffered++;
        }
      }

      for(float[] ref: progressMap.values()){
        if(ref[0] >= 1) ref[0] = 0;
      }

      if(updateValid()){
        for(ObjectMap.Entry<Item, float[]> entry: progressMap){
          float delta = Time.delta*consEfficiency();
          float delay = drillTime + entry.key.hardness*hardMultiple;

          entry.value[0] += 1/delay*delta;
        }

        if(putReq != null) putReq.update();
        if(armTasks.size >= 4){
          armWaiting = Time.time;

          if(handleTask(
              armTasks.items[0],
              armTasks.items[1],
              armTasks.items[2]
          )){
            for(int i = 0; i < 4; i++){
              armTasks.removeIndex(0);
            }
          }
          else{
            armPos.lerpDelta(armTasks.get(0)*tilesize, armTasks.get(1)*tilesize, armSpeed);
          }

          for(int i = 4; i < armTasks.size; i += 4){
            if(armTasks.get(i + 3) > 0){
              if(handleTask(
                  armTasks.items[i],
                  armTasks.items[i+1],
                  armTasks.items[i+2]
              )){
                for(int l = 0; l < 4; l++){
                  armTasks.removeIndex(i);
                }
                i -= 4;
              }
            }
          }
        }
        else{
          if(Time.time - armWaiting >= armIdleWait) armPos.lerpDelta(x, y, armSpeed);
        }
      }
    }

    public boolean handleTask(int x, int y, int cmdCode){
      float worldX = x*tilesize, worldY = y*tilesize;

      switch(cmdCode){
        case 0:{
          if(Math.abs(armPos.x - worldX) <= 3f && Math.abs(armPos.y - worldY) <= 3f){
            mineBits.remove(Point2.pack(x, y));
            MatrixDrillBit bit = new MatrixDrillBit();
            bit.x = x;
            bit.y = y;
            int pos = Point2.pack(x, y);
            bit.ore = getMine(pos);
            mineBits.put(pos, bit);
            progressMap.put(bit.ore, new float[1]);
            return true;
          }
          break;
        }
        case 1:{
          MatrixDrillBit bit = mineBits.get(Point2.pack(x, y));
          if(bit != null){
            bit.remove();
            oreUpdated(bit);
          }
          return true;
        }
        case 2:{
          if(Math.abs(armPos.x - worldX) <= bitPostDistance && Math.abs(armPos.y - worldY) <= bitPostDistance){
            MatrixDrillBit bit = mineBits.get(Point2.pack(x, y));
            if(bit == null) return true;
            int move = Math.min(bit.buffered, itemBuffer.remainingCapacity());
            bit.post(move);
            itemBuffer.put(bit.ore, move);
            return true;
          }
          break;
        }
        default: return false;
      }
      return false;
    }

    @Override
    public boolean updateValid(){
      return distributor.network.netValid() && rect != null;
    }

    @Override
    public boolean onConfigureBuildTapped(Building other){
      if(other == this && updateValid()){
        configuring = !configuring;
        switchController();
        return false;
      }
      else if(other instanceof EdgeLinkerBuildComp && canLink(this, (EdgeLinkerBuildComp) other)){
        configure(other.pos());
        return false;
      }

      configuring = false;
      return true;
    }

    @Override
    public void buildConfiguration(Table table){
      super.buildConfiguration(table);

      TextureRegion selectIcon = Singularity.getModAtlas("select"), deselectIcon = Singularity.getModAtlas("deselect");

      table.table(Styles.black6, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image().size(40).update(image -> image.setDrawable(selectMode ? selectIcon : deselectIcon))).size(50);
        t.table(b -> {
          b.check("", selectMode, this::configure).left();
          b.table(text -> {
            text.defaults().grow().left();
            text.add(Core.bundle.get("infos.selectMode")).color(Pal.accent);
            text.row();
            text.add("").update(l -> {
              l.setText(selectMode ? Core.bundle.get("infos.selecting"): Core.bundle.get("infos.deselecting"));
            });
          }).grow().right().padLeft(8);
        }).height(50).padLeft(8).growX();
      }).height(50).growX();
      table.row();
    }

    public Item getMine(int pos){
      Tile tile = Vars.world.tile(pos);
      Item drop;
      if(tile != null && (drop = tile.drop()) != null && tile.build == null){
        return drop;
      }
      return null;
    }

    public void switchController(){
      if(configuring && rect != null){
        Vars.ui.hudGroup.addChild(lastController = new Element(){
          final Vec2 originPos = Core.input.mouseScreen(rect.x, rect.y);
          float originXWorld, originYWorld;
          float originX, originY;
          float currX, currY;

          boolean touched;

          {
            touchablility = () -> configuring? Touchable.enabled: Touchable.disabled;

            addListener(new InputListener(){
              @Override
              public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                originX = currX = originPos.x + x;
                originY = currY = originPos.y + y;
                Tmp.v1.set(Core.camera.unproject(originX, originY));
                originXWorld = Tmp.v1.x;
                originYWorld = Tmp.v1.y;
                touched = true;
                return super.touchDown(event, x, y, pointer, button);
              }

              @Override
              public void touchDragged(InputEvent event, float x, float y, int pointer){
                Tmp.v1.set(Core.input.mouseScreen(rect.x, rect.y));
                Tmp.v2.set(Core.camera.project(rect.x + rect.width, rect.y + rect.height));
                currX = Mathf.clamp(originPos.x + x, Tmp.v1.x, Tmp.v2.x);
                currY = Mathf.clamp(originPos.y + y, Tmp.v1.y, Tmp.v2.y);
                super.touchDragged(event, x, y, pointer);
              }

              @Override
              public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                Tmp.v1.set(Core.camera.unproject(originX, originY));
                Tmp.v2.set(Core.camera.unproject(currX, currY));
                assignConfigTasks(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
                touched = false;
                super.touchUp(event, x, y, pointer, button);
              }
            });

            addCaptureListener(new ElementGestureListener(){
              @Override
              public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                originX = currX = originPos.x + x;
                originY = currY = originPos.y + y;
                Tmp.v1.set(Core.camera.unproject(originX, originY));
                originXWorld = Tmp.v1.x;
                originYWorld = Tmp.v1.y;
                touched = true;
                super.touchDown(event, x, y, pointer, button);
              }

              @Override
              public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                Tmp.v1.set(Core.input.mouseScreen(rect.x, rect.y));
                Tmp.v2.set(Core.camera.project(rect.x + rect.width, rect.y + rect.height));
                currX = Mathf.clamp(originPos.x + x, Tmp.v1.x, Tmp.v2.x);
                currY = Mathf.clamp(originPos.y + y, Tmp.v1.y, Tmp.v2.y);
                super.pan(event, x, y, deltaX, deltaY);
              }

              @Override
              public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                Tmp.v1.set(Core.camera.unproject(originX, originY));
                Tmp.v2.set(Core.camera.unproject(currX, currY));
                assignConfigTasks(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
                touched = false;
                super.touchUp(event, x, y, pointer, button);
              }
            });
          }

          @Override
          public void draw(){
            super.draw();
            Lines.stroke(6.5f, Pal.accent);
            Lines.rect(x, y, width, height);

            if(!touched) return;
            Tmp.v1.set(Core.camera.unproject(originX, originY));
            Tmp.v2.set(Core.camera.unproject(currX, currY));
            float x = (float) (Math.round(Math.min(Tmp.v1.x, Tmp.v2.x)/tilesize)*tilesize - 4), y = (float) (Math.round(Math.min(Tmp.v1.y, Tmp.v2.y)/tilesize)*tilesize - 4);
            float dx = (float) (Math.round(Math.max(Tmp.v1.x, Tmp.v2.x)/tilesize)*tilesize + 4), dy = (float) (Math.round(Math.max(Tmp.v1.y, Tmp.v2.y)/tilesize)*tilesize + 4);
            Tmp.v1.set(Core.camera.project(x, y));
            Tmp.v2.set(Core.camera.project(dx, dy)).sub(Tmp.v1);

            Draw.color(selectMode? Pal.accent: Pal.redderDust);
            Lines.rect(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
          }

          @Override
          public void act(float delta){
            super.act(delta);
            if(rect == null) return;
            Vec2 v = Core.camera.project(originXWorld, originYWorld);
            originX = v.x;
            originY = v.y;

            Tmp.v1.set(Core.input.mouseScreen(rect.x, rect.y));
            Tmp.v2.set(Core.camera.project(rect.x + rect.width, rect.y + rect.height));
            setBounds(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x - Tmp.v1.x, Tmp.v2.y - Tmp.v1.y);

            if(!configuring
                || !Vars.control.input.config.isShown()
                || Vars.control.input.config.getSelected() != MatrixMinerBuild.this) Vars.ui.hudGroup.removeChild(this);
          }
        });
      }
      else Vars.ui.hudGroup.removeChild(lastController);
    }

    public void assignConfigTasks(float x, float y, float ox, float oy){
      if(!updateValid()) return;
      int originX = Math.round(Math.min(x, ox)/tilesize);
      int originY = Math.round(Math.min(y, oy)/tilesize);
      int otherX = Math.round(Math.max(x, ox)/tilesize);
      int otherY = Math.round(Math.max(y, oy)/tilesize);

      MatrixDrillBit b;
      int pos;
      for(int posY = originY; posY <= otherY; posY++){
        for(int posX = originX; posX <= otherX; posX++){
          if(selectMode){
            if(getMine(pos = Point2.pack(posX, posY)) != null && !mineBits.containsKey(pos)) assignTask(posX, posY, 0, false);
          }
          else{
            if((b = mineBits.get(Point2.pack(posX, posY))) != null && b.working()) assignTask(posX, posY, 1, true);
          }
        }
      }
    }

    @Override
    public void drawConfigure(){
      super.drawConfigure();
      int x, y;
      int cmdCode;

      for(int i = 0; i < armTasks.size; i+=4){
        x = armTasks.items[i];
        y = armTasks.items[i + 1];
        cmdCode = armTasks.items[i + 2];


      }
    }

    @Override
    public void draw(){
      drawArm();

      for(MatrixDrillBit bit: mineBits.values()){
        bit.draw();
      }

      Draw.z(Layer.block);
    }

    @Override
    public void drawLink(){
      EdgeLinkerBuildComp.super.drawLink();
      Draw.reset();
      Draw.z(Layer.block);
    }

    public void drawArm(){
      if(rect == null) return;
      Draw.z(Layer.flyingUnit - 0.02f);
      Lines.stroke(4);
      Lines.line(
          armRegion,
          armPos.x, rect.y,
          armPos.x, rect.y + rect.height,
          true
      );
      Draw.rect(armCapRegion, armPos.x, rect.y);
      Draw.rect(armCapRegion, armPos.x, rect.y + rect.height);
      Lines.line(
          armRegion,
          rect.x, armPos.y,
          rect.x + rect.width, armPos.y,
          true
      );
      Draw.rect(armCapRegion, rect.x, armPos.y);
      Draw.rect(armCapRegion, rect.x + rect.width, armPos.y);
      Draw.rect(armBitRegion, armPos.x, armPos.y);
      Draw.reset();
      Draw.z(Layer.block);
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      armPos.set(read.f(), read.f());

      armTasks.clear();
      int taskCount = read.i();
      for(int i = 0; i < taskCount; i++){
        armTasks.add(read.i());
      }

      progressMap.clear();
      int progCount = read.i();
      for(int i = 0; i < progCount; i++){
        progressMap.get(Vars.content.item(read.i()), () -> new float[1])[0] = read.f();
      }

      mineBits.clear();
      int mineCount = read.i();
      for(int i = 0; i < mineCount; i++){
        MatrixDrillBit bit = new MatrixDrillBit();
        bit.read(read);
        mineBits.put(Point2.pack(bit.x, bit.y), bit);
      }
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.f(armPos.x);
      write.f(armPos.y);

      write.i(armTasks.size);
      for(int i = 0; i < armTasks.size; i++){
        write.i(armTasks.get(i));
      }

      write.i(progressMap.size);
      for(ObjectMap.Entry<Item, float[]> entry: progressMap){
        write.i(entry.key.id);
        write.f(entry.value[0]);
      }

      write.i(mineBits.size);
      for(MatrixDrillBit entry: mineBits.values()){
        entry.write(write);
      }
    }

    public class MatrixDrillBit{
      public int x, y;
      public float alpha;
      public float warmup;
      public float totalProgress;
      public Item ore;
      public Item lastOre;
      public int buffered;

      public boolean postedTask;
      public float timer;

      public Vec2 controlPoint = new Vec2();

      public boolean removing;

      public void update(){
        alpha = Mathf.lerpDelta(alpha, removing? 0: 1, 0.02f);
        if(alpha < 0.075f && removing) mineBits.remove(Point2.pack(x, y));

        warmup = Mathf.lerpDelta(warmup, updateValid()? 1: 0, 0.02f);
        totalProgress += warmup;

        ore = getMine(Point2.pack(x, y));
        if(ore == null){
          remove();
          return;
        }
        if(lastOre != ore) oreUpdated(this);

        if(buffered >= drillBufferCapacity*0.75f){
          if(!postedTask){
            assignTaskClient(x, y, 2, true);
            postedTask = true;
          }
        }
        else postedTask = false;
      }

      public void draw(){
        float x = this.x*tilesize, y = this.y*tilesize;
        if(warmup > 0.5f) Draw.z(Layer.bullet);
        Draw.color(ore.color);
        Draw.alpha(alpha);
        Draw.rect(drillRegion, x, y, totalProgress*rotatorSpeed);
        Draw.color(drillColor);
        Draw.alpha(alpha);

        Draw.rect(drillTop, x, y);

        if(Time.time - timer <= bitPostTime){
          Lines.stroke(3.75f*Mathf.clamp(1 - (Time.time - timer)/bitPostTime), ore.color);
          float distance = Tmp.v1.set(x, y).sub(armPos).len();
          Lines.curve(x, y, x + controlPoint.x, y + controlPoint.y, x + controlPoint.x, y + controlPoint.y, armPos.x, armPos.y, (int) (distance/2.75f));
        }
        else controlPoint.set(x, y);

        Draw.z(Layer.block);
        Draw.reset();
      }

      public void post(int amount){
        buffered -= amount;
        postedTask = false;
        timer = Time.time;
        controlPoint.set(Tmp.v1.set(x*tilesize, y*tilesize).sub(armPos).scl(-Mathf.random(0.5f, 0.8f)));
      }

      public void remove(){
        removing = true;
      }

      public boolean working(){
        return !removing;
      }

      public void read(Reads read){
        int pos = read.i();
        x = Point2.x(pos);
        y = Point2.y(pos);
        alpha = read.f();
        buffered = read.i();
        timer = Time.time - read.f();
        warmup = read.f();
        totalProgress = read.f();
        postedTask = read.bool();
        removing = read.bool();
      }

      public void write(Writes write){
        write.i(Point2.pack(x, y));
        write.f(alpha);
        write.i(buffered);
        write.f(Time.time - timer);
        write.f(warmup);
        write.f(totalProgress);
        write.bool(postedTask);
        write.bool(removing);
      }
    }
  }
}
