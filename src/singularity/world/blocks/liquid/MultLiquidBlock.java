package singularity.world.blocks.liquid;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.WindowedMean;
import arc.scene.ui.layout.Table;
import arc.struct.Bits;
import arc.util.Interval;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.modules.LiquidModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ReplaceBuildComp;
import universecore.util.handler.FieldHandler;

import java.util.Arrays;

import static mindustry.Vars.content;

@Annotations.ImplEntries
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
  }

  @Annotations.ImplEntries
  public class MultLiquidBuild extends Building implements ReplaceBuildComp {
    static {
      Events.run(EventType.Trigger.update, () -> {
        Building nextFlowBuild = FieldHandler.getValueDefault(Vars.ui.hudfrag.blockfrag, "nextFlowBuild");

        if(nextFlowBuild instanceof MultLiquidBuild mulB){
          mulB.updateLiquidsFlow();
        }
      });
    }

    public ClusterLiquidModule[] liquidsBuffer;
    public ClusterLiquidModule cacheLiquids;
    
    protected int current;
  
    public LiquidModule getModule(Liquid liquid){
      for(LiquidModule liquids: liquidsBuffer){
        if(liquids.current() == liquid) return liquids;
      }
      return null;
    }
  
    public LiquidModule getModuleAccept(Building source, Liquid liquid){
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
      liquidsBuffer = new ClusterLiquidModule[conduitAmount];
      for(int i=0; i<liquidsBuffer.length; i++){
        liquidsBuffer[i] = new ClusterLiquidModule();
      }
      liquids = liquidsBuffer.length == 0? new ClusterLiquidModule(): liquidsBuffer[0];
      cacheLiquids = (ClusterLiquidModule) liquids;
    
      return this;
    }

    @Override
    public void displayBars(Table table) {
      super.displayBars(table);
      for(int i=0; i<liquidsBuffer.length; i++){
        ClusterLiquidModule current = liquidsBuffer[i];
        int fi = i;

        table.add(new Bar(
            () -> current.smoothCurrent <= 0.001f?
                Core.bundle.get("bar.liquid") + " #" + fi:
                current.current().localizedName + "     " + (current.getFlowRate(current.current()) >= 0? Strings.autoFixed(current.getFlowRate(current.current()), 0): "...") + Core.bundle.get("misc.perSecond"),
            () -> current.current().barColor(),
            () -> current.smoothCurrent
        ));
        table.row();
      }
    }

    protected void updateLiquidsFlow() {
      for (ClusterLiquidModule module : liquidsBuffer) {
        module.updateFlow();
      }
    }
  
    @Override
    public MultLiquidBlock block(){
      return (MultLiquidBlock) block;
    }

    @Override
    public void updateTile(){
      super.updateTile();

      if (liquidsBuffer.length > 0) {
        liquids = liquidsBuffer[current = (current + 1) % liquidsBuffer.length];
        cacheLiquids = (ClusterLiquidModule) liquids;
      }
      for (ClusterLiquidModule module : liquidsBuffer) {
        module.smoothCurrent = Mathf.lerpDelta(module.smoothCurrent, module.currentAmount(), 0.4f);
      }
    }
  
    public boolean conduitAccept(MultLiquidBuild source, int index, Liquid liquid){
      noSleep();
      LiquidModule liquids = liquidsBuffer[index];
      return source.interactable(team) && liquids.currentAmount() < 0.01f || liquids.current() == liquid && liquids.currentAmount() < liquidCapacity;
    }

    public boolean shouldClusterMove(MultLiquidBuild source){
      return source.liquidsBuffer.length == liquidsBuffer.length;
    }
  
    public void handleLiquid(MultLiquidBuild source, int index, Liquid liquid, float amount){
      liquidsBuffer[index].add(liquid, amount);
    }
  
    public float moveLiquid(MultLiquidBuild dest, int index, Liquid liquid){
      if(dest == null) return 0;

      if (index >= dest.liquidsBuffer.length || !dest.shouldClusterMove(this)){
        return moveLiquid(dest, liquid);
      }
    
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
      LiquidModule liquids = getModuleAccept(source, liquid);
      if(liquids != null || (liquids = getEmpty()) != null){
        liquids.add(liquid, amount);
      }
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      noSleep();
      return source.interactable(team) && (getModuleAccept(source, liquid) != null || !isFull());
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      write.i(liquidsBuffer.length);
      for(LiquidModule liquids: liquidsBuffer){
        liquids.write(write);
      }
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      liquidsBuffer = new ClusterLiquidModule[read.i()];
      for (int i = 0; i < liquidsBuffer.length; i++) {
        ClusterLiquidModule module = new ClusterLiquidModule();
        module.read(read);
        liquidsBuffer[i] = module;
      }
    }

    @Override
    public void onReplaced(ReplaceBuildComp old) {
    }
  }

  public static class ClusterLiquidModule extends LiquidModule{
    final Interval flowTimer = new Interval(2);
    WindowedMean[] cacheFlow;
    float[] cacheSums;
    float[] displayFlow;
    Liquid current = content.liquid(0);
    WindowedMean[] flow;
    final Bits cacheBits = new Bits();
    final float[] liquids = new float[content.liquids().size];

    public float smoothCurrent;

    @Override
    public void updateFlow() {
      if(flowTimer.get(1, 20)){
        if(flow == null){
          if(cacheFlow == null || cacheFlow.length != liquids.length){
            cacheFlow = new WindowedMean[liquids.length];
            for(int i = 0; i < liquids.length; i++){
              cacheFlow[i] = new WindowedMean(3);
            }
            cacheSums = new float[liquids.length];
            displayFlow = new float[liquids.length];
          }else{
            for(int i = 0; i < liquids.length; i++){
              cacheFlow[i].reset();
            }
            Arrays.fill(cacheSums, 0);
            cacheBits.clear();
          }

          Arrays.fill(displayFlow, -1);

          flow = cacheFlow;
        }

        boolean updateFlow = flowTimer.get(30);

        for(int i = 0; i < liquids.length; i++){
          flow[i].add(cacheSums[i]);
          if(cacheSums[i] > 0){
            cacheBits.set(i);
          }
          cacheSums[i] = 0;

          if(updateFlow){
            displayFlow[i] = flow[i].hasEnoughData() ? flow[i].mean()/20 : -1;
          }
        }
      }
    }

    @Override
    public void stopFlow() {
      flow = null;
    }
    public float getFlowRate(Liquid liquid){
      return flow == null ? -1f : displayFlow[liquid.id] * 60;
    }

    public boolean hasFlowLiquid(Liquid liquid){
      return flow != null && cacheBits.get(liquid.id);
    }

    public Liquid current(){
      return current;
    }

    public void reset(Liquid liquid, float amount){
      Arrays.fill(liquids, 0f);
      liquids[liquid.id] = amount;
      current = liquid;
    }

    public float currentAmount(){
      return liquids[current.id];
    }

    public float get(Liquid liquid){
      return liquids[liquid.id];
    }

    public void clear(){
      Arrays.fill(liquids, 0);
    }

    public void add(Liquid liquid, float amount){
      liquids[liquid.id] += amount;
      current = liquid;

      if(flow != null){
        cacheSums[liquid.id] += Math.max(amount, 0);
      }
    }

    public void handleFlow(Liquid liquid, float amount){
      if(flow != null){
        cacheSums[liquid.id] += Math.max(amount, 0);
      }
    }

    public void remove(Liquid liquid, float amount){
      //cap to prevent negative removal
      add(liquid, Math.max(-amount, -liquids[liquid.id]));
    }

    public void each(LiquidModule.LiquidConsumer cons){
      for(int i = 0; i < liquids.length; i++){
        if(liquids[i] > 0){
          cons.accept(content.liquid(i), liquids[i]);
        }
      }
    }

    public float sum(LiquidModule.LiquidCalculator calc){
      float sum = 0f;
      for(int i = 0; i < liquids.length; i++){
        if(liquids[i] > 0){
          sum += calc.get(content.liquid(i), liquids[i]);
        }
      }
      return sum;
    }

    @Override
    public void write(Writes write){
      int amount = 0;
      for(float liquid : liquids){
        if(liquid > 0) amount++;
      }

      write.s(amount); //amount of liquids

      for(int i = 0; i < liquids.length; i++){
        if(liquids[i] > 0){
          write.s(i); //liquid ID
          write.f(liquids[i]); //liquid amount
        }
      }
    }

    @Override
    public void read(Reads read, boolean legacy){
      Arrays.fill(liquids, 0);
      int count = legacy ? read.ub() : read.s();

      for(int j = 0; j < count; j++){
        Liquid liq = content.liquid(legacy ? read.ub() : read.s());
        float amount = read.f();
        if(liq != null){
          int liquidid = liq.id;
          liquids[liquidid] = amount;
          if(amount > 0){
            current = liq;
          }
        }
      }
    }
  }
}
