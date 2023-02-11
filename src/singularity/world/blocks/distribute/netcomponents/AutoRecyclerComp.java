package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import universecore.util.Empties;

public class AutoRecyclerComp extends NetPluginComp{
  public AutoRecyclerComp(String name){
    super(name);
    configurable = true;
  }

  public class AutoRecyclerCompBuild extends NetPluginCompBuild{
    ObjectMap<DistBufferType<?>, ObjectSet<UnlockableContent>> list = new ObjectMap<>();
    boolean isBlackList = true;

    ClearBuffRequest currTask;

    @Override
    public void onPluginValided(){
      if(currTask != null) currTask.kill();
      distributor.network.getCore().distCore().receive(currTask = new ClearBuffRequest(this));
      currTask.init(distributor.network);
    }

    @Override
    public void onPluginInvalided(){
      if(currTask != null) currTask.kill();
    }

    @Override
    public void onPluginRemoved(){
      if(currTask != null) currTask.kill();
    }

    @Override
    public void updateTile(){
      super.updateTile();
      if(currTask != null) currTask.update();
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
                ImageButton button = items.button(Tex.whiteui, Styles.selecti, 30, () -> {
                  ObjectSet<UnlockableContent> set = list.get(currType, ObjectSet::new);
                  if(!set.add(item)) set.remove(item);
                }).size(40).get();
                button.getStyle().imageUp = new TextureRegionDrawable(item.uiIcon);
                button.update(() -> button.setChecked(list.get(currType, Empties.nilSetO()).contains(item)));

                if(counter++ != 0 && counter%5 == 0) items.row();
              }
            }
          };
          currType = DistBufferType.all[0];
          rebuildItems.run();
        }).size(225, 160);

        main.image().color(Pal.gray).growY().width(4).colspan(2).padLeft(3).padRight(3).margin(0);

        main.table(sideBar -> {
          sideBar.pane(typesTable -> {
            for(DistBufferType<?> type: DistBufferType.all){
              typesTable.button(t -> t.add(Core.bundle.get("content." + type.targetType().name() + ".name")), Styles.underlineb, () -> {
                    currType = type;
                    rebuildItems.run();
                  }).growX().height(35).update(b -> b.setChecked(currType == type))
                  .touchable(() -> currType == type? Touchable.disabled: Touchable.enabled);
              typesTable.row();
            }
          }).size(120, 100);
          sideBar.row();
          sideBar.check("", isBlackList, b -> {
            isBlackList = b;
          }).update(c -> c.setText(Core.bundle.get(isBlackList? "misc.blackListMode": "misc.whiteListMode"))).size(120, 40);
          sideBar.row();
          sideBar.button(Core.bundle.get("misc.reset"), Icon.cancel, Styles.cleart, () -> {
            list.clear();
          }).size(120, 40);
        }).fillX();
      }).fill();
    }

    public class ClearBuffRequest extends DistRequestBase{
      public ClearBuffRequest(DistElementBuildComp sender){
        super(sender);
      }

      @Override
      protected boolean preHandleTask(){
        return true;
      }

      @Override
      protected boolean handleTask(){
        return true;
      }

      @Override
      protected boolean afterHandleTask(){
        if(isBlackList){
          for(ObjectMap.Entry<DistBufferType<?>, BaseBuffer<?, ?, ?>> entry: target.getCore().distCore().buffers){
            for(UnlockableContent content: list.get(entry.key, Empties.nilSetO())){
              entry.value.remove(content.id);
            }
          }
        }
        else{
          for(ObjectMap.Entry<DistBufferType<?>, BaseBuffer<?, ?, ?>> entry: target.getCore().distCore().buffers){
            ObjectSet<UnlockableContent> writeList = list.get(entry.key, Empties.nilSetO());
            for(BaseBuffer.Packet<?, ?> packet: entry.value){
              if(!writeList.contains(packet.get())){
                entry.value.remove(packet.id());
              }
            }
          }
        }

        return true;
      }
    }

    @Override
    public byte version() {
      return 1;
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
      if (revision == 1){
        isBlackList = read.bool();
      }

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
