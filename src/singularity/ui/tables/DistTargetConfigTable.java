package singularity.ui.tables;

import arc.Core;
import arc.func.Cons;
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
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import singularity.world.blocks.distribute.TargetConfigure;
import singularity.world.distribution.GridChildType;

public class DistTargetConfigTable extends Table{
  private static final ObjectSet<Character> numbers = ObjectSet.with('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-');
 
  TargetConfigure config = new TargetConfigure();
  ContentType currType;
  byte[] currDireBit;
  ObjectSet<UnlockableContent> currConfig;
  
  Runnable rebuildItems;
  int index;
  
  public DistTargetConfigTable(int positionOffset, TargetConfigure defaultCfg, GridChildType[] IOTypes,
                               ContentType[] types, boolean flip, Cons<TargetConfigure> cons, Runnable close){
    super(Tex.pane);
    if(defaultCfg != null){
      config.read(defaultCfg.pack());
    }
    else{
      config.offsetPos = positionOffset;
    }
    
    class Flip extends Element{
      float deltaX, deltaY;
      float alpha;
      float pressTime;
      boolean pressing, valid = true, hovering, hold;
  
      public Flip(){
        setSize(90);
        touchable(() -> currDireBit != null? Touchable.enabled: Touchable.disabled);
        update(() -> {
          alpha = Mathf.lerpDelta(alpha, pressing || hovering || hold? 1: 0, 0.045f);

          if(!pressing || !valid){
            deltaX = Mathf.lerpDelta(deltaX, 0, 0.05f);
            deltaY = Mathf.lerpDelta(deltaY, 0, 0.05f);
          }
        });

        if(Core.app.isDesktop() || Core.settings.getBool("keyboard")) {
          hovered(() -> {
            hovering = true;
          });

          exited(() -> {
            hovering = false;
          });
        }

        addCaptureListener(new ElementGestureListener(){
          @Override
          public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
            super.touchDown(event, x, y, pointer, button);
            pressing = true;
            pressTime = Time.globalTime;
            valid = true;
          }

          @Override
          public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
            super.touchUp(event, x, y, pointer, button);
            if(Time.globalTime - pressTime <= 20){
              hold = !hold && valid;
            }
            pressing = false;
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
      }
  
      @Override
      public void draw(){
        validate();

        Draw.scl(scaleX, scaleY);

        Draw.color(currDireBit == null? Pal.gray: Color.lightGray);
        Draw.alpha(0.5f + 0.5f*alpha);
        Lines.stroke(4.5f);
        Lines.circle(x + deltaX + width/2f, y + deltaY + height/2f, 45);
    
        if(currDireBit != null){
          byte bit = 1;
          for(int i = 0; i < 4; i++){
            int dx = Geometry.d4x(i);
            int dy = Geometry.d4y(i);

            Draw.color(Pal.gray);
            Draw.alpha(alpha);
            Fill.square(x + deltaX + width/2f + dx*72*alpha, y + deltaY + height/2f + dy*72*alpha, 24, 45);
            if((currDireBit[0] & bit) != 0){
              Draw.color(Pal.accent);
              Draw.alpha(alpha);
              Fill.square(x + deltaX + width/2f + dx*72*alpha, y + deltaY + height/2f + dy*72*alpha, 20, 45);
            }

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
      topBar.add(Core.bundle.get("fragments.configs.nodeConfig")).left().padLeft(4);
      topBar.button(
          t -> t.add("").update(l -> l.setText(Core.bundle.format("misc.mode", IOTypes[index].locale()))),
          Styles.cleart,
          () -> {
            index = (index + 1)%IOTypes.length;
            currConfig = config.getOrNew(IOTypes[index], currType);
            rebuildItems.run();
          }
      ).width(85).padLeft(4).padRight(4).growY().left().touchable(IOTypes.length > 1? Touchable.enabled: Touchable.disabled);

      topBar.add(Core.bundle.get("misc.priority")).right().padRight(4);
      topBar.field(Integer.toString(config.priority),
          (f, c) -> numbers.contains(c),
          str ->{
            try {
              config.priority = str.isEmpty() ? 0 : Integer.parseInt(str);
            }
            catch (NumberFormatException ignored){
              config.priority = 0;
            }
          }).right().width(75).padRight(4);
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
            if(item.unlockedNow()){
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
        sideBar.button(Core.bundle.get("misc.sure"), Icon.ok, Styles.cleart, () -> {
          cons.get(config);
          close.run();
        }).size(120, 40);
        sideBar.row();
        sideBar.button(Core.bundle.get("misc.reset"), Icon.cancel, Styles.cleart, () -> {
          config.clear();
          cons.get(config);
          close.run();
        }).size(120, 40);
      }).fillX();
    });
    
    Element ele;
    if(flip){
      addChild(ele = new Flip());
      update(() -> {
        ele.setPosition(width/2, height + ele.getHeight(), 2);
      });
    }
  }

}
