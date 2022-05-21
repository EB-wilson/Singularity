package singularity.ui.tables;

import arc.Core;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Cons3;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.scene.Element;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import singularity.world.blocks.distribute.IOPointBlock;
import singularity.world.distribution.GridChildType;
import universecore.util.DataPackable;
import universecore.util.Empties;

public class DistTargetConfigTable extends Table{
  private static final ObjectSet<Character> numbers = ObjectSet.with('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-');
 
  TargetConfigure config = new TargetConfigure();
  ContentType currType;
  byte[] currDireBit;
  ObjectSet<UnlockableContent> currConfig;
  
  Runnable rebuildItems;
  int index;
  
  public DistTargetConfigTable(Building build, TargetConfigure defaultCfg, GridChildType[] IOTypes, ContentType[] types, Cons<TargetConfigure> cons, Runnable close){
    super(Tex.pane);
    if(defaultCfg != null){
      config.read(defaultCfg.pack());
    }
    else{
      config.position = build.pos();
    }
    
    class Flip extends Element{
      float deltaX, deltaY;
      float alpha;
      boolean selected, valid = true;
  
      public Flip(){
        setSize(90);
        touchable(() -> currDireBit != null? Touchable.enabled: Touchable.disabled);
        update(() -> {
          alpha = Mathf.lerpDelta(alpha, selected? 1: 0, 0.045f);

          if(Core.app.isAndroid()){
            if(!selected || !valid){
              deltaX = Mathf.lerpDelta(deltaX, 0, 0.05f);
              deltaY = Mathf.lerpDelta(deltaY, 0, 0.05f);
            }
          }
          else{
            selected = Tmp.cr1.set(x, y, 45).contains(Core.input.mouse());
          }
        });

        addCaptureListener(new ElementGestureListener(){
          @Override
          public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
            super.touchDown(event, x, y, pointer, button);
            selected = true;
          }

          @Override
          public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
            super.touchUp(event, x, y, pointer, button);
            selected = false;
            valid = true;
          }

          @Override
          public void pan(InputEvent event, float x, float y, float dx, float dy){
            if(valid){
              deltaX += dx;
              deltaY += dy;
              if(deltaX > 90){
                setDireBit((byte) 1);
              }
              else if(deltaX < -90){
                setDireBit((byte) 4);
              }
              else if(deltaY > 90){
                setDireBit((byte) 2);
              }
              else if(deltaY < -90){
                setDireBit((byte) 8);
              }
            }
            super.pan(event, x, y, deltaX, deltaY);
          }
        });

        addListener(new InputListener(){
          @Override
          public boolean keyDown(InputEvent event, KeyCode keycode){
            if(selected){
              if(keycode.equals(KeyCode.right)){
                setDireBit((byte) 1);
              }
              else if(keycode.equals(KeyCode.up)){
                setDireBit((byte) 2);
              }
              else if(keycode.equals(KeyCode.left)){
                setDireBit((byte) 4);
              }
              else if(keycode.equals(KeyCode.down)){
                setDireBit((byte) 8);
              }
            }
            return super.keyDown(event, keycode);
          }
        });
      }
  
      @Override
      public void draw(){
        validate();
    
        Draw.color(currDireBit == null? Pal.gray: Color.lightGray);
        Draw.alpha(0.5f + 0.5f*alpha);
        Lines.stroke(4.5f);
        Lines.circle(x + deltaX + width/2f, y + deltaY + height/2f, 45);
    
        if(currDireBit != null){
          byte bit = 1;
          for(int i = 0; i < 4; i++){
            int dx = Geometry.d4x(i);
            int dy = Geometry.d4y(i);

            Draw.color((currDireBit[0] & bit) != 0 ? Pal.accent : Pal.gray);
            Draw.alpha(alpha);
            Fill.square(x + deltaX + width/2f + dx*60*alpha, y + deltaY + height/2f + dy*60*alpha, 18, 45);

            bit *= 2;
          }
        }
      }
  
      private void setDireBit(byte bit){
        if(currDireBit != null){
          currDireBit[0] ^= bit;
        }
        valid = false;
      }
    }
    
    table(topBar -> {
      topBar.image(Icon.settings).size(50).left().padLeft(4);
      topBar.add(Core.bundle.get("fragments.configs.gridConfig")).left().padLeft(4);
      topBar.button(
          t -> t.add("").update(l -> l.setText(Core.bundle.format("misc.mode", IOTypes[index].locale()))),
          Styles.clearPartialt,
          () -> {
            index = (index + 1)%IOTypes.length;
            currConfig = config.getOrNew(IOTypes[index], currType);
            rebuildItems.run();
          }
      ).width(85).padLeft(4).padRight(4).growY().left();

      topBar.add(Core.bundle.get("misc.priority")).right().padRight(4);
      topBar.field(Integer.toString(config.priority),
          (f, c) -> numbers.contains(c),
          str -> config.priority = Integer.valueOf(str)).right().width(75).padRight(4);
    }).fillY().expandX();

    row();
    image().color(Pal.gray).growX().height(4).colspan(2).pad(0).margin(0);
    row();
    table(main -> {
      main.pane(items -> {
        rebuildItems = () -> {
          currDireBit = null;
          items.clearChildren();
          Seq<UnlockableContent> itemSeq = Vars.content.getBy(currType);
          int counter = 0;
          for(UnlockableContent item: itemSeq){
            if(item.unlocked()){
              ImageButton button = items.button(Tex.whiteui, Styles.selecti, 30, () -> {
                if(!config.remove(IOTypes[index], item)){
                  config.set(IOTypes[index], item, currDireBit = new byte[1]);
                }
                else currDireBit = null;
              }).size(40).get();
              button.getStyle().imageUp = new TextureRegionDrawable(item.uiIcon);
              button.update(() -> button.setChecked(currConfig.contains(item)));
  
              if(counter++ != 0 && counter%5 == 0) items.row();
            }
          }
        };
        currType = types[0];
        currConfig = config.getOrNew(IOTypes[index], currType);
        rebuildItems.run();
      }).size(225, 160);
      
      main.image().color(Pal.gray).growY().width(4).colspan(2).padLeft(3).padRight(3).margin(0);
      
      main.table(sideBar -> {
        sideBar.pane(typesTable -> {
          for(ContentType type : types){
            typesTable.button(t -> t.add(Core.bundle.get("content." + type.name() + ".name")), Styles.underlineb, () -> {
              currConfig = config.getOrNew(IOTypes[index], type);
              currType = type;
              rebuildItems.run();
            }).growX().height(35).update(b -> b.setChecked(currType == type))
                .touchable(() -> currType == type? Touchable.disabled: Touchable.enabled);
            typesTable.row();
          }
        }).size(120, 80);
        sideBar.row();
        sideBar.button(Core.bundle.get("misc.sure"), Icon.ok, Styles.clearPartialt, () -> {
          cons.get(config);
          close.run();
        }).size(120, 40);
        sideBar.row();
        sideBar.button(Core.bundle.get("misc.reset"), Icon.cancel, Styles.clearPartialt, () -> {
          config.clear();
          cons.get(config);
          close.run();
        }).size(120, 40);
      }).fillX();
    });
    
    Element ele;
    if(build instanceof IOPointBlock.IOPoint){
      addChild(ele = new Flip());
      update(() -> {
        ele.setPosition(width/2, height + ele.getHeight(), 2);
      });
    }
  }
  
  public static class TargetConfigure implements DataPackable{
    public static final long typeID = 6253491887543618527L;
    
    static{
      DataPackable.assignType(typeID, p -> new TargetConfigure());
    }
    
    public int position;
    public int priority;
    
    protected ObjectMap<GridChildType, ObjectMap<ContentType, ObjectSet<UnlockableContent>>> data = new ObjectMap<>();
    protected ObjectMap<GridChildType, ObjectMap<UnlockableContent, byte[]>> directBits = new ObjectMap<>();
    
    public void set(GridChildType type, UnlockableContent content, byte[] dirBit){
      data.get(type, ObjectMap::new).get(content.getContentType(), ObjectSet::new).add(content);
      directBits.get(type, ObjectMap::new).put(content, dirBit);
    }
    
    public boolean remove(GridChildType type, UnlockableContent content){
      boolean result = data.get(type, Empties.nilMapO()).get(content.getContentType(), Empties.nilSetO()).remove(content);
      directBits.get(type, Empties.nilMapO()).remove(content);
      return result;
    }
    
    public boolean get(GridChildType type, UnlockableContent content){
      return data.get(type, Empties.nilMapO()).get(content.getContentType(), Empties.nilSetO()).contains(content);
    }
    
    public void each(Cons3<GridChildType, ContentType, UnlockableContent> cons){
      for(ObjectMap.Entry<GridChildType, ObjectMap<ContentType, ObjectSet<UnlockableContent>>> entry : data){
        for(ObjectMap.Entry<ContentType, ObjectSet<UnlockableContent>> contEntry : entry.value){
          for(UnlockableContent content : contEntry.value){
            cons.get(entry.key, contEntry.key, content);
          }
        }
      }
    }
    
    public void eachChildType(Cons2<GridChildType, ObjectMap<ContentType, ObjectSet<UnlockableContent>>> cons){
      for(ObjectMap.Entry<GridChildType, ObjectMap<ContentType, ObjectSet<UnlockableContent>>> entry : data){
        for(ObjectSet<UnlockableContent> value: entry.value.values()){
          if(!value.isEmpty()){
            cons.get(entry.key, entry.value);
            break;
          }
        }
      }
    }
    
    public byte[] getDirectBit(GridChildType type, UnlockableContent content){
      return directBits.get(type, Empties.nilMapO()).get(content, new byte[]{-1});
    }
    
    public boolean directValid(GridChildType type, UnlockableContent content, byte match){
      byte bit = getDirectBit(type, content)[0];
      if(bit == -1 || match == -1) return false;
      if(bit == 0) return true;
      return (bit & match) != 0;
    }
    
    public ObjectSet<UnlockableContent> getOrNew(GridChildType type, ContentType t){
      return data.get(type, ObjectMap::new).get(t, ObjectSet::new);
    }
    
    public ObjectSet<UnlockableContent> get(GridChildType type, ContentType t){
      return data.get(type, Empties.nilMapO()).get(t, Empties.nilSetO());
    }
    
    public ObjectMap<GridChildType, ObjectMap<ContentType, ObjectSet<UnlockableContent>>> get(){
      return data;
    }
    
    public boolean any(){
      for(ObjectMap<ContentType, ObjectSet<UnlockableContent>> conts :data.values()){
        for(ObjectSet<UnlockableContent> cont : conts.values()){
          if(!cont.isEmpty()) return true;
        }
      }
      return false;
    }

    public boolean isContainer(){
      for(GridChildType type : data.keys()){
        if(type == GridChildType.container){
          return true;
        }
      }
      return false;
    }

    @Override
    public long typeID(){
      return typeID;
    }
  
    @Override
    public void write(Writes write){
      write.i(position);
      write.i(priority);
      
      write.i(data.size);
      for(ObjectMap.Entry<GridChildType, ObjectMap<ContentType, ObjectSet<UnlockableContent>>> entry : data){
        write.i(entry.key.ordinal());
        write.i(entry.value.size);
        for(ObjectMap.Entry<ContentType, ObjectSet<UnlockableContent>> dataEntry : entry.value){
          write.i(dataEntry.key.ordinal());
          write.i(dataEntry.value.size);
          for(UnlockableContent v: dataEntry.value){
            write.i(v.id);
          }
        }
      }
      
      write.i(directBits.size);
      for(ObjectMap.Entry<GridChildType, ObjectMap<UnlockableContent, byte[]>> entry : directBits){
        write.i(entry.key.ordinal());
        write.i(entry.value.size);
        for(ObjectMap.Entry<UnlockableContent, byte[]> cEntry : entry.value){
          write.i(cEntry.key.getContentType().ordinal());
          write.i(cEntry.key.id);
          write.b(cEntry.value[0]);
        }
      }
    }
  
    @Override
    public void read(Reads read){
      position = read.i();
      priority = read.i();
      
      data = new ObjectMap<>();
      int count = read.i(), count2, amount;
      for(int i = 0; i < count; i++){
        ObjectMap<ContentType, ObjectSet<UnlockableContent>> map = data.get(GridChildType.values()[read.i()], ObjectMap::new);
        count2 = read.i();
        for(int l = 0; l < count2; l++){
          ContentType type = ContentType.values()[read.i()];
          ObjectSet<UnlockableContent> set = map.get(type, ObjectSet::new);
          amount = read.i();
          for(int i1 = 0; i1 < amount; i1++){
            set.add(Vars.content.getByID(type, read.i()));
          }
        }
      }
      
      directBits = new ObjectMap<>();
      int size = read.i(), length;
      for(int i = 0; i < size; i++){
        ObjectMap<UnlockableContent, byte[]> map = directBits.get(GridChildType.values()[read.i()], ObjectMap::new);
        length = read.i();
        for(int l = 0; l < length; l++){
          int typeId = read.i();
          map.get(Vars.content.getByID(ContentType.values()[typeId], read.i()), () -> new byte[]{read.b()});
        }
      }
    }
  
    public void clear(){
      priority = 0;
      data.clear();
    }

    public boolean isClear(){
      if(data.isEmpty()) return true;
      for(ObjectMap<ContentType, ObjectSet<UnlockableContent>> map: data.values()){
        for(ObjectSet<UnlockableContent> value: map.values()){
          if(!value.isEmpty()) return false;
        }
      }
      return true;
    }

    @Override
    public String toString(){
      return "TargetConfigure{" +
          "position=" + position +
          ", priority=" + priority +
          ", data=" + data +
          ", directBits=" + directBits +
          '}';
    }
  }
}
