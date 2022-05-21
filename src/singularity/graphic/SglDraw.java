package singularity.graphic;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Rect;
import arc.util.Nullable;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.graphics.Layer;
import mindustry.world.Tile;

public class SglDraw{
  private static boolean blooming;
  private static final Rect rect = new Rect();

  static {
    Events.run(EventType.Trigger.drawOver, () -> {
      if(Vars.renderer.bloom != null){
        Draw.draw(Layer.block + 0.02f, () -> {
          Vars.renderer.bloom.capture();
          Vars.renderer.bloom.capturePause();
        });
        Draw.draw(Layer.blockOver - 0.02f, () -> Vars.renderer.bloom.render());
      }
    });
  }

  public static boolean clipDrawable(float x, float y, float clipSize){
    Core.camera.bounds(rect);
    return rect.overlaps(x - clipSize/2, y - clipSize/2, clipSize, clipSize);
  }

  public static void drawLink(Tile origin, Tile other, TextureRegion linkRegion, TextureRegion capRegion, float lerp){
    drawLink(origin, origin.block() != null? origin.block().offset: 0, other, other.block() != null? other.block().offset: 0, linkRegion, capRegion, lerp);
  }
  
  public static void drawLink(Tile origin, float offsetO, Tile other, float offset, TextureRegion linkRegion, @Nullable TextureRegion capRegion, float lerp){
    Tmp.v1.set(other.drawx() - origin.drawx(), other.drawy() - origin.drawy()).setLength(offsetO);
    float ox = origin.drawx() + Tmp.v1.x;
    float oy = origin.drawy() + Tmp.v1.y;
    Tmp.v1.scl(-1).setLength(offset);
    float otx = other.drawx() + Tmp.v1.x;
    float oty = other.drawy() + Tmp.v1.y;

    Tmp.v1.set(otx, oty).sub(ox, oy);
    Tmp.v2.set(Tmp.v1).scl(lerp);
    Tmp.v3.set(0, 0);
    
    if(capRegion != null){
      Tmp.v3.set(Tmp.v1).setLength(capRegion.width/4f);
      Draw.rect(capRegion, ox + Tmp.v3.x/2, oy + Tmp.v3.y/2, Tmp.v2.angle());
      Draw.rect(capRegion, ox + Tmp.v2.x - Tmp.v3.x/2, oy + Tmp.v2.y - Tmp.v3.y/2, Tmp.v2.angle() + 180);
    }

    Lines.stroke(8);
    Lines.line(linkRegion,
        ox + Tmp.v3.x, oy + Tmp.v3.y,
        ox + Tmp.v2.x - Tmp.v3.x,
        oy + Tmp.v2.y - Tmp.v3.y,
        false);
  }

  public static void drawLightEdge(float x, float y, float width, float widthH, float height, float heightW, float rotation){
    drawLightEdge(x, y, width, widthH, 1, height, heightW, 1, rotation);
  }

  public static void drawLightEdge(float x, float y, float width, float widthH, float alphaW, float height, float heightW, float alphaH, float rotation){
    float heightR = height/2, widthR = width/2;

    Tmp.v1.set(1, 0).setLength(widthR).y = widthH/2;
    Tmp.v1.rotate(rotation);
    Tmp.v2.set(0, 1).setLength(heightR).x = heightW/2;
    Tmp.v2.rotate(rotation);

    Color c = Draw.getColor();
    Tmp.c1.set(c).a *= alphaW;
    Tmp.c2.set(c).a *= alphaH;

    Fill.quad(
        x, y + Tmp.v1.y, c.toFloatBits(),
        x, y - Tmp.v1.y, c.toFloatBits(),
        x + Tmp.v1.x, y, Tmp.c1.toFloatBits(),
        x + Tmp.v1.x, y, Tmp.c1.toFloatBits()
    );
    Fill.quad(
        x, y + Tmp.v1.y, c.toFloatBits(),
        x, y - Tmp.v1.y, c.toFloatBits(),
        x - Tmp.v1.x, y, Tmp.c1.toFloatBits(),
        x - Tmp.v1.x, y, Tmp.c1.toFloatBits()
    );
    Fill.quad(
        x + Tmp.v2.x, y, c.toFloatBits(),
        x - Tmp.v2.x, y, c.toFloatBits(),
        x, y + Tmp.v2.y, Tmp.c2.toFloatBits(),
        x, y + Tmp.v2.y, Tmp.c2.toFloatBits()
    );
    Fill.quad(
        x + Tmp.v2.x, y, c.toFloatBits(),
        x - Tmp.v2.x, y, c.toFloatBits(),
        x, y - Tmp.v2.y, Tmp.c2.toFloatBits(),
        x, y - Tmp.v2.y, Tmp.c2.toFloatBits()
    );
  }

  public static void gradientCircle(float x, float y, float radius, Color gradientColor){
    gradientCircle(x, y, radius, x, y, gradientColor);
  }

  public static void gradientCircle(float x, float y, float radius, float offset, Color gradientColor){
    gradientCircle(x, y, radius, x, y, offset, gradientColor);
  }

  public static void gradientCircle(float x, float y, float radius, float gradientCenterX, float gradientCenterY, Color gradientColor){
    gradientCircle(x, y, radius, gradientCenterX, gradientCenterY, -radius, gradientColor);
  }

  public static void gradientCircle(float x, float y, float radius, float gradientCenterX, float gradientCenterY, float offset, Color gradientColor){
    gradientPoly(x, y, Lines.circleVertices(radius), radius, Draw.getColor(), gradientCenterX, gradientCenterY, offset, gradientColor, 0);
  }

  public static void gradientSqrt(float x, float y, float radius, float rotation, float offset, Color gradientColor){
    gradientSqrt(x, y, radius, x, y, offset, gradientColor, rotation);
  }

  public static void gradientSqrt(float x, float y, float radius, float gradientCenterX, float gradientCenterY, float offset, Color gradientColor, float rotation){
    gradientPoly(x, y, 4, 1.41421f*(radius/2), Draw.getColor(), gradientCenterX, gradientCenterY, offset, gradientColor, rotation);
  }

  public static void gradientPoly(float x, float y, int edges, float radius, Color color, float gradientCenterX, float gradientCenterY, float offset, Color gradientColor, float rotation){
    Tmp.v1.set(gradientCenterX - x, gradientCenterY - y).rotate(rotation);
    gradientCenterX = x + Tmp.v1.x;
    gradientCenterY = y + Tmp.v1.y;

    Tmp.v1.set(1, 0).setLength(radius).rotate(rotation);
    float step = 360f/edges;

    float lastX = -1, lastY = -1;
    float lastGX = -1, lastGY = -1;

    for(int i = 0; i <= edges; i++){
      if(i == edges) Tmp.v1.setAngle(rotation);
      Tmp.v2.set(Tmp.v1).sub(gradientCenterX - x, gradientCenterY - y);

      if(lastX != -1){
        Tmp.v3.set(Tmp.v2).setLength(offset).scl(offset < 0? -1: 1);
        Tmp.v4.set(lastGX, lastGY).setLength(offset).scl(offset < 0? -1: 1);
        Fill.quad(
            lastX, lastY, color.toFloatBits(),
            x + Tmp.v1.x, y + Tmp.v1.y, color.toFloatBits(),
            gradientCenterX + Tmp.v2.x + Tmp.v3.x, gradientCenterY + Tmp.v2.y + Tmp.v3.y, gradientColor.toFloatBits(),
            gradientCenterX + lastGX + Tmp.v4.x, gradientCenterY + lastGY + Tmp.v4.y, gradientColor.toFloatBits()
        );
      }

      lastX = x + Tmp.v1.x;
      lastY = y + Tmp.v1.y;
      lastGX = Tmp.v2.x;
      lastGY = Tmp.v2.y;
      Tmp.v1.rotate(step);
    }
  }

  public static void startBloom(float z){
    if(z < Layer.block + 0.02f || z > Layer.blockOver - 0.02f) throw new IllegalArgumentException("z");
    if(blooming) throw new IllegalStateException("current is blooming, please endBloom");
    blooming = true;
    Draw.z(z);
    if(Vars.renderer.bloom != null) Draw.draw(z, () -> Vars.renderer.bloom.captureContinue());
  }

  public static void endBloom(){
    if(!blooming) throw new IllegalStateException("current is not blooming, please statBloom");
    blooming = false;
    if(Vars.renderer.bloom != null) Draw.draw(Draw.z(), () -> Vars.renderer.bloom.capturePause());
    Draw.z(Layer.blockOver);
  }

  public static void dashCircle(float x, float y, float radius){
    dashCircle(x, y, radius, 0);
  }

  public static void dashCircle(float x, float y, float radius, float rotate){
    dashCircle(x, y, radius, 0.6f, rotate);
  }

  public static void dashCircle(float x, float y, float radius, float scaleFactor, float rotate){
    int sides = 10 + (int)(radius * scaleFactor);
    if(sides % 2 == 1) sides++;

    Tmp.v1.set(0, 0);

    for(int i = 0; i < sides; i++){
      if(i % 2 == 0) continue;
      Tmp.v1.set(radius, 0).setAngle(rotate + 360f / sides * i + 90);
      float x1 = Tmp.v1.x;
      float y1 = Tmp.v1.y;

      Tmp.v1.set(radius, 0).setAngle(rotate + 360f / sides * (i + 1) + 90);

      Lines.line(x1 + x, y1 + y, Tmp.v1.x + x, Tmp.v1.y + y);
    }
  }

  public static void curve(){
  }
}
