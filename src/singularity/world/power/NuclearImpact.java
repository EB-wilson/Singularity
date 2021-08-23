package singularity.world.power;

import singularity.world.blocks.product.NormalCrafter;
import singularity.world.consumers.SglConsumeType;
import singularity.world.meta.SglBlockStatus;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.type.ItemStack;
import universeCore.util.UncLiquidStack;
import universeCore.world.consumers.UncConsumeItems;
import universeCore.world.consumers.UncConsumeLiquids;

import static mindustry.Vars.tilesize;

/**普通核反应堆，发电量和燃料总量有关，堆温的控制应引用consume可选配方的method调用SubTrigger*/
public class NuclearImpact extends BaseGenerator{
  public float heat;
  public TextureRegion lightsRegion;
  public float flashThreshold = 0.46f;
  public float smokeThreshold = 0.3f;
  
  public NuclearImpact(NormalCrafter.NormalCrafterBuild entity){
    super(entity);
  }
  
  @Override
  public void update() {
    if(entity.consValid()){
      heat = Mathf.lerpDelta(heat, 1, entity.productionEfficiency / 50);
    }
    else{
      entity.productionEfficiency = 0;
    }
    
    if(heat > smokeThreshold){
      float smoke = 1.0f + (heat - smokeThreshold) / (1f - smokeThreshold); //ranges from 1.0 to 2.0
      if(Mathf.chance(smoke / 20.0 * entity.delta())){
        Fx.reactorsmoke.at(entity.x + Mathf.range(entity.block().size * tilesize / 2f),
        entity.y + Mathf.range(entity.block().size * tilesize / 2f));
      }
    }
    
    if(heat >= 1f){
      if(entity.productionEfficiency < 0.5){
        entity.enabled = false;
        entity.status = SglBlockStatus.broken;
        entity.reset();
        entity.recipeCurrent = -1;
      }
      else if(entity.productionEfficiency < 0.8){
        /*TODO 你还没写损坏*/
      }
      else{
        Events.fire(EventType.Trigger.thoriumReactorOverheat);
        entity.kill();
        /*TODO 还没写爆炸*/
      }
    }
  }
  
  @Override
  public void trigger() {
    entity.productionEfficiency = 0f;
    UncConsumeItems ci = entity.consumer.current.get(SglConsumeType.item);
    UncConsumeLiquids cl = entity.consumer.current.get(SglConsumeType.liquid);
    if(ci != null) for(ItemStack stack: ci.items){
      entity.productionEfficiency += (float)entity.items.get(stack.item) / entity.block().itemCapacity;
    }
    if(cl != null) for(UncLiquidStack stack: cl.liquids){
      entity.productionEfficiency += entity.liquids.get(stack.liquid) / entity.block().liquidCapacity;
    }
    entity.productionEfficiency /= ci != null? ci.items.length: 0f + (cl != null? cl.liquids.length: 0f);
  }
  
  @Override
  public void draw(){
    if(heat > flashThreshold){
      float flash = 1f + ((heat - flashThreshold) / (1f - flashThreshold)) * 5.4f;
      flash += flash * Time.delta;
      Draw.color(Color.red, Color.yellow, Mathf.absin(flash, 9f, 1f));
      Draw.alpha(0.6f);
      Draw.rect(lightsRegion, entity.x, entity.y);
    }
  }
  
  @Override
  public void subTrigger(Object... parameter){
    heat = ((Number)parameter[0]).floatValue() < heat? heat - ((Number)parameter[0]).floatValue(): 0;
  }
}
