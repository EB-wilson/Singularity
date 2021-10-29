package singularity.world.draw;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.type.Liquid;
import mindustry.world.Block;
import singularity.world.blocks.nuclear.NuclearReactor;
import singularity.world.consumers.SglConsumeType;

import static mindustry.Vars.tilesize;

public class DrawNuclearReactor extends DrawFactory<NuclearReactor.NuclearReactorBuild>{
  public TextureRegion light;
  public Color hotColor = Color.valueOf("ff9575a3");
  public Color coolColor = new Color(1, 1, 1, 0f);
  public float flashThreshold = 580f;
  
  public DrawNuclearReactor(Block block){
    super(block);
  }
  
  @Override
  public void load(){
    super.load();
    light = Core.atlas.find(block.name + "_light");
  }
  
  @Override
  public TextureRegion[] icons(){
    return new TextureRegion[]{
        bottom,
        region
    };
  }
  
  public class DrawNuclearReactorDrawer extends DrawFactoryDrawer{
    public DrawNuclearReactorDrawer(NuclearReactor.NuclearReactorBuild entity){
      super(entity);
    }
  
    @Override
    public void draw(){
      Draw.rect(bottom, entity.x, entity.y);
      Draw.rect(region, entity.x, entity.y);
      
      Draw.color(coolColor, hotColor, entity.temperature()/entity.block().maxTemperature);
      Fill.rect(entity.x, entity.y, entity.block.size * tilesize, entity.block.size * tilesize);
      
      Liquid liquid = entity.consumer.optionalCurr != null? entity.consumer.optionalCurr.get(SglConsumeType.liquid).liquids[0].liquid:
          entity.consumer.getOptional(0).get(SglConsumeType.liquid).liquids[0].liquid;
      
      Draw.color(liquid.color);
      Draw.alpha(entity.liquids.get(liquid)/entity.block.liquidCapacity);
      Draw.rect(top, entity.x, entity.y);

      if(entity.temperature() > flashThreshold){
        float flash = 1f + ((entity.temperature() - flashThreshold) / (entity.block().maxTemperature - flashThreshold)) * 5.4f;
        flash += flash * Time.delta;
        Draw.color(Color.red, Color.yellow, Mathf.absin(flash, 9f, 1f));
        Draw.alpha(0.6f);
        Draw.rect(light, entity.x, entity.y);
      }
  
      Draw.reset();
    }
  
    @Override
    public void drawLight(){
      super.drawLight();
    }
  }
}
