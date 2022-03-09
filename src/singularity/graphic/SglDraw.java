package singularity.graphic;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.util.Tmp;
import mindustry.world.Tile;

import static mindustry.Vars.tilesize;

public class SglDraw{
  public static void drawLink(Tile origin, Tile other, TextureRegion linkRegion, TextureRegion capRegion, float lerp){
    drawLink(origin, origin.block() != null? origin.block().offset: 0, other, other.block() != null? other.block().offset: 0, linkRegion, capRegion, lerp);
  }
  
  public static void drawLink(Tile origin, float offsetO, Tile other, float offset, TextureRegion linkRegion, TextureRegion capRegion, float lerp){
    float ox = origin.worldx() + offsetO;
    float oy = origin.worldy() + offsetO;
    float otx = other.worldx() + offset;
    float oty = other.worldy() + offset;
    Tmp.v1.set(otx, oty).sub(ox, oy);
    Tmp.v2.set(Tmp.v1).scl(lerp).sub(Tmp.v1.setLength(origin.block().size - tilesize/2f).cpy().setLength(other.block().size - tilesize/2f));
    Lines.stroke(8);
    Lines.line(linkRegion, ox + Tmp.v1.x,
        oy + Tmp.v1.y,
        ox + Tmp.v2.x,
        oy + Tmp.v2.y,
        false);
    
    if(capRegion != null) Draw.rect(capRegion, ox + Tmp.v2.x, oy + Tmp.v2.y, Tmp.v2.angle());
  }
}
