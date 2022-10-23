package singularity.world.draw.part;

import arc.graphics.g2d.Draw;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.entities.part.DrawPart;

public class CustomPart extends DrawPart{
  public Seq<Drawer> drawers = new Seq<>();
  public Seq<PartHandler> offsets = new Seq<>();
  public Seq<PartMove> moves = new Seq<>();

  public float layer;

  public float x, y, rotation;

  @Override
  public void draw(PartParams params){
    float mx = 0, my = 0, mr = 0;

    if(moves.size > 0){
      for(int l = 0; l < moves.size; l++){
        var move = moves.get(l);
        float p = move.progress.getClamp(params);
        mx += move.x * p;
        my += move.y * p;
        mr += move.rot * p;
      }
    }

    float z = Draw.z();
    for(int i = 0; i < drawers.size; i++){
      PartHandler hand = offsets.get(i);
      float progress = hand.progress.get(params);

      Draw.z(layer + hand.layerOffset);

      float rot = rotation + params.rotation + hand.rot*progress + mr;
      Tmp.v1.set(
          x + hand.x*progress + mx,
          y + hand.y*progress + my
      ).rotate(rot);

      drawers.get(i).draw(params.x + Tmp.v1.x, params.y + Tmp.v1.y, rot, progress);
    }
    Draw.z(z);
  }

  public void set(PartHandler mover, Drawer drawer){
    offsets.add(mover);
    drawers.add(drawer);
  }

  @Override
  public void load(String name){}

  public interface Drawer{
    void draw(float x, float y, float rotation, float progress);
  }

  public static class PartHandler{
    public PartProgress progress = p -> 1;
    public float x, y, rot, layerOffset;

    public PartHandler(PartProgress progress, float x, float y, float rot, float layerOffset){
      this.progress = progress;
      this.x = x;
      this.y = y;
      this.rot = rot;
      this.layerOffset = layerOffset;
    }

    public PartHandler(){
    }
  }
}
