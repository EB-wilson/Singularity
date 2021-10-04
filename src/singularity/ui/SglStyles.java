package singularity.ui;

import arc.Core;
import arc.scene.ui.Button;
import arc.scene.ui.Slider;
import mindustry.gen.Tex;

import static mindustry.gen.Tex.*;

public class SglStyles{
  public static Slider.SliderStyle sliderLine;
  public static Button.ButtonStyle underline;
  
  public static void load(){
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
  }
}
