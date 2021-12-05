package singularity.world.draw;

import arc.graphics.Blending;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.world.Block;
import singularity.world.blocks.product.NormalCrafter;

import static mindustry.Vars.tilesize;

public class DrawExpandPlasma<T extends NormalCrafter.NormalCrafterBuild> extends SglDrawPlasma<T>{
  public float rotationSpeed = 1f;
  public float cycle = 15;
  
  public DrawExpandPlasma(Block block, int length){
    super(block, length);
  }
  
  @Override
  public void drawPlasma(T entity){
    for(int i = 0; i < plasmas.length; i++){
      float r = block.size*tilesize*(2f/3f) + ((Time.time%cycle)/cycle)*block.size*tilesize*(2f/3);
      float rd = block.size*tilesize*(2f/3f) + ((Time.time*1.72f%cycle)/cycle)*block.size*tilesize*(2f/3);
    
      Draw.color(plasma1, plasma2, (float)i / plasmas.length);
      Draw.alpha((0.3f + Mathf.absin(Time.time, 2f + i * 2f, 0.3f + i * 0.05f)) * warmup(entity));
      Draw.blend(Blending.additive);
      Draw.rect(plasmas[i], entity.x, entity.y, r, r, Time.time * (3 + i * 6f) * warmup(entity) * rotationSpeed);
      Draw.rect(plasmas[(i+1)%plasmas.length], entity.x, entity.y, rd, rd, Time.time * (3 + i * 6f) * warmup(entity) * rotationSpeed);
      Draw.blend();
    }
    Draw.color();
  }
}
