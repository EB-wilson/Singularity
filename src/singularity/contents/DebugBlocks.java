package singularity.contents;

import arc.Core;
import arc.audio.Sound;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Tex;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import singularity.graphic.SglDraw;
import singularity.type.SglCategory;
import singularity.ui.SglStyles;
import singularity.world.blocks.TestBlock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class DebugBlocks implements ContentList{
  public static Block drawTest, volTest;

  @Override
  public void load(){
    drawTest = new TestBlock("draw_test"){{
      requirements(SglCategory.debugging, ItemStack.with());

      draw = new DrawBlock(){
        @Override
        public void draw(Building build){
          super.draw(build);

          for(int i = 0; i < 12; i++){
            SglDraw.drawRectAsCylindrical(
                build.x, build.y + i*4 + Mathf.randomSeed(build.id + i, -6, 6),
                Mathf.randomSeed(build.id + 1 + i, 18), Mathf.randomSeed(build.id + 2 + i, 8),
                10 + i + Mathf.randomSeed(build.id + 3 + i, -5, 5),
                Time.time + Mathf.randomSeed(build.id + 4 + i, 360),
                0, Pal.reactorPurple, Pal.reactorPurple2, Draw.z(), Layer.effect
            );
          }
        }
      };
    }};

    volTest = new TestBlock("vol_test"){{
      requirements(SglCategory.debugging, ItemStack.with());
      configurable = true;

      buildType = () -> new TestBlockBuild(){
        Sound curr;
        float volume;
        float pitch;
        boolean continuation;

        @Override
        public void update() {
          super.update();
          if (curr != null && continuation) Vars.control.sound.loop(curr, this, volume);
        }

        @Override
        public void buildConfiguration(Table table){
          table.table(Styles.black6, t -> {
            t.defaults().pad(0).margin(0);
            t.table(Tex.buttonTrans, i -> i.image().size(35)).size(40);
            t.button(b -> {
              b.table(text -> {
                text.defaults().grow().left();
                text.add(Core.bundle.get("misc.currentMode")).color(Pal.accent);
                text.row();
                text.add("").update(l -> {
                  l.setText(continuation? "continuation": "none");
                });
              }).grow().right().padLeft(8);
            }, Styles.cleart, () -> continuation = !continuation).size(194, 40).padLeft(8);
          }).size(250, 40);
          table.row();

          table.pane(sound -> {
            for (Field field : Sounds.class.getDeclaredFields()) {
              if (Modifier.isStatic(field.getModifiers()) && field.getType() == Sound.class) {
                try {
                  Sound s = (Sound) field.get(null);
                  sound.button(t -> t.add(field.getName()).left().grow(), Styles.underlineb, () -> {
                    continuation = false;
                    curr = s;
                    curr.at(x, y, pitch, volume);
                  }).update(b -> b.setChecked(curr == s)).size(250, 35).pad(0);
                  sound.row();
                } catch (IllegalAccessException e) {
                  throw new RuntimeException(e);
                }
              }
            }
          }).fillX().height(460);

          table.row();
          table.table(t -> {
            t.slider(0, 10, 0.1f, volume, f -> volume = f).size(200, 50).padLeft(8).padRight(8).get().setStyle(SglStyles.sliderLine);
            t.add("0").size(50).update(lable -> lable.setText(Strings.autoFixed(volume, 1)));
          });
          table.table(t -> {
            t.slider(0f, 2, 0.05f, pitch, f -> pitch = f).size(200, 50).padLeft(8).padRight(8).get().setStyle(SglStyles.sliderLine);
            t.add("0").size(50).update(lable -> lable.setText(Strings.autoFixed(pitch, 2)));
          });
        }
      };
    }};
  }
}
