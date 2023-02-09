package singularity.graphic;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.Shader;
import arc.math.Mat;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.math.geom.Vec3;
import arc.util.Log;
import singularity.Sgl;

public class MathRenderer{
  public static MathShader sinShader;

  public static MathShader ovalShader;

  static float dispersion = 0.02f;
  static float minThreshold = 0.01f;
  static float maxThreshold = 1;
  static TextureRegion blank;

  static class MathShader extends Shader{
    private static final String vert = """
        uniform mat4 u_projTrans;
        
        attribute vec4 a_position;
        attribute vec2 a_texCoord0;
        attribute vec4 a_color;
        
        varying vec4 v_color;
        varying vec2 v_texCoords;
        
        void main(){
            gl_Position = u_projTrans * a_position;
            v_texCoords = a_texCoord0;
            v_color = a_color;
        }
        """;

    private static final String frag = """
        #define HIGHP
                
        uniform sampler2D u_texture;
        uniform float dispersion;
                
        uniform float maxThreshold;
        uniform float minThreshold;
                
        %args%
                
        varying vec4 v_color;
        varying vec2 v_texCoords;
                
        void main(){
            float x = (v_texCoords.x - 0.5)*2.0*%wScl%;
            float y = (v_texCoords.y - 0.5)*2.0*%hScl%;
                
            vec4 c = texture2D(u_texture, v_texCoords);
            vec4 mixed = v_color*c;
                
            float gradMod = %gradMod%;
            float alpha = dispersion*gradMod/(abs(%fx%) + dispersion*gradMod);
                
            alpha = max(min((alpha - minThreshold)/(maxThreshold - minThreshold), 1.0), 0.0)*mixed.a;
                
            gl_FragColor = vec4(mixed.r, mixed.g, mixed.b, alpha);
        }
        """;

    public final Object[] argsCount;
    public final String function;

    public MathShader(String function, String gradMod, String... argTypes){
      this(2, 2, function, gradMod, argTypes);
    }

    public MathShader(float widthScl, float heightScl, String function, String gradMod, String... argTypes){
      super(vert, genFrag(widthScl, heightScl, function, gradMod, argTypes));
      this.function = function;
      argsCount = new Object[argTypes.length];
    }

    public static String genFrag(float widthScl, float heightScl, String function, String gradMod, String... argTypes){
      String res = frag.replace("%fx%", function)
          .replace("%gradMod%", gradMod)
          .replace("%args%", genArgList(argTypes))
          .replace("%wScl%", Float.toString(widthScl))
          .replace("%hScl%", Float.toString(heightScl));

      if(Sgl.config.loadInfo && Sgl.config.debugMode){
        Log.info("[DEBUG] [Singularity] math shader generate, frag shader content:\n" + res);
      }

      return res;
    }

    private static String genArgList(String... argsCount){
      StringBuilder res = new StringBuilder();
      for(int i = 0; i < argsCount.length; i++){
        res.append("uniform ")
            .append(argsCount[i])
            .append(" arg")
            .append(i)
            .append(";")
            .append(System.lineSeparator());
      }

      return res.toString();
    }

    public void setArg(int index, Object value){
      argsCount[index] = value;
    }

    @Override
    public void apply(){
      super.apply();

      bind();
      setUniformf("dispersion", dispersion);

      setUniformf("minThreshold", minThreshold);
      setUniformf("maxThreshold", maxThreshold);

      for(int i = 0; i < argsCount.length; i++){
        Object o = argsCount[i];
        if(o instanceof Float f){
          setUniformf("arg" + i, f);
        }
        else if(o instanceof Integer in){
          setUniformi("arg" + i, in);
        }
        else if(o instanceof Vec2 v){
          setUniformf("arg" + i, v);
        }
        else if(o instanceof Vec3 v){
          setUniformf("arg" + i, v);
        }
        else if(o instanceof Mat m){
          setUniformMatrix("arg" + i, m);
        }
        else throw new IllegalArgumentException("invalid type: " + o.getClass());
      }
    }
  }

  private static TextureRegion getBlank(){
    if(blank == null){
      Pixmap pix = new Pixmap(1024, 1024);
      pix.fill(Color.white);
      blank = new TextureRegion(new Texture(pix));
    }

    return blank;
  }

  public static void load(){
    sinShader = new MathShader(1, 2,
        "y - sin(x*arg0 + arg1)",
        "sqrt(1.0 + pow(arg0*cos(x*arg0 + arg1), 2.0))",
        "float", "float"
    );

    ovalShader = new MathShader(
        "x*x*arg0 + y*y*arg1 - arg2",
        "sqrt(4.0*pow(arg0*x, 2.0) + 4.0*pow(arg1*y, 2.0))",
        "float", "float", "float"
    );
  }

  public static void setDispersion(float dispersion){
    MathRenderer.dispersion = dispersion;
  }

  public static void setThreshold(float minThreshold, float maxThreshold){
    MathRenderer.minThreshold = minThreshold;
    MathRenderer.maxThreshold = maxThreshold;
  }

  public static void drawSin(float x1, float y1, float x2, float y2, float max, float omiga, float fine){
    sinShader.setArg(0, omiga*Mathf.degRad);
    sinShader.setArg(1, fine*Mathf.degRad);

    Draw.shader(sinShader);
    Lines.stroke(max*4);
    Lines.line(getBlank(), x1, y1, x2, y2, false);
    Draw.shader();
  }

  public static void drawCos(float x1, float y1, float x2, float y2, float max, float omiga, float fine){
    drawSin(x1, y1, x2, y2, max, omiga, fine + 90);
  }

  public static void drawCircle(float x, float y, float radius){
    ovalShader.setArg(0, 1);
    ovalShader.setArg(1, 1);
    ovalShader.setArg(2, 1);

    Draw.shader(ovalShader);
    float r = radius*4;
    Draw.rect(getBlank(), x, y, r, r);
    Draw.shader();
  }

  //其实好像直接缩放就好，但线条宽度会受到影响，所以还是实现了椭圆的函数绘制工具
  public static void drawOval(float x, float y, float horizon, float vert, float rotation){
    ovalShader.setArg(0, Mathf.pow(1/horizon, 2));
    ovalShader.setArg(1, Mathf.pow(1/vert, 2));
    ovalShader.setArg(2, Mathf.pow(Math.min(1/horizon, 1/vert), 2));

    Draw.shader(ovalShader);
    float r = Math.max(horizon, vert)*4;
    Draw.rect(getBlank(), x, y, r, r, rotation);
    Draw.shader();
  }
}
