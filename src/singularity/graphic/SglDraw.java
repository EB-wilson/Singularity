package singularity.graphic;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.util.Tmp;
import mindustry.world.Tile;

import static mindustry.Vars.tilesize;

public class SglDraw{
  public static void drawLink(Tile origin, Tile other, TextureRegion linkRegion, TextureRegion capRegion, float lerp){
    Tmp.v1.set(other.drawx(), other.drawy()).sub(origin.drawx(), origin.drawy());
    Tmp.v2.set(Tmp.v1).scl(lerp).sub(Tmp.v1.setLength(origin.block().size - tilesize/2f).cpy().setLength(other.block().size - tilesize/2f));
    Lines.stroke(8);
    Lines.line(linkRegion, origin.drawx() + Tmp.v1.x,
        origin.drawy() + Tmp.v1.y,
        origin.drawx() + Tmp.v2.x,
        origin.drawy() + Tmp.v2.y,
        false);
    
    if(capRegion != null) Draw.rect(capRegion, origin.drawx() + Tmp.v2.x, origin.drawy() + Tmp.v2.y, Tmp.v2.angle());
  }
}
