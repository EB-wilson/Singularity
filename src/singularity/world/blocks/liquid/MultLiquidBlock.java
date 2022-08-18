package singularity.world.blocks.liquid;

import arc.Core;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.modules.LiquidModule;

public class MultLiquidBlock extends LiquidBlock{
  public int conduitAmount = 4;
  
  public MultLiquidBlock(String name){
    super(name);
    displayFlow = false;
  }
  
  @Override
  public void setBars(){
    super.setBars();
    removeBar("liquid");
    for(int i=0; i<conduitAmount; i++){
      int index = i;
      addBar("liquid#" + index, (MultLiquidBuild e) -> {
        LiquidModule current = e.liquidsBuffer[index];
        return new Bar(
            () -> current.currentAmount() <= 0.001f ?
                Core.bundle.get("bar.liquid") + " #" + index:
                current.current().localizedName + "     " + (current.getFlowRate(current.current()) >= 0? Strings.autoFixed(current.getFlowRate(current.current()), 0): "...") + Core.bundle.get("misc.preSecond"),
            () -> current.current().barColor(),
            () -> current.currentAmount() /liquidCapacity
        );
      });
    }
  }
  
  public class MultLiquidBuild extends Building{
    public LiquidModule[] liquidsBuffer;
    public LiquidModule cacheLiquids;
    
    protected int current;
  
    public LiquidModule getModule(Liquid liquid){
      for(LiquidModule liquids: liquidsBuffer){
        if(liquids.current() == liquid) return liquids;
      }
      return null;
    }
  
    public LiquidModule getModuleAccept(Liquid liquid){
      for(LiquidModule liquids: liquidsBuffer){
        if(liquids.current() == liquid && liquids.currentAmount() < liquidCapacity) return liquids;
      }
      return null;
    }
  
    public LiquidModule getEmpty(){
      for(LiquidModule liquids: liquidsBuffer){
        if(liquids.currentAmount() < 0.01f) return liquids;
      }
      return null;
    }
  
    public boolean isFull(){
      return getEmpty() == null;
    }
  
    public boolean anyLiquid(){
      for(LiquidModule liquids: liquidsBuffer){
        if(liquids.currentAmount() > 0.01f) return true;
      }
      return false;
    }
  
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      liquidsBuffer = new LiquidModule[conduitAmount];
      for(int i=0; i<liquidsBuffer.length; i++){
        liquidsBuffer[i] = new LiquidModule();
      }
      liquids = liquidsBuffer[0];
      cacheLiquids = liquids;
    
      return this;
    }
  
    @Override
    public MultLiquidBlock block(){
      return (MultLiquidBlock) block;
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      for(LiquidModule liquids: liquidsBuffer){
        liquids.updateFlow();
      }
      liquids = liquidsBuffer[current = (current + 1)%conduitAmount];
      cacheLiquids = liquids;
    }
  
    public boolean conduitAccept(MultLiquidBuild source, int index, Liquid liquid){
      noSleep();
      LiquidModule liquids = liquidsBuffer[index];
      return source.interactable(team) && liquids.currentAmount() < 0.01f || liquids.current() == liquid && liquids.currentAmount() < liquidCapacity;
    }
  
    public void handleLiquid(MultLiquidBuild source, int index, Liquid liquid, float amount){
      liquidsBuffer[index].add(liquid, amount);
    }
  
    public float moveLiquid(MultLiquidBuild dest, int index, Liquid liquid){
      if(dest == null) return 0;
    
      LiquidModule liquids = liquidsBuffer[index], oLiquids = dest.liquidsBuffer[index];
      if(dest.interactable(team) && liquids.get(liquid) > 0f){
        float ofract = oLiquids.get(liquid) / dest.block().liquidCapacity;
        float fract = liquids.get(liquid) / block().liquidCapacity* block.liquidPressure;
        float flow = Math.min(Mathf.clamp((fract - ofract)) * (block().liquidCapacity), liquids.get(liquid));
        flow = Math.min(flow, dest.block().liquidCapacity - oLiquids.get(liquid));
      
        if(flow > 0f && ofract <= fract && dest.conduitAccept(this, index, liquid)){
          dest.handleLiquid(this, index, liquid, flow);
          liquids.remove(liquid, flow);
          return flow;
        }else if(oLiquids.currentAmount() / dest.block().liquidCapacity > 0.1f && fract > 0.1f){
          float fx = (x + dest.x) / 2f, fy = (y + dest.y) / 2f;
        
          Liquid other = oLiquids.current();
          if((other.flammability > 0.3f && liquid.temperature > 0.7f) || (liquid.flammability > 0.3f && other.temperature > 0.7f)){
            damage(1 * Time.delta);
            dest.damage(1 * Time.delta);
            if(Mathf.chance(0.1 * Time.delta)){
              Fx.fire.at(fx, fy);
            }
          }else if((liquid.temperature > 0.7f && other.temperature < 0.55f) || (other.temperature > 0.7f && liquid.temperature < 0.55f)){
            liquids.remove(liquid, Math.min(liquids.get(liquid), 0.7f * Time.delta));
            if(Mathf.chance(0.2f * Time.delta)){
              Fx.steam.at(fx, fy);
            }
          }
        }
      }
      return 0;
    }
  
    @Override
    public void handleLiquid(Building source, Liquid liquid, float amount){
      LiquidModule liquids = getModuleAccept(liquid);
      if(liquids != null || (liquids = getEmpty()) != null){
        liquids.add(liquid, amount);
      }
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      return source.interactable(team) && (getModuleAccept(liquid) != null || !isFull());
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      for(LiquidModule liquids: liquidsBuffer){
        liquids.write(write);
      }
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      for(LiquidModule liquids: liquidsBuffer){
        liquids.read(read);
      }
    }
  }
}
