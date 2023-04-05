package singularity.world.blocks.drills;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.OrderedSet;
import arc.struct.Sort;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.request.PutItemsRequest;

import static mindustry.Vars.tilesize;

public class MatrixMiner extends DistNetBlock{
  public static final Integer[] NIL = new Integer[0];

  public int baseRange = 0;

  public MatrixMiner(String name){
    super(name);
    configurable = true;
    hasItems = true;
    itemCapacity = 64;
    topologyUse = 2;
  }

  @Override
  public void appliedConfig(){
    config(Integer.class, (MatrixMinerBuild b, Integer i) -> {
      Item item = Vars.content.item(i);
      if(!b.drillItem.remove(item)) b.drillItem.add(item);
    });

    config(Float.class, (MatrixMinerBuild b, Float f) -> b.drillRange = f.intValue());
  }

  @Override
  public void setBars(){
    super.setBars();
    addBar("efficiency", (MatrixMinerBuild m) -> new Bar(
        () -> Core.bundle.format("bar.efficiency", Mathf.round(m.consEfficiency()*100)),
        () -> Pal.lighterOrange,
        m::consEfficiency
    ));
    addBar("energyUse", (MatrixMinerBuild m) -> new Bar(
        () -> Core.bundle.format("bar.energyCons",
            Strings.autoFixed(m.matrixEnergyConsume()*60, 1),
            Strings.autoFixed(m.energyConsMultiplier, 1)),
        () -> SglDrawConst.matrixNet,
        () -> m.matrixEnergyConsume() > 0.01f? 1: 0
    ));
  }

  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotation) {
    return Vars.indexer.findTile(team, tile.worldx(), tile.worldy(), baseRange*1.44f, b -> {
      if (Math.abs(b.tileX() - tile.x) > baseRange/2 || Math.abs(b.tileY() - tile.y) > baseRange/2) return false;

      return b.block == this;
    }) == null;
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid) {
    super.drawPlace(x, y, rotation, valid);

    float l = baseRange*tilesize/2f;
    Drawf.dashLine(Pal.accent, x - l, y - l, x - l, y + l);
    Drawf.dashLine(Pal.accent, x - l, y - l, x + l, y - l);
    Drawf.dashLine(Pal.accent, x + l, y + l, x - l, y + l);
    Drawf.dashLine(Pal.accent, x + l, y + l, x + l, y - l);
  }

  public class MatrixMinerBuild extends DistNetBuild{
    public OrderedSet<MatrixMinerPlugin.MatrixMinerPluginBuild> plugins = new OrderedSet<>();

    public ObjectSet<Item> drillItem = new ObjectSet<>();
    public OrderedSet<Item> allOre = new OrderedSet<>();

    public float energyConsMultiplier = 1;

    public int drillRange;
    public int maxRange, lastRadius;
    public float boost, drillMoveMulti;

    public int drillSize;
    public ItemsBuffer itemBuffer = DistBufferType.itemBuffer.get(itemCapacity*DistBufferType.itemBuffer.unit());

    public ObjectMap<Integer, Item> ores = new ObjectMap<>();
    public Integer[] orePosArr = NIL;

    public boolean pierce;
    public PutItemsRequest putReq;

    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      items = itemBuffer.generateBindModule();
      return this;
    }

    @Override
    public void buildConfiguration(Table table){
      super.buildConfiguration(table);

      table.table(Tex.pane, t -> {
        t.defaults().left();
        t.add(Core.bundle.get("fragment.buttons.selectMine"));
        t.row();
        t.table(items -> {
          int counter = 0;
          for(Item item: allOre){
            if(item.unlockedNow()){
              ImageButton button = items.button(Tex.whiteui, Styles.selecti, 30, () -> {
                configure((int)item.id);
              }).size(40).get();
              button.getStyle().imageUp = new TextureRegionDrawable(item.uiIcon);
              button.update(() -> button.setChecked(drillItem.contains(item)));

              if(counter++ != 0 && counter%5 == 0) items.row();
            }
          }
        });
        t.row();
        t.add("").update(l -> l.setText(Core.bundle.format("infos.drillRange", drillRange)));
        t.row();
        t.slider(0, maxRange, 1, drillRange, this::configure).growX().height(45);
      });
    }

    @Override
    public void drawConfigure(){
      super.drawConfigure();
      float l = drillRange*tilesize/2f;
      Drawf.dashLine(Pal.accent, x - l, y - l, x - l, y + l);
      Drawf.dashLine(Pal.accent, x - l, y - l, x + l, y - l);
      Drawf.dashLine(Pal.accent, x + l, y + l, x - l, y + l);
      Drawf.dashLine(Pal.accent, x + l, y + l, x + l, y - l);

      for(MatrixMinerPlugin.MatrixMinerPluginBuild plugin: plugins){
        plugin.drawConfigure();
      }
    }

    @Override
    public void updateTile(){
      super.updateTile();

      energyConsMultiplier = 1;
      boost = 1;
      drillMoveMulti = 1;
      drillSize = 0;
      maxRange = baseRange;
      pierce = false;

      itemBuffer.update();

      for(MatrixMinerPlugin.MatrixMinerPluginBuild plugin: plugins){
        if (!plugin.enabled()) continue;

        energyConsMultiplier *= plugin.energyMultiplier();
        boost *= plugin.boost();
        drillMoveMulti *= plugin.drillMoveMulti();
        maxRange += plugin.range();
        drillSize = Math.max(drillSize, plugin.drillSize());
        pierce |= plugin.pierceBuild();
      }

      if(lastRadius != maxRange){
        drillRange = Math.min(drillRange, maxRange);

        ores.clear();

        int off = (maxRange + 1)%2;
        int ox = tileX() - maxRange/2 + off;
        int oy = tileY() - maxRange/2 + off;

        for(int rx = 0; rx < maxRange; rx++){
          for(int ry = 0; ry < maxRange; ry++){
            Tile t = Vars.world.tile(ox + rx, oy + ry);
            if(t == null) continue;

            if(t.drop() != null){
              ores.put(t.pos(), t.drop());
              allOre.add(t.drop());
            }
          }
        }

        orePosArr = ores.keys().toSeq().toArray(Integer.class);
        Sort.instance().sort(orePosArr, (a, b) -> {
          int x1 = Point2.x(a) - tileX();
          int y1 = Point2.y(a) - tileY();
          int x2 = Point2.x(b) - tileX();
          int y2 = Point2.y(b) - tileY();

          float r1 = Math.max(Math.abs(x1), Math.abs(y1)), rot1 = Angles.angle(x1, y1);
          float r2 = Math.max(Math.abs(x2), Math.abs(y2)), rot2 = Angles.angle(x2, y2);

          return Float.compare(r1*1000 + rot1, r2*1000 + rot2);
        });
      }

      if(putReq != null && distributor.network.netValid()) putReq.update();
    }

    @Override
    public void networkValided(){
      super.networkValided();

      if(putReq != null) putReq.kill();
      putReq = new PutItemsRequest(this, itemBuffer);
      distributor.assign(putReq);
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();
      plugins.clear();

      for(Building building: proximity){
        if(building instanceof MatrixMinerPlugin.MatrixMinerPluginBuild b && b.interactable(team)
        && (b.tileX() == tileX() || b.tileY() == tileY())){
          plugins.add(b);
          b.setOwner(this);
        }
      }
    }

    @Override
    public boolean updateValid(){
      return itemBuffer.remainingCapacity() > 0;
    }

    public boolean angleValid(float angle){
      for(MatrixMinerPlugin.MatrixMinerPluginBuild plugin: plugins){
        if(plugin.angleValid(angle)) return true;
      }
      return false;
    }

    @Override
    public Object config() {
      return null;
    }

    @Override
    public float consEfficiency(){
      return super.consEfficiency()*distributor.network.netEfficiency();
    }

    @Override
    public float matrixEnergyConsume(){
      return super.matrixEnergyConsume()*energyConsMultiplier;
    }

    public boolean isChild(MatrixMinerPlugin.MatrixMinerPluginBuild build){
      return plugins.contains(build);
    }

    @Override
    public void draw(){
      super.draw();
      Draw.z(Layer.effect);
      Lines.stroke(1.6f*consEfficiency()*boost, SglDrawConst.matrixNet);
      SglDraw.dashCircle(x, y, 12, 6, 180, Time.time);
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.i(drillRange);
      write.i(drillItem.size);
      for(Item item: drillItem){
        write.i(item.id);
      }
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      drillRange = read.i();
      int l = read.i();
      for(int i = 0; i < l; i++){
        drillItem.add(Vars.content.item(read.i()));
      }
    }
  }
}
