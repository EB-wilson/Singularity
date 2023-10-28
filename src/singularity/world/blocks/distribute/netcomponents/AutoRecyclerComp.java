package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.func.Cons;
import arc.func.Cons2;
import arc.math.geom.Point2;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Teamc;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Styles;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.Payload;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.blocks.distribute.TargetConfigure;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import universecore.util.Empties;

import java.util.HashMap;

public class AutoRecyclerComp extends DistNetBlock {
  public OrderedMap<DistBufferType<?>, Cons<Building>> usableRecycle = new OrderedMap<>();

  public AutoRecyclerComp(String name){
    super(name);
    configurable = true;

    config(IntSeq.class, (AutoRecyclerCompBuild b, IntSeq c) -> {
      ContentType type = ContentType.values()[c.get(0)];
      UnlockableContent content = Vars.content.getByID(type, c.get(1));
      ObjectSet<UnlockableContent> set = b.list.get(DistBufferType.typeOf(type), ObjectSet::new);
      if(!set.add(content)) set.remove(content);

      b.flush = true;
    });
    config(Integer.class, (AutoRecyclerCompBuild b, Integer i) -> {
      if (i == -1){
        b.list.clear();
      }
      else if(i == 1){
        b.isBlackList = !b.isBlackList;
      }
      b.flush = true;
    });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public <E extends Building> void setRecycle(DistBufferType<?> type, Cons<E> recycle){
    usableRecycle.put(type, (Cons)recycle);
  }

  public class AutoRecyclerCompBuild extends DistNetBuild{
    ObjectMap<DistBufferType<?>, ObjectSet<UnlockableContent>> list = new ObjectMap<>();
    boolean isBlackList = true;
    boolean flush;

    public void updateConfig(){
      if (distributor.network.netStructValid()) {
        TargetConfigure config = new TargetConfigure();

        config.priority = -65536;

        Tile coreTile = distributor.network.getCore().getTile();
        int dx = tile.x - coreTile.x;
        int dy = tile.y - coreTile.y;
        config.offsetPos = Point2.pack(dx, dy);

        if (!isBlackList) {
          for (DistBufferType<?> type : usableRecycle.orderedKeys()) {
            for (Content content : Vars.content.getBy(type.targetType())) {
              if (content instanceof UnlockableContent c && !list.get(type, Empties.nilSetO()).contains(c)) {
                config.set(GridChildType.container, c, new byte[]{-1});
              }
            }
          }
        }
        else {
          for (ObjectSet<UnlockableContent> counts : list.values()) {
            for (UnlockableContent content: counts) {
              config.set(GridChildType.container, content, new byte[]{-1});
            }
          }
        }

        distributor.network.getCore().matrixGrid().remove(this);
        distributor.network.getCore().matrixGrid().addConfig(config);
      }
    }

    @Override
    public void updateTile(){
      super.updateTile();

      if (flush){
        updateConfig();

        flush = false;
      }

      for (Cons<Building> rec : usableRecycle.values()) {
        rec.get(this);
      }
    }

    @Override
    public void networkValided() {
      flush = true;
    }

    Runnable rebuildItems;
    DistBufferType<?> currType;
    @Override
    public void buildConfiguration(Table table){
      table.table(Tex.pane, main -> {
        main.pane(items -> {
          rebuildItems = () -> {
            items.clearChildren();
            Seq<UnlockableContent> itemSeq = Vars.content.getBy(currType.targetType());
            int counter = 0;
            for(UnlockableContent item: itemSeq){
              if(item.unlockedNow()){
                ImageButton button = items.button(Tex.whiteui, Styles.selecti, 30,
                    () -> configure(IntSeq.with(item.getContentType().ordinal(), item.id))).size(40).get();
                button.getStyle().imageUp = new TextureRegionDrawable(item.uiIcon);
                button.update(() -> button.setChecked(list.get(currType, Empties.nilSetO()).contains(item)));

                button.addListener(new Tooltip(t -> t.table(Tex.paneLeft).get().add(item.localizedName)));

                if(counter++ != 0 && counter%5 == 0) items.row();
              }
            }
          };
          currType = usableRecycle.orderedKeys().get(0);
          rebuildItems.run();
        }).size(225, 160);

        main.image().color(Pal.gray).growY().width(4).colspan(2).padLeft(3).padRight(3).margin(0);

        main.table(sideBar -> {
          sideBar.pane(typesTable -> {
            for(DistBufferType<?> type: usableRecycle.orderedKeys()){
              typesTable.button(t -> t.add(Core.bundle.get("content." + type.targetType().name() + ".name")), Styles.underlineb, () -> {
                    currType = type;
                    rebuildItems.run();
                  }).growX().height(35).update(b -> b.setChecked(currType == type))
                  .touchable(() -> currType == type? Touchable.disabled: Touchable.enabled);
              typesTable.row();
            }
          }).size(120, 100);
          sideBar.row();
          sideBar.check("", isBlackList, b -> configure(1)).update(c -> c.setText(Core.bundle.get(isBlackList? "misc.blackListMode": "misc.whiteListMode"))).size(120, 40);
          sideBar.row();
          sideBar.button(Core.bundle.get("misc.reset"), Icon.cancel, Styles.cleart, () -> configure(-1)).size(120, 40);
        }).fillX();
      }).fill();
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.bool(isBlackList);

      write.i(list.size);
      for(ObjectMap.Entry<DistBufferType<?>, ObjectSet<UnlockableContent>> entry: list){
        write.i(entry.key.id);
        write.i(entry.value.size);
        for(UnlockableContent content: entry.value){
          write.i(content.getContentType().ordinal());
          write.i(content.id);
        }
      }
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      isBlackList = read.bool();

      list.clear();
      int size = read.i();
      for(int i = 0; i < size; i++){
        ObjectSet<UnlockableContent> set = list.get(DistBufferType.all[read.i()], ObjectSet::new);
        int s = read.i();
        for(int l = 0; l < s; l++){
          set.add(Vars.content.getByID(ContentType.all[read.i()], read.i()));
        }
      }
    }
  }
}
