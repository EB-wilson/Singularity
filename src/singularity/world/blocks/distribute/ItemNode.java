package singularity.world.blocks.distribute;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.scene.actions.Actions;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.core.Renderer;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.TargetPriority;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.input.Placement;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.blocks.SglBlock;
import singularity.world.distribution.GridChildType;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.Takeable;
import universecore.util.DataPackable;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class ItemNode extends SglBlock {
  public static final byte[] EMP = new byte[0];
  private static BuildPlan otherReq;

  public final int timerCheckMoved = timers ++;

  public int range;
  public float transportTime = 2f;
  public TextureRegion endRegion;
  public TextureRegion bridgeRegion;
  public TextureRegion arrowRegion;

  public boolean fadeIn = true;
  public boolean pulse = false;
  public float arrowSpacing = 4f, arrowOffset = 2f, arrowPeriod = 0.4f;
  public float arrowTimeScl = 6.2f;
  public ItemNodeBuild lastBuild;
  public int maxItemCapacity = 40;
  public boolean siphon = false;

  public ItemNode(String name) {
    super(name);
    update = true;
    solid = true;
    underBullets = true;
    hasPower = true;
    conductivePower = false;
    itemCapacity = 10;
    outputItems = true;
    configurable = true;
    hasItems = true;
    unloadable = false;
    allowConfigInventory = false;
    group = BlockGroup.transportation;
    noUpdateDisabled = true;
    copyConfig = false;
    priority = TargetPriority.transport;

    config(byte[].class, (ItemNodeBuild e, byte[] b) -> {
      Object conf = DataPackable.readObject(b);
      if (conf instanceof TargetConfigure c){
        e.config = c.isClear()? null: c;
        e.link = c.offsetPos > 0? Point2.unpack(c.offsetPos).add(e.tileX(), e.tileY()).pack(): e.link;
      }
    });

    config(Integer.class, (ItemNodeBuild tile, Integer i) -> tile.link = i);
  }

  @Override
  public void onPlanRotate(BuildPlan plan, int direction) {
    if (plan.config instanceof byte[] data && DataPackable.readObject(data) instanceof TargetConfigure nodeConfig){
      nodeConfig.rotateDir(this, direction);

      plan.config = nodeConfig.pack();
    }
  }

  @Override
  public void drawPlanConfigTop(BuildPlan plan, Eachable<BuildPlan> list){
    otherReq = null;
    list.each(other -> {
      if(other.block == this && plan != other && plan.config instanceof Point2 p && p.equals(other.x - plan.x, other.y - plan.y)){
        otherReq = other;
      }
    });

    if(otherReq != null){
      drawBridge(plan, otherReq.drawx(), otherReq.drawy(), 0);
    }
  }

  @Override
  public void setStats() {
    super.setStats();
    stats.remove(Stat.itemCapacity);
    stats.add(Stat.linkRange, range, StatUnit.blocks);
    stats.add(Stat.itemCapacity, Core.bundle.format("infos.mixedItemCapacity", itemCapacity, maxItemCapacity));
    stats.add(Stat.itemsMoved, 60/transportTime, StatUnit.itemsSecond);
  }

  @Override
  public void load() {
    super.load();
    endRegion = Core.atlas.find(name + "_end");
    bridgeRegion = Core.atlas.find(name + "_bridge");
    arrowRegion = Core.atlas.find(name + "_arrow");
  }

  public void drawBridge(BuildPlan req, float ox, float oy, float flip){
    if(Mathf.zero(Renderer.bridgeOpacity)) return;
    Draw.alpha(Renderer.bridgeOpacity);

    Lines.stroke(8f);

    Tmp.v1.set(ox, oy).sub(req.drawx(), req.drawy()).setLength(tilesize/2f);

    Lines.line(
        bridgeRegion,
        req.drawx() + Tmp.v1.x,
        req.drawy() + Tmp.v1.y,
        ox - Tmp.v1.x,
        oy - Tmp.v1.y, false
    );

    Draw.rect(arrowRegion, (req.drawx() + ox) / 2f, (req.drawy() + oy) / 2f,
        Angles.angle(req.drawx(), req.drawy(), ox, oy) + flip);

    Draw.reset();
  }

  @Override
  public void setBars() {
    super.setBars();
    removeBar("items");
    addBar("items", entity -> new Bar(
        () -> Core.bundle.format("bar.items", entity.items.total()),
        () -> Pal.items,
        () -> (float)entity.items.total() / maxItemCapacity)
    );
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    super.drawPlace(x, y, rotation, valid);

    Tile link = findLink(x, y);

    for(int i = 0; i < 4; i++){
      Drawf.dashLine(Pal.placing,
          x * tilesize + Geometry.d4[i].x * (tilesize / 2f + 2),
          y * tilesize + Geometry.d4[i].y * (tilesize / 2f + 2),
          x * tilesize + Geometry.d4[i].x * (range) * tilesize,
          y * tilesize + Geometry.d4[i].y * (range) * tilesize);
    }

    Draw.reset();
    Draw.color(Pal.placing);
    Lines.stroke(1f);
    if(link != null && Math.abs(link.x - x) + Math.abs(link.y - y) > 1){
      int rot = link.absoluteRelativeTo(x, y);
      float w = (link.x == x ? tilesize : Math.abs(link.x - x) * tilesize - tilesize);
      float h = (link.y == y ? tilesize : Math.abs(link.y - y) * tilesize - tilesize);
      Lines.rect((x + link.x) / 2f * tilesize - w / 2f, (y + link.y) / 2f * tilesize - h / 2f, w, h);

      Draw.rect("bridge-arrow", link.x * tilesize + Geometry.d4(rot).x * tilesize, link.y * tilesize + Geometry.d4(rot).y * tilesize, link.absoluteRelativeTo(x, y) * 90);
    }
    Draw.reset();
  }

  public boolean linkValid(Tile tile, Tile other){
    return linkValid(tile, other, true);
  }

  public boolean linkValid(Tile tile, Tile other, boolean checkDouble){
    if(other == null || tile == null || !positionsValid(tile.x, tile.y, other.x, other.y)) return false;

    return ((other.block() == tile.block() && tile.block() == this) || (!(tile.block() instanceof ItemNode) && other.block() == this))
        && (other.team() == tile.team() || tile.block() != this)
        && (!checkDouble || ((ItemNodeBuild)other.build).link != tile.pos());
  }

  public boolean positionsValid(int x1, int y1, int x2, int y2){
    if(x1 == x2){
      return Math.abs(y1 - y2) <= range;
    }else if(y1 == y2){
      return Math.abs(x1 - x2) <= range;
    }else{
      return false;
    }
  }

  public Tile findLink(int x, int y){
    Tile tile = world.tile(x, y);
    if(tile != null && lastBuild != null && linkValid(tile, lastBuild.tile) && lastBuild.tile != tile && lastBuild.link == -1){
      return lastBuild.tile;
    }
    return null;
  }

  @Override
  public void init(){
    super.init();
    updateClipRadius((range + 0.5f) * tilesize);
  }

  @Override
  public void handlePlacementLine(Seq<BuildPlan> plans){
    for(int i = 0; i < plans.size - 1; i++){
      var cur = plans.get(i);
      var next = plans.get(i + 1);
      if(positionsValid(cur.x, cur.y, next.x, next.y)){
        cur.config = new Point2(next.x - cur.x, next.y - cur.y);
      }
    }
  }

  @Override
  public void changePlacementPath(Seq<Point2> points, int rotation){
    Placement.calculateNodes(points, this, rotation, (point, other) -> Math.max(Math.abs(point.x - other.x), Math.abs(point.y - other.y)) <= range);
  }

  @Annotations.ImplEntries
  public class ItemNodeBuild extends SglBuilding implements Takeable {
    public TargetConfigure config;
    public int link = -1;
    public IntSeq incoming = new IntSeq(false, 4);
    public float warmup;
    public float time = -8f, timeSpeed;
    public boolean wasMoved, moved;
    public float transportCounter;

    int itemTakeCursor;
    Runnable show = () -> {}, close = () -> {};
    boolean showing;

    @Override
    public void pickedUp(){
      link = -1;
    }

    @Override
    public void playerPlaced(Object config){
      super.playerPlaced(config);

      Tile link = findLink(tile.x, tile.y);
      if(linkValid(tile, link) && this.link != link.pos() && !proximity.contains(link.build)){
        link.build.configure(tile.pos());
      }

      lastBuild = this;
    }

    @Override
    public void drawSelect(){
      if(linkValid(tile, world.tile(link))){
        drawInput(world.tile(link));
      }

      incoming.each(pos -> drawInput(world.tile(pos)));

      Draw.reset();
    }

    private void drawInput(Tile other){
      if(!linkValid(tile, other, false)) return;
      boolean linked = other.pos() == link;

      Tmp.v2.trns(tile.angleTo(other), 2f);
      float tx = tile.drawx(), ty = tile.drawy();
      float ox = other.drawx(), oy = other.drawy();
      float alpha = Math.abs((linked ? 100 : 0)-(Time.time * 2f) % 100f) / 100f;
      float x = Mathf.lerp(ox, tx, alpha);
      float y = Mathf.lerp(oy, ty, alpha);

      Tile otherLink = linked ? other : tile;
      int rel = (linked ? tile : other).absoluteRelativeTo(otherLink.x, otherLink.y);

      //draw "background"
      Draw.color(Pal.gray);
      Lines.stroke(2.5f);
      Lines.square(ox, oy, 2f, 45f);
      Lines.stroke(2.5f);
      Lines.line(tx + Tmp.v2.x, ty + Tmp.v2.y, ox - Tmp.v2.x, oy - Tmp.v2.y);

      //draw foreground colors
      Draw.color(linked ? Pal.place : Pal.accent);
      Lines.stroke(1f);
      Lines.line(tx + Tmp.v2.x, ty + Tmp.v2.y, ox - Tmp.v2.x, oy - Tmp.v2.y);

      Lines.square(ox, oy, 2f, 45f);
      Draw.mixcol(Draw.getColor(), 1f);
      Draw.color();
      Draw.rect(arrowRegion, x, y, rel * 90);
      Draw.mixcol();
    }

    @Override
    public void drawConfigure(){
      Drawf.select(x, y, tile.block().size * tilesize / 2f + 2f, Pal.accent);

      for(int i = 1; i <= range; i++){
        for(int j = 0; j < 4; j++){
          Tile other = tile.nearby(Geometry.d4[j].x * i, Geometry.d4[j].y * i);
          if(linkValid(tile, other)){
            boolean linked = other.pos() == link;

            Drawf.select(other.drawx(), other.drawy(),
                other.block().size * tilesize / 2f + 2f + (linked ? 0f : Mathf.absin(Time.time, 4f, 1f)), linked ? Pal.place : Pal.breakInvalid);
          }
        }
      }
    }

    @Override
    public boolean onConfigureBuildTapped(Building other){
      if (other == this){
        if (!showing){
          show.run();
          showing = true;
        }
        else {
          close.run();
          showing = false;
        }
        return false;
      }

      //reverse connection
      if(other instanceof ItemNodeBuild b && b.link == pos()){
        configure(other.pos());
        other.configure(-1);
        return true;
      }

      if(linkValid(tile, other.tile)){
        if(link == other.pos()){
          configure(-1);
        }else{
          configure(other.pos());
        }
        return false;
      }
      return true;
    }

    public void checkIncoming(){
      int idx = 0;
      while(idx < incoming.size){
        int i = incoming.items[idx];
        Tile other = world.tile(i);
        if(!linkValid(tile, other, false) || ((ItemNodeBuild)other.build).link != tile.pos()){
          incoming.removeIndex(idx);
          idx --;
        }
        idx ++;
      }
    }

    public void updateTransport(Building other){
      transportCounter += consEfficiency()*delta();
      while(transportCounter >= transportTime){
        Seq<Item> items = Vars.content.items();
        boolean any = false;
        for (int i = 0; transportCounter >= transportTime && i < items.size; i++) {
          int id = itemTakeCursor = (itemTakeCursor + 1)%items.size;
          if (this.items.get(id) <= 0) continue;

          Item item = items.get(id);
          if (other.acceptItem(this, item)){
            this.items.remove(item, 1);
            other.handleItem(this, item);
            transportCounter -= transportTime;
            moved = true;
            any = true;
          }
        }
        if (!any) transportCounter %= transportTime;
      }
    }

    @Override
    public void draw(){
      super.draw();

      Draw.z(Layer.power);

      Tile other = world.tile(link);
      if(!linkValid(tile, other)) return;

      if(Mathf.zero(Renderer.bridgeOpacity)) return;

      int i = relativeTo(other.x, other.y);

      if(pulse){
        Draw.color(Color.white, Color.black, Mathf.absin(Time.time, 6f, 0.07f));
      }

      float warmup = hasPower ? this.warmup : 1f;

      Draw.alpha((fadeIn ? Math.max(warmup, 0.25f) : 1f) * Renderer.bridgeOpacity);

      Draw.rect(endRegion, x, y, i * 90 + 90);
      Draw.rect(endRegion, other.drawx(), other.drawy(), i * 90 + 270);

      Lines.stroke(8f);

      Tmp.v1.set(x, y).sub(other.worldx(), other.worldy()).setLength(tilesize/2f).scl(-1f);

      Lines.line(bridgeRegion,
          x + Tmp.v1.x,
          y + Tmp.v1.y,
          other.worldx() - Tmp.v1.x,
          other.worldy() - Tmp.v1.y, false);

      int dist = Math.max(Math.abs(other.x - tile.x), Math.abs(other.y - tile.y)) - 1;

      Draw.color();

      int arrows = (int)(dist * tilesize / arrowSpacing), dx = Geometry.d4x(i), dy = Geometry.d4y(i);

      for(int a = 0; a < arrows; a++){
        Draw.alpha(Mathf.absin(a - time / arrowTimeScl, arrowPeriod, 1f) * warmup * Renderer.bridgeOpacity);
        Draw.rect(arrowRegion,
            x + dx * (tilesize / 2f + a * arrowSpacing + arrowOffset),
            y + dy * (tilesize / 2f + a * arrowSpacing + arrowOffset),
            i * 90f);
      }

      Draw.reset();
    }

    @Override
    public boolean canDumpLiquid(Building to, Liquid liquid){
      return checkDump(to);
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return
          hasLiquids && team == source.team &&
              (liquids.current() == liquid || liquids.get(liquids.current()) < 0.2f) &&
              checkAccept(source, world.tile(link));
    }

    protected boolean checkAccept(Building source, Tile other){
      if(tile == null || linked(source)) return true;

      if(linkValid(tile, other)){
        int rel = relativeTo(other);
        int rel2 = relativeTo(Edges.getFacingEdge(source, this));

        return rel != rel2;
      }

      return false;
    }

    protected boolean linked(Building source){
      return source instanceof ItemNodeBuild && linkValid(source.tile, tile) && ((ItemNodeBuild)source).link == pos();
    }

    @Override
    public boolean canDump(Building to, Item item){
      return checkDump(to);
    }

    protected boolean checkDump(Building to){
      Tile other = world.tile(link);
      if(!linkValid(tile, other)){
        Tile edge = Edges.getFacingEdge(to.tile, tile);
        int i = relativeTo(edge.x, edge.y);

        for(int j = 0; j < incoming.size; j++){
          int v = incoming.items[j];
          if(relativeTo(Point2.x(v), Point2.y(v)) == i){
            return false;
          }
        }
        return true;
      }

      int rel = relativeTo(other.x, other.y);
      int rel2 = relativeTo(to.tileX(), to.tileY());

      return rel != rel2;
    }

    @Override
    public boolean shouldConsume(){
      return linkValid(tile, world.tile(link)) && enabled;
    }

    @Override
    public byte version(){
      return 1;
    }

    @Override
    public void buildConfiguration(Table table) {
      showing = false;
      table.table(t -> {
        t.visible = false;
        t.setOrigin(Align.center);
        t.center().add(new DistTargetConfigTable(
            0,
            config,
            siphon ?
                new GridChildType[]{GridChildType.output, GridChildType.acceptor, GridChildType.input} :
                new GridChildType[]{GridChildType.output, GridChildType.acceptor},
            new ContentType[]{ContentType.item},
            true,
            c -> {
              c.offsetPos = 0;
              configure(c.pack());
            },
            () -> Vars.control.input.config.hideConfig()
        )).fill().center();

        show = () -> {
          t.visible = true;
          t.pack();
          t.setTransform(true);
          t.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
              Actions.scaleTo(1f, 1f, 0.07f, Interp.pow3Out));
        };

        close = () -> {
          t.actions(Actions.scaleTo(1f, 1f), Actions.scaleTo(0f, 1f, 0.07f, Interp.pow3Out), Actions.visible(false));
        };
      }).fillY();
    }

    @Override
    public void updateTile(){
      if(timer(timerCheckMoved, 30f)){
        wasMoved = moved;
        moved = false;
      }

      //smooth out animation, so it doesn't stop/start immediately
      timeSpeed = Mathf.approachDelta(timeSpeed, wasMoved ? 1f : 0f, 1f / 60f);

      time += timeSpeed * delta();

      checkIncoming();

      if (config != null && config.priority > 0){
        doDump();
        if (siphon) doSiphon();
      }

      Tile other = world.tile(link);
      if(!linkValid(tile, other)){
        warmup = 0f;
      }else{
        IntSeq inc = ((ItemNodeBuild)other.build).incoming;
        int pos = tile.pos();
        if(!inc.contains(pos)){
          inc.add(pos);
        }

        warmup = Mathf.approachDelta(warmup, efficiency, 1f / 30f);
        updateTransport(other.build);
      }

      if ((config != null || !linkValid(tile, other)) && (config == null || config.priority <= 0)) {
        if(siphon) doSiphon();
        doDump();
      }
    }

    public void doSiphon() {
      if(config == null) return;
      for(UnlockableContent con : config.get(GridChildType.input, ContentType.item)) {
        Item item = (Item) con;
        Building other = getNext("siphonItem",
            e -> e.interactable(team)
                && e.block.hasItems
                && e.items.has(item)
                && config.directValid(GridChildType.input, item, getDirectBit(e)));

        if (other == null || !(hasItems && items.get(item) < itemCapacity && items.total() < maxItemCapacity)) return;
        other.removeStack(item, 1);
        handleItem(other, item);
      }
    }

    public void doDump() {
      if (config == null || config.isClear()){
        dumpAccumulate();
      }
      else {
        for (UnlockableContent content : config.get(GridChildType.output, ContentType.item)) {
          Item i = (Item) content;
          if (items.get(i) <= 0) continue;

          Building next = getNext("items",
              e -> e.interactable(team) && config.directValid(GridChildType.output, i, getDirectBit(e)) &&  e.acceptItem(this, i));
          if (next == null) continue;

          items.remove(i, 1);
          next.handleItem(this, i);
        }
      }
    }

    @Override
    public boolean acceptItem(Building source, Item item){
      return hasItems && team == source.team && items.get(item) < itemCapacity && items.total() < maxItemCapacity && checkAccept(source, world.tile(link), item);
    }

    protected boolean checkAccept(Building source, Tile other, UnlockableContent content) {
      if(tile == null || linked(source) || config == null) return true;

      if(linkValid(tile, other)){
        return config.directValid(GridChildType.acceptor, content, getDirectBit(source));
      }

      return false;
    }

    protected byte getDirectBit(Building e) {
      byte dir = relativeTo(Edges.getFacingEdge(e, this));
      return (byte) (dir == 0? 1: dir == 1? 2: dir == 2? 4: dir == 3? 8: 0);
    }

    @Override
    public Object config() {
      TargetConfigure res = config == null? new TargetConfigure(): config.clone();
      res.offsetPos = Point2.unpack(link).sub(tile.x, tile.y).pack();
      return res.pack();
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.i(link);
      write.f(warmup);
      write.i(itemTakeCursor);
      write.b(incoming.size);

      for(int i = 0; i < incoming.size; i++){
        write.i(incoming.items[i]);
      }

      write.bool(wasMoved || moved);
      byte[] b = config == null? EMP : config.pack();
      write.i(b.length);
      if (b.length > 0) write.b(b);
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      link = read.i();
      warmup = read.f();
      itemTakeCursor = read.i();
      byte links = read.b();
      for(int i = 0; i < links; i++){
        incoming.add(read.i());
      }

      wasMoved = moved = read.bool();

      int len = read.i();
      if (len == 0) return;
      config = new TargetConfigure();
      config.read(read.b(len));
    }
  }
}
