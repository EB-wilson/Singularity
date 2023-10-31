package singularity.ui;

import arc.Core;
import arc.freetype.FreeTypeFontGenerator;
import arc.graphics.Color;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Lines;
import arc.scene.style.BaseDrawable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Dialog;
import arc.scene.ui.Slider;
import arc.util.Tmp;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import singularity.Singularity;
import singularity.graphic.SglDrawConst;
import singularity.ui.fragments.HealthBarStyle;
import universecore.ui.elements.markdown.Markdown;
import universecore.util.handler.ObjectHandler;

import javax.sound.sampled.Line;

import static mindustry.gen.Tex.*;
import static singularity.ui.SglUI.uiBlur;

public class SglStyles{
  //来自IDEA Community的等宽字体，谢谢你，JB
  public static Font jetBrainsMono;

  public static TextureRegionDrawable BLUR_BACK;

  public static Slider.SliderStyle sliderLine;
  public static Button.ButtonStyle underline;
  public static Dialog.DialogStyle blurBack, transparentBack;
  public static Markdown.MarkdownStyle defaultMD;

  public static void load(){
    HealthBarStyle.loadAll();

    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Singularity.getInternalFile("fonts").child("JetBrainsMono.ttf"));
    jetBrainsMono = generator.generateFont(new FreeTypeFontGenerator.FreeTypeFontParameter(){{
      size = 16;
      shadowColor = Color.darkGray;
      shadowOffsetY = 2;
      incremental = true;
    }}, new FreeTypeFontGenerator.FreeTypeFontData(){
      @Override
      public Font.Glyph getGlyph(char ch) {
        Font.Glyph res = super.getGlyph(ch);
        if (res != missingGlyph) return res;

        res = Fonts.def.getData().getGlyph(ch);
        Font.Glyph copy = new Font.Glyph();

        ObjectHandler.copyField(res, copy);
        res = copy;
        res.page += 1;
        res.yoffset -= 4;

        return res;
      }
    });
    jetBrainsMono.getRegions().addAll(Fonts.def.getRegions());

    BLUR_BACK = new TextureRegionDrawable(Core.atlas.white()) {
      @Override
      public void draw(float x, float y, float width, float height) {
        uiBlur.directDraw(() -> super.draw(x, y, width, height));

        Styles.black5.draw(x, y, width, height);
      }

      @Override
      public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        uiBlur.directDraw(() -> super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation));

        Styles.black5.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
      }
    };

    sliderLine = new Slider.SliderStyle(){{
      background = Core.atlas.drawable("singularity-slider_line_back");
      knob = sliderKnob;
      knobOver = sliderKnobOver;
      knobDown = sliderKnobDown;
    }};
    
    underline = new Button.ButtonStyle(){{
      up = Tex.underline;
      down = underlineWhite;
      over = underlineOver;
    }};

    blurBack = new Dialog.DialogStyle(){{
      stageBackground = BLUR_BACK;
      titleFont = Fonts.def;
      background = windowEmpty;
      titleFontColor = Pal.accent;
    }};

    transparentBack = new Dialog.DialogStyle(){{
      stageBackground = SglDrawConst.transparent;
      titleFont = Fonts.outline;
      background = SglDrawConst.transparent;
      titleFontColor = Pal.accent;
    }};

    defaultMD = new Markdown.MarkdownStyle(){{
      font = subFont = Fonts.def;
      codeFont = jetBrainsMono;
      FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Core.files.internal("fonts/font.woff"));
      strongFont = gen.generateFont(new FreeTypeFontGenerator.FreeTypeFontParameter(){{
        size = 19;
        borderWidth = 0.3f;
        shadowOffsetY = 2;
        incremental = true;
        borderColor = color;
      }});
      emFont = Fonts.def;

      textColor = Color.white;
      emColor = Pal.accent;
      subTextColor = Color.lightGray;
      lineColor = Color.gray;
      linkColor = Pal.place;

      linesPadding = 5;
      maxCodeBoxHeight = 400;
      tablePadHor = 14;
      tablePadVert = 10;
      paragraphPadding = 14;

      board = paneLeft;
      codeBack = ((TextureRegionDrawable) SglDrawConst.grayUI).tint(Tmp.c1.set(Pal.darkerGray).a(0.7f));
      codeBack.setLeftWidth(4);
      codeBack.setRightWidth(4);
      codeBlockBack = SglDrawConst.grayUI;
      codeBlockStyle = Styles.smallPane;

      tableBack1 = ((TextureRegionDrawable) whiteui).tint(Tmp.c1.set(Pal.darkerGray).a(0.7f));
      tableBack2 = ((TextureRegionDrawable) whiteui).tint(Tmp.c1.set(Color.gray).a(0.7f));

      curtain = ((TextureRegionDrawable) whiteui).tint(Pal.darkerGray);

      listMarks = new Drawable[]{
          new BaseDrawable(){
            @Override
            public void draw(float x, float y, float width, float height) {
              Fill.square(x + width/2, y + height/2, width*0.25f, 45f);
            }
          },
          new BaseDrawable(){
            @Override
            public void draw(float x, float y, float width, float height) {
              Fill.circle(x + width/2, y + height/2, width*0.3f);
            }
          },
          new BaseDrawable(){
            @Override
            public void draw(float x, float y, float width, float height) {
              Lines.stroke(1);
              Lines.circle(x + width/2, y + height/2, width*0.3f);
            }
          }
      };
    }};
  }
}
